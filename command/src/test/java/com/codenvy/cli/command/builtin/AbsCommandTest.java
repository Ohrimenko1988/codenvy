/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.cli.command.builtin;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.codenvy.client.Codenvy;
import com.codenvy.client.CodenvyBuilder;
import com.codenvy.client.CodenvyClient;
import com.codenvy.client.ProjectClient;
import com.codenvy.client.Request;
import com.codenvy.client.UserClient;
import com.codenvy.client.WorkspaceClient;
import com.codenvy.client.auth.Credentials;
import com.codenvy.client.auth.CredentialsBuilder;
import com.codenvy.client.model.Project;
import com.codenvy.client.model.User;
import com.codenvy.client.model.Workspace;

/**
 * Super class of tests.
 *
 * @author Florent Benoit
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class AbsCommandTest {
    public static final String                  DEFAULT_URL = "http://ide3.cf.codenvy-stg.com";

    @Mock
    private CommandSession                      commandSession;

    @Mock
    private CodenvyClient                       codenvyClient;

    @Mock
    private CredentialsBuilder                  credentialsBuilder;

    @Mock
    private Credentials                         credentials;

    @Mock
    private CodenvyBuilder                      codenvyBuilder;

    @Mock
    private Codenvy                             codenvy;

    @Mock
    private UserClient                          userClient;

    @Mock
    private User                                user;

    @Mock
    private Request<User>                       userRequest;

    @Mock
    private ProjectClient                       projectClient;

    @Mock
    private Request<List< ? extends Project>>   projectRequests;

    @Mock
    private WorkspaceClient                     workspaceClient;

    @Mock
    private Request<List< ? extends Workspace>> workspaceRequests;

    private List<Workspace>                     workspaces;

    /**
     * List of projects for a given project name
     */
    private Map<String, List<Project>>          projects;


    @Before
    public void setUp() {
        // return current system.out when invocation is performed
        when(commandSession.getConsole()).thenAnswer(
                                                     new Answer() {
                                                         @Override
                                                         public Object answer(InvocationOnMock invocation) {
                                                             return System.out;
                                                         }
                                                     });

    }

    protected CommandSession getCommandSession() {
        return commandSession;
    }

    /**
     * Prepare the given command by injecting default configuration
     *
     * @param command
     */
    protected void prepare(AbsCommand command) {

        doReturn(credentials).when(credentialsBuilder).build();
        doReturn(credentialsBuilder).when(credentialsBuilder).withPassword(anyString());
        doReturn(credentialsBuilder).when(credentialsBuilder).withUsername(anyString());
        doReturn(credentialsBuilder).when(getCodenvyClient()).newCredentialsBuilder();

        doReturn(codenvyBuilder).when(getCodenvyClient()).newCodenvyBuilder(anyString(), anyString());
        doReturn(codenvyBuilder).when(codenvyBuilder).withCredentials(credentials);
        doReturn(codenvy).when(codenvyBuilder).build();

        // UserClient
        doReturn(userRequest).when(userClient).current();
        doReturn(user).when(userRequest).execute();
        doReturn(userClient).when(codenvy).user();

        // WorkspaceClient
        doReturn(workspaceClient).when(codenvy).workspace();
        doReturn(workspaceRequests).when(getWorkspaceClient()).all();
        // workspaces to use
        this.workspaces = new ArrayList<>();
        doReturn(workspaces).when(workspaceRequests).execute();

        // ProjectClient
        this.projects = new HashMap<>();
        doReturn(projectClient).when(codenvy).project();

        // intercept request
        when(projectClient.getWorkspaceProjects(anyString())).thenAnswer(
                                                                         new Answer() {
                                                                             @Override
                                                                             public Object answer(InvocationOnMock invocation) {
                                                                                 final String workspaceName =
                                                                                                              invocation.getArguments()[0].toString();

                                                                                 Request<List<Project>> requestProject =
                                                                                                                         mock(Request.class);

                                                                                 when(requestProject.execute()).thenAnswer(new Answer<Object>() {
                                                                                                                               @Override
                                                                                                                               public Object answer(InvocationOnMock invocation) throws Throwable {
                                                                                                                                   List<Project> workspaceProjects =
                                                                                                                                                                     projects.get(workspaceName);
                                                                                                                                   if (workspaceProjects == null) {
                                                                                                                                       workspaceProjects =
                                                                                                                                                           new ArrayList<Project>();
                                                                                                                                       projects.put(workspaceName,
                                                                                                                                                    workspaceProjects);
                                                                                                                                   }

                                                                                                                                   return workspaceProjects;
                                                                                                                               }
                                                                                                                           });

                                                                                 return requestProject;

                                                                             }
                                                                         });

        doReturn(codenvy).when(commandSession).get(Codenvy.class.getName());

        command.setCodenvyClient(codenvyClient);
    }

    protected CodenvyClient getCodenvyClient() {
        return codenvyClient;
    }

    protected CredentialsBuilder getCredentialsBuilder() {
        return credentialsBuilder;
    }

    protected Credentials getCredentials() {
        return credentials;
    }

    protected CodenvyBuilder getCodenvyBuilder() {
        return codenvyBuilder;
    }

    protected User getUser() {
        return user;
    }

    protected UserClient getUserClient() {
        return userClient;
    }

    public Codenvy getCodenvy() {
        return codenvy;
    }

    public Request<User> getRequest() {
        return userRequest;
    }

    public ProjectClient getProjectClient() {
        return projectClient;
    }

    public WorkspaceClient getWorkspaceClient() {
        return workspaceClient;
    }

    public Request<List< ? extends Workspace>> getWorkspaceRequests() {
        return workspaceRequests;
    }

    public List<Workspace> getWorkspaces() {
        return workspaces;
    }


    protected Workspace addWorkspace(String workspaceName) {
        Workspace workspace = mock(Workspace.class);
        WorkspaceReference workspaceRef = mock(WorkspaceReference.class);
        doReturn(workspaceRef).when(workspace).workspaceReference();
        doReturn(workspaceName).when(workspaceRef).name();
        doReturn(workspaceName).when(workspaceRef).id();

        getWorkspaces().add(workspace);

        Request< ? extends WorkspaceReference> requestWorkspaceRef = mock(Request.class);
        doReturn(requestWorkspaceRef).when(getWorkspaceClient()).withName(workspaceName);
        doReturn(workspaceRef).when(requestWorkspaceRef).execute();

        return workspace;
    }

    protected Project addProject(String workspaceName, String projectName) {
        Project project = mock(Project.class);
        doReturn(projectName).when(project).name();

        getProjects(workspaceName).add(project);

        return project;
    }

    protected List<Project> getProjects(String workspaceName) {
        List<Project> workspaceProjects = projects.get(workspaceName);
        if (workspaceProjects == null) {
            workspaceProjects = new ArrayList<>();
            projects.put(workspaceName, workspaceProjects);
        }
        return workspaceProjects;
    }


}
