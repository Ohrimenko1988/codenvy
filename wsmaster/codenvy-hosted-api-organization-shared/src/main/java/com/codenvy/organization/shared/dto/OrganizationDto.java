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
package com.codenvy.organization.shared.dto;

import com.codenvy.organization.shared.model.Organization;
import java.util.List;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

/** @author Sergii Leschenko */
@DTO
public interface OrganizationDto extends Organization, Hyperlinks {
  @Override
  String getId();

  void setId(String id);

  OrganizationDto withId(String id);

  @Override
  String getName();

  void setName(String name);

  OrganizationDto withName(String name);

  @Override
  String getQualifiedName();

  void setQualifiedName(String qualifiedName);

  OrganizationDto withQualifiedName(String qualifiedName);

  @Override
  String getParent();

  void setParent(String parent);

  OrganizationDto withParent(String parent);

  @Override
  OrganizationDto withLinks(List<Link> links);
}
