/*
 *    Copyright 2009-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.extension;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for the three CRUD method generation approaches:
 * <ol>
 * <li>CrudMapper extension</li>
 * <li>SqlProviderFactory via annotation pipeline</li>
 * <li>Conventional method name matching</li>
 * </ol>
 */
class EntitySqlProviderTest extends BaseDataTest {

  public interface UserMapper extends CrudMapper<User> {
  }

  @Test
  void crudMapper_shouldInsertAndSelectById() throws SQLException, IOException {
    SqlSessionFactory sqlSessionFactory = createDefaultHsqlDbSqlSessionFactory("entity_sql_provider");
    sqlSessionFactory.getConfiguration().setLogImpl(StdOutImpl.class);
    // populate in-memory database
    runScriptSql(sqlSessionFactory, "drop table users if exists;");
    runScriptSql(sqlSessionFactory, "create table users (id int, name varchar(20), email varchar(100));");

    Configuration configuration = sqlSessionFactory.getConfiguration();
    configuration.setSqlProviderFactory(new EntitySqlProviderFactory());
    configuration.addMapper(UserMapper.class);

    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      UserMapper mapper = session.getMapper(UserMapper.class);

      // insert one
      {
        User user = new User("Alice", "alice@example.com");
        // key generation is not supported now
        user.setId(1);
        int rows = mapper.insert(user);
        assertEquals(1, rows);
      }

      // select by id
      {
        User loaded = mapper.selectById(1);
        assertNotNull(loaded);
        assertEquals("Alice", loaded.getName());
        assertEquals("alice@example.com", loaded.getEmail());
      }

      // insert batch
      final List<User> users = new ArrayList<>();
      {
        for (int i = 2; i < 10; i++) {
          User userI = new User("User" + i, "user" + i + "@example.com");
          userI.setId(i);
          users.add(userI);
        }
        int i = mapper.insertBatch(users);
        assertEquals(8, i);
      }

      // update by id
      final List<User> updatedUsers = new ArrayList<>();
      {
        for (User userI : users) {
          User loadedI = mapper.selectById(userI.getId());
          assertNotNull(loadedI);
          assertEquals(userI.getName(), loadedI.getName());
          assertEquals(userI.getEmail(), loadedI.getEmail());

          loadedI.setName("Updated " + userI.getName());
          int res = mapper.updateById(loadedI);
          assertEquals(1, res);
          loadedI = mapper.selectById(userI.getId());
          assertEquals("Updated " + userI.getName(), loadedI.getName());

          updatedUsers.add(loadedI);
        }
      }

      // selectByIds
      {
        List<Integer> ids = updatedUsers.stream().map(User::getId).collect(Collectors.toList());
        List<User> userList = mapper.selectByIds(ids);
        assertEquals(ids.size(), userList.size());
        for (int i = 0; i < userList.size(); i++) {
          User userI = userList.get(i);
          assertEquals(updatedUsers.get(i).getId(), userI.getId());
          assertEquals(updatedUsers.get(i).getName(), userI.getName());
          assertEquals(updatedUsers.get(i).getEmail(), userI.getEmail());
        }
      }

      // selectAll
      {
        List<User> userList = mapper.selectAll();
        assertEquals(users.size() + 1, userList.size());
        for (int i = 0; i < userList.size(); i++) {
          User userI = userList.get(i);
          if (i == 0) {
            assertEquals(1, userI.getId());
            assertEquals("Alice", userI.getName());
            assertEquals("alice@example.com", userI.getEmail());
          } else {
            assertEquals(updatedUsers.get(i - 1).getId(), userI.getId());
            assertEquals(updatedUsers.get(i - 1).getName(), userI.getName());
            assertEquals(updatedUsers.get(i - 1).getEmail(), userI.getEmail());
          }
        }
      }

      // countAll
      {
        long count = mapper.countAll();
        assertEquals(9, count);
      }

      // deleteById
      {
        int i = mapper.deleteById(1);
        assertEquals(1, i);
        User loaded = mapper.selectById(1);
        assertNull(loaded);
      }

      // deleteByIds
      {
        int i = mapper.deleteByIds(updatedUsers.stream().map(User::getId).collect(Collectors.toList()));
        assertEquals(updatedUsers.size(), i);
        for (User userI : updatedUsers) {
          User loadedI = mapper.selectById(userI.getId());
          assertNull(loadedI);
        }
      }

      // deleteAll
      {
        int i = mapper.deleteAll();
        assertEquals(0, i);
        for (User userI : users) {
          User loadedI = mapper.selectById(userI.getId());
          assertNull(loadedI);
        }
      }

      // countAll
      {
        long count = mapper.countAll();
        assertEquals(0, count);
      }

      // existsById
      {
        boolean exists = mapper.existsById(1);
        assertFalse(exists);
      }

      // updateSelective
      {
        mapper.deleteAll();
        User user = new User();
        user.setId(1);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        mapper.insert(user);

        // update by id
        user.setId(1);
        user.setName("Alice-Updated");
        user.setEmail("alice-updated@example.com");
        int i = mapper.updateSelective(user, Collections.singleton("email"));
        assertEquals(1, i);

        User userInDb = mapper.selectById(1);
        // name column is not passed to update, so it should not be updated
        assertEquals("Alice", userInDb.getName());
        assertEquals("alice-updated@example.com", userInDb.getEmail());
      }
      {
        mapper.deleteAll();
        User user = new User();
        user.setId(1);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        mapper.insert(user);

        // update by id
        user.setId(1);
        user.setName("Alice-Updated");
        user.setEmail("alice-updated@example.com");
        int i = mapper.updateSelective(user, Collections.singleton("name"));
        assertEquals(1, i);

        User userInDb = mapper.selectById(1);
        // name column is not passed to update, so it should not be updated
        assertEquals("Alice-Updated", userInDb.getName());
        assertEquals("alice@example.com", userInDb.getEmail());
      }
    }
  }
}
