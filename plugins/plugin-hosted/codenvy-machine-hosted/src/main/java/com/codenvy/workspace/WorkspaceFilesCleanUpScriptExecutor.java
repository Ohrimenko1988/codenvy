/*
 * Copyright (c) [2012] - [2017] Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.codenvy.workspace;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.codenvy.machine.backup.WorkspaceIdHashLocationFinder;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.workspace.server.WorkspaceFilesCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component to launch cleanUp workspace files script.
 *
 * @author Alexander Andrienko
 */
@Singleton
public class WorkspaceFilesCleanUpScriptExecutor implements WorkspaceFilesCleaner {

  private static final Logger LOG =
      LoggerFactory.getLogger(WorkspaceFilesCleanUpScriptExecutor.class);

  private final WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder;
  private final File backupsRootDir;
  private final int cleanUpTimeOut;
  private final String workspaceCleanUpScript;

  @Inject
  public WorkspaceFilesCleanUpScriptExecutor(
      WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder,
      @Named("che.user.workspaces.storage") File backupsRootDir,
      @Named("workspace.projects_storage.cleanup.script_path") String workspaceCleanUpScript,
      @Named("workspace.projects_storage.cleanup.timeout_seconds") int cleanUpTimeOut) {
    this.workspaceIdHashLocationFinder = workspaceIdHashLocationFinder;
    this.backupsRootDir = backupsRootDir;
    this.workspaceCleanUpScript = workspaceCleanUpScript;
    this.cleanUpTimeOut = cleanUpTimeOut;
  }

  /**
   * Execute workspace cleanUp script.
   *
   * @param workspace to cleanUp files.
   * @throws IOException in case I/O error.
   * @throws ServerException in case internal server error.
   */
  @Override
  public void clear(Workspace workspace) throws IOException, ServerException {
    File wsFolder =
        workspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspace.getId());
    CommandLine commandLine = new CommandLine(workspaceCleanUpScript, wsFolder.getAbsolutePath());

    try {
      execute(commandLine.asArray(), cleanUpTimeOut);
    } catch (InterruptedException | TimeoutException e) {
      throw new ServerException(
          format(
              "Failed to delete workspace files by path: '%s' for workspace with id: '%s'",
              wsFolder.getAbsolutePath(), workspace.getId()),
          e);
    }
  }

  @VisibleForTesting
  void execute(String[] commandLine, int timeout)
      throws TimeoutException, IOException, InterruptedException {
    final ListLineConsumer outputConsumer = new ListLineConsumer();
    Process process = ProcessUtil.executeAndWait(commandLine, timeout, SECONDS, outputConsumer);

    if (process.exitValue() != 0) {
      LOG.error(outputConsumer.getText());
      throw new IOException("Process failed. Exit code " + process.exitValue());
    }
  }
}
