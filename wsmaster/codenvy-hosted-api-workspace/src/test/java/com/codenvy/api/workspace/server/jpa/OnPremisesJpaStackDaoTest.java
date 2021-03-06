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
package com.codenvy.api.workspace.server.jpa;

import static org.eclipse.che.commons.test.db.H2TestHelper.inMemoryDefault;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.codenvy.api.machine.server.jpa.OnPremisesJpaMachineModule;
import com.codenvy.api.workspace.server.spi.jpa.OnPremisesJpaStackDao;
import com.codenvy.api.workspace.server.stack.StackPermissionsImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.JpaPersistModule;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.commons.test.db.H2TestHelper;
import org.eclipse.che.core.db.DBInitializer;
import org.eclipse.che.core.db.schema.SchemaInitializer;
import org.eclipse.che.core.db.schema.impl.flyway.FlywaySchemaInitializer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** @author Max Shaposhnik (mshaposhnik@codenvy.com) */
public class OnPremisesJpaStackDaoTest {

  private EntityManager manager;
  private OnPremisesJpaStackDao dao;

  private StackPermissionsImpl[] permissions;
  private UserImpl[] users;
  private StackImpl[] stacks;

  @BeforeClass
  public void setupEntities() throws Exception {
    permissions =
        new StackPermissionsImpl[] {
          new StackPermissionsImpl("user1", "stack1", Arrays.asList("read", "use", "search")),
          new StackPermissionsImpl("user1", "stack2", Arrays.asList("read", "search")),
          new StackPermissionsImpl("*", "stack3", Arrays.asList("read", "search")),
          new StackPermissionsImpl("user1", "stack4", Arrays.asList("read", "run")),
          new StackPermissionsImpl("user2", "stack1", Arrays.asList("read", "use"))
        };

    users =
        new UserImpl[] {
          new UserImpl("user1", "user1@com.com", "usr1"),
          new UserImpl("user2", "user2@com.com", "usr2")
        };

    stacks =
        new StackImpl[] {
          new StackImpl(
              "stack1",
              "st1",
              null,
              null,
              null,
              Arrays.asList("tag1", "tag2"),
              null,
              null,
              null,
              null),
          new StackImpl("stack2", "st2", null, null, null, null, null, null, null, null),
          new StackImpl(
              "stack3",
              "st3",
              null,
              null,
              null,
              Arrays.asList("tag1", "tag2"),
              null,
              null,
              null,
              null),
          new StackImpl("stack4", "st4", null, null, null, null, null, null, null, null)
        };

    Injector injector = Guice.createInjector(new TestModule(), new OnPremisesJpaMachineModule());
    manager = injector.getInstance(EntityManager.class);
    dao = injector.getInstance(OnPremisesJpaStackDao.class);
  }

  @BeforeMethod
  public void setUp() throws Exception {
    manager.getTransaction().begin();
    for (UserImpl user : users) {
      manager.persist(user);
    }

    for (StackImpl recipe : stacks) {
      manager.persist(recipe);
    }

    for (StackPermissionsImpl recipePermissions : permissions) {
      manager.persist(recipePermissions);
    }
    manager.getTransaction().commit();
    manager.clear();
  }

  @AfterMethod
  public void cleanup() {
    manager.getTransaction().begin();

    manager
        .createQuery("SELECT p FROM StackPermissions p", StackPermissionsImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT r FROM Stack r", StackImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT u FROM Usr u", UserImpl.class)
        .getResultList()
        .forEach(manager::remove);
    manager.getTransaction().commit();
  }

  @AfterClass
  public void shutdown() throws Exception {
    manager.getEntityManagerFactory().close();
    H2TestHelper.shutdownDefault();
  }

  @Test
  public void shouldFindStackByPermissions() throws Exception {
    List<StackImpl> results = dao.searchStacks(users[0].getId(), null, 0, 0);
    assertEquals(results.size(), 3);
    assertTrue(results.contains(stacks[0]));
    assertTrue(results.contains(stacks[1]));
    assertTrue(results.contains(stacks[2]));
  }

  @Test
  public void shouldFindRecipeByPermissionsAndTags() throws Exception {
    List<StackImpl> results =
        dao.searchStacks(users[0].getId(), Collections.singletonList("tag2"), 0, 0);
    assertEquals(results.size(), 2);
    assertTrue(results.contains(stacks[0]));
    assertTrue(results.contains(stacks[2]));
  }

  @Test
  public void shouldNotFindRecipeByNonexistentTags() throws Exception {
    List<StackImpl> results =
        dao.searchStacks(users[0].getId(), Collections.singletonList("unexisted_tag2"), 0, 0);
    assertTrue(results.isEmpty());
  }

  private class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      install(new JpaPersistModule("main"));
      bind(SchemaInitializer.class)
          .toInstance(
              new FlywaySchemaInitializer(inMemoryDefault(), "che-schema", "codenvy-schema"));
      bind(DBInitializer.class).asEagerSingleton();
    }
  }
}
