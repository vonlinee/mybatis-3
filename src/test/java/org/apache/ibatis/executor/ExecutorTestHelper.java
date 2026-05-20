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
package org.apache.ibatis.executor;

import java.util.*;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.domain.blog.*;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.TextSqlNode;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

final class ExecutorTestHelper {

  static final Cache authorCache;

  static {
    authorCache = new SynchronizedCache(
        new SerializedCache(new LoggingCache(new ScheduledCache(new PerpetualCache("author_cache")))));
  }

  static MappedStatement prepareInsertAuthorMappedStatement(final Configuration config) {
    return new MappedStatement.Builder(config, "insertAuthor",
        new StaticSqlSource(
            "INSERT INTO author (id,username,password,email,bio,favourite_section) values(?,?,?,?,?,?)"),
        SqlCommandType.INSERT)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id",               int.class)
                .addMapping("username",          String.class)
                .addMapping("password",          String.class)
                .addMapping("email",             String.class)
                .addMapping("bio",               String.class,  JdbcType.VARCHAR)
                .addMapping("favouriteSection",  Section.class, JdbcType.VARCHAR)
                .build())
            .cache(authorCache)
            .build();
            // @formatter:on
  }

  static MappedStatement prepareInsertAuthorMappedStatementWithAutoKey(final Configuration config) {
    return new MappedStatement.Builder(config, "insertAuthor",
        new StaticSqlSource("INSERT INTO author (username,password,email,bio,favourite_section) values(?,?,?,?,?)"),
        SqlCommandType.INSERT)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("username",          String.class)
                .addMapping("password",          String.class)
                .addMapping("email",             String.class)
                .addMapping("bio",               String.class,  JdbcType.VARCHAR)
                .addMapping("favouriteSection",  Section.class, JdbcType.VARCHAR)
                .build())
            .cache(authorCache)
            .keyGenerator(Jdbc3KeyGenerator.INSTANCE)
            .keyProperty("id")
            .build();
    // @formatter:on
  }

  static MappedStatement prepareInsertAuthorProc(final Configuration config) {
    return new MappedStatement.Builder(config, "insertAuthorProc", new StaticSqlSource("{call insertAuthor(?,?,?,?)}"),
        SqlCommandType.INSERT)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id",        int.class)
                .addMapping("username",  String.class)
                .addMapping("password",  String.class)
                .addMapping("email",     String.class)
                .build())
            .cache(authorCache)
            .build();
            // @formatter:on
  }

  static MappedStatement prepareUpdateAuthorMappedStatement(final Configuration config) {
    return new MappedStatement.Builder(config, "updateAuthor",
        new StaticSqlSource("UPDATE author SET username = ?, password = ?, email = ?, bio = ? WHERE id = ?"),
        SqlCommandType.UPDATE)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("username",  String.class)
                .addMapping("password",  String.class)
                .addMapping("email",     String.class)
                .addMapping("bio",       String.class, JdbcType.VARCHAR)
                .addMapping("id",        int.class)
                .build())
            .cache(authorCache)
            .build();
            // @formatter:on
  }

  static MappedStatement prepareDeleteAuthorMappedStatement(final Configuration config) {
    return new MappedStatement.Builder(config, "deleteAuthor", new StaticSqlSource("DELETE FROM author WHERE id = ?"),
        SqlCommandType.DELETE)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id", int.class)
                .build())
            .cache(authorCache)
            .build();
            // @formatter:on
  }

  static MappedStatement prepareSelectOneAuthorMappedStatement(final Configuration config) {
    // @formatter:off
    final ResultMap rm = ResultMap.builder(config, "defaultResultMap", Author.class)
        .addMapping("id",               "id",               int.class)
        .addMapping("username",          "username",          String.class)
        .addMapping("password",          "password",          String.class)
        .addMapping("email",             "email",             String.class)
        .addMapping("bio",               "bio",               String.class)
        .addMapping("favouriteSection",  "favourite_section", Section.class)
        .build();
    // @formatter:on

    return new MappedStatement.Builder(config, "selectAuthor", new StaticSqlSource("SELECT * FROM author WHERE id = ?"),
        SqlCommandType.SELECT)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id", int.class)
                .build())
            .resultMap(rm)
            .cache(authorCache)
            .build();
            // @formatter:on
  }

  static MappedStatement prepareSelectAllAuthorsAutoMappedStatement(final Configuration config) {
    // @formatter:off
    final ResultMap autoMap = ResultMap.builder(config, "defaultResultMap", Author.class)
        .addMapping("favouriteSection", "favourite_section", Section.class)
        .addMapping(null,               "not_exists",         Object.class)
        .build();
    return new MappedStatement.Builder(config, "selectAuthorAutoMap",
        new StaticSqlSource("SELECT * FROM author ORDER BY id"), SqlCommandType.SELECT)
            .resultMap(autoMap)
            .fetchSize(1000)
            .timeout(2000)
            .build();
    // @formatter:on
  }

  static MappedStatement prepareSelectOneAuthorMappedStatementWithConstructorResults(final Configuration config) {
    // @formatter:off
    final ResultMap constructorResultMap = ResultMap.builder(config, "defaultResultMap", Author.class)
        .addMapping(null, "id", Integer.class, int.class, ResultFlag.CONSTRUCTOR)
        .addMapping("username",          "username",          String.class)
        .addMapping("password",          "password",          String.class)
        .addMapping("email",             "email",             String.class)
        .addMapping("bio",               "bio",               String.class)
        .addMapping("favouriteSection",  "favourite_section", Section.class)
        .build();
    // @formatter:on
    return new MappedStatement.Builder(config, "selectAuthor", new StaticSqlSource("SELECT * FROM author WHERE id = ?"),
        SqlCommandType.SELECT)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id", int.class)
                .build())
            .resultMap(constructorResultMap)
            .cache(authorCache)
            .build();
    // @formatter:on
  }

  static MappedStatement prepareSelectTwoSetsOfAuthorsProc(final Configuration config) {
    // @formatter:off
    final ResultMap authorsResultMap = ResultMap.builder(config, "defaultResultMap", Author.class)
        .addMapping("id",       "id",       int.class)
        .addMapping("username", "username", String.class)
        .addMapping("password", "password", String.class)
        .addMapping("email",    "email",    String.class)
        .addMapping("bio",      "bio",      String.class)
        .build();
    // @formatter:on
    return new MappedStatement.Builder(config, "selectTwoSetsOfAuthors",
        new StaticSqlSource("{call selectTwoSetsOfAuthors(?,?)}"), SqlCommandType.SELECT)
            .statementType(StatementType.CALLABLE)
            // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id1", int.class)
                .addMapping("id2", int.class)
                .build())
            // @formatter:on
            .resultMaps(new ArrayList<>() {
              {
                add(authorsResultMap);
                add(authorsResultMap);
              }
            }).build();
  }

  static MappedStatement prepareSelectAuthorViaOutParams(final Configuration config) {
    return new MappedStatement.Builder(config, "selectAuthorViaOutParams",
        new StaticSqlSource("{call selectAuthorViaOutParams(?,?,?,?,?)}"), SqlCommandType.SELECT)
            .statementType(StatementType.CALLABLE)
            // @formatter:off
            .parameterMap(ParameterMap.builder(config, "defaultParameterMap", Author.class)
                .addMapping("id",        int.class)
                .addMapping("username",  String.class, JdbcType.VARCHAR, ParameterMode.OUT)
                .addMapping("password",  String.class, JdbcType.VARCHAR, ParameterMode.OUT)
                .addMapping("email",     String.class, JdbcType.VARCHAR, ParameterMode.OUT)
                .addMapping("bio",       String.class, JdbcType.VARCHAR, ParameterMode.OUT)
                .build())
            .resultMaps(new ArrayList<>())
            .cache(authorCache)
            .build();
            // @formatter:on
  }

  static MappedStatement prepareSelectDiscriminatedPost(final Configuration config) {
    // @formatter:off
    final ResultMap discriminatorResultMap = ResultMap.builder(config, "postResultMap", HashMap.class)
        .addMapping("subject", "subject", String.class)
        .addMapping("body",    "body",    String.class)
        .build();
    // @formatter:on
    config.addResultMap(discriminatorResultMap);
    // @formatter:off
    final ResultMap postsResultMap = ResultMap.builder(config, "defaultResultMap", HashMap.class)
        .addMapping("id",      "id",      int.class)
        .addMapping("blog_id", "blog_id", int.class)
        .discriminator("section", "section", String.class, new HashMap<>() {{
          put("NEWS",     discriminatorResultMap.getId());
          put("VIDEOS",   discriminatorResultMap.getId());
          put("PODCASTS", discriminatorResultMap.getId());
          // IMAGES left out on purpose.
        }})
        .build();
    // @formatter:on
    return new MappedStatement.Builder(config, "selectPosts", new StaticSqlSource("SELECT * FROM post"),
        SqlCommandType.SELECT).resultMap(postsResultMap).build();
  }

  static MappedStatement createInsertAuthorWithIDof99MappedStatement(final Configuration config) {
    return new MappedStatement.Builder(config, "insertAuthor", new StaticSqlSource(
        "INSERT INTO author (id,username,password,email,bio) values(99,'someone','******','someone@apache.org',null)"),
        SqlCommandType.INSERT).statementType(StatementType.STATEMENT)
            .parameterMap(ParameterMap.create("defaultParameterMap", Author.class)).cache(authorCache).build();
  }

  static MappedStatement createSelectAuthorWithIDof99MappedStatement(final Configuration config) {
    // @formatter:off
    final ResultMap selectResultMap = ResultMap.builder(config, "defaultResultMap", Author.class)
        .addMapping("id",       "id",       int.class)
        .addMapping("username", "username", String.class)
        .addMapping("password", "password", String.class)
        .addMapping("email",    "email",    String.class)
        .addMapping("bio",      "bio",      String.class)
        .build();
    // @formatter:on
    return new MappedStatement.Builder(config, "selectAuthor",
        new StaticSqlSource("SELECT * FROM author WHERE id = 99"), SqlCommandType.SELECT)
            .statementType(StatementType.STATEMENT)
            .parameterMap(ParameterMap.create("defaultParameterMap", Author.class)).resultMap(selectResultMap).build();
  }

  static MappedStatement prepareComplexSelectBlogMappedStatement(final Configuration config) {
    final SqlSource sqlSource = new StaticSqlSource("""
        SELECT b.id \
             , b.author_id \
             , b.title \
             , a.username \
             , a.password \
             , a.email \
             , a.bio\
          FROM blog b\
         INNER JOIN author a ON b.author_id = a.id\
         WHERE b.id = ?\
        """);
    // @formatter:off
    final ParameterMap parameterMap =  ParameterMap.builder(config, "defaultParameterMap", int.class)
        .addMapping("id", int.class)
        .build();
    final ResultMap resultMap = ResultMap.builder(config, "defaultResultMap", Blog.class)
        .addMapping("id",              "id",        int.class,     ResultFlag.ID)
        .addMapping("title",           "title",     String.class)
        .addMapping("author.id",       "author_id", int.class)
        .addMapping("author.username", "username",  String.class)
        .addMapping("author.password", "password",  String.class)
        .addMapping("author.email",    "email",     String.class)
        .addMapping("author.bio",      "bio",       String.class)
        .addNestedMapping("posts", "id", int.class, List.class, "selectPostsForBlog")
        .build();
    // @formatter:on

    return new MappedStatement.Builder(config, "selectBlogById", sqlSource, SqlCommandType.SELECT)
        .parameterMap(parameterMap).resultMap(resultMap).build();
  }

  static MappedStatement prepareSelectBlogByIdAndAuthor(final Configuration config) {
    final SqlSource sqlSource = new StaticSqlSource("""
        SELECT b.id\
             , b.author_id\
             , b.title\
             , a.username\
             , a.password\
             , a.email\
             , a.bio\
          FROM blog b\
         INNER JOIN author a ON b.author_id = a.id\
         WHERE b.id = ? and a.id = ?\
        """);
    // @formatter:off
    final ParameterMap parameterMap = ParameterMap.builder(config, "defaultParameterMap", Map.class)
        .addMapping("blogId",   int.class)
        .addMapping("authorId", int.class)
        .build();
    final ResultMap resultMap = ResultMap.builder(config, "defaultResultMap", Blog.class)
        .addMapping("id",              "id",        int.class,     ResultFlag.ID)
        .addMapping("title",           "title",     String.class)
        .addMapping("author.id",       "author_id", int.class)
        .addMapping("author.username", "username",  String.class)
        .addMapping("author.password", "password",  String.class)
        .addMapping("author.email",    "email",     String.class)
        .addMapping("author.bio",      "bio",       String.class)
        .addNestedMapping("posts", "id", int.class, List.class, "selectPostsForBlog")
        .build();
    // @formatter:on

    return new MappedStatement.Builder(config, "selectBlogByIdAndAuthor", sqlSource, SqlCommandType.SELECT)
        .parameterMap(parameterMap).resultMap(resultMap).build();
  }

  static MappedStatement prepareSelectPostsForBlogMappedStatement(final Configuration config) {
    final SqlSource sqlSource = new StaticSqlSource("""
        SELECT p.id\
             , p.created_on\
             , p.blog_id\
             , p.section\
             , p.subject\
             , p.body\
             , pt.tag_id\
             , t.name as tag_name\
             , c.id as comment_id\
             , c.name as comment_name\
             , c.comment\
          FROM post p\
         INNER JOIN post_tag pt ON pt.post_id = p.id\
         INNER JOIN tag t ON pt.tag_id = t.id\
         LEFT OUTER JOIN comment c ON c.post_id = p.id\
         WHERE p.blog_id = ?\
        """);
    // @formatter:off
    final ParameterMap parameterMap = ParameterMap.builder(config, "defaultParameterMap", Author.class)
        .addMapping("id", int.class)
        .build();
    final ResultMap tagResultMap =  ResultMap.builder(config, "tagResultMap", Tag.class)
        .addMapping("id",   "tag_id",   int.class,    ResultFlag.ID)
        .addMapping("name", "tag_name", String.class)
        .build();
    final ResultMap commentResultMap = ResultMap.builder(config, "commentResultMap", Comment.class)
        .addMapping("id",      "comment_id",   int.class,    ResultFlag.ID)
        .addMapping("name",    "comment_name", String.class)
        .addMapping("comment", "comment",      String.class)
        .build();
    // @formatter:on
    config.addResultMap(tagResultMap);
    config.addResultMap(commentResultMap);
    // @formatter:off
    final ResultMap postResultMap = ResultMap.builder(config, "defaultResultMap", Post.class)
        .addMapping("id",        "id",         int.class,   ResultFlag.ID)
        .addNestedMapping("blog", "blog_id", int.class, Blog.class, "selectBlogById")
        .addMapping("createdOn", "created_on", Date.class)
        .addMapping("section",   "section",    Section.class)
        .addMapping("subject",   "subject",    String.class)
        .addMapping("body",      "body",       String.class)
        .addNestedMapping("tags", tagResultMap.getId())
        .addNestedMapping("comments", commentResultMap.getId())
        .build();
    // @formatter:on
    return new MappedStatement.Builder(config, "selectPostsForBlog", sqlSource, SqlCommandType.SELECT)
        .parameterMap(parameterMap).resultMap(postResultMap).build();
  }

  static MappedStatement prepareSelectPostMappedStatement(final Configuration config) {
    final SqlSource sqlSource = new StaticSqlSource("""
        SELECT p.id\
             , p.created_on\
             , p.blog_id\
             , p.section\
             , p.subject\
             , p.body\
             , pt.tag_id\
             , t.name as tag_name\
             , c.id as comment_id\
             , c.name as comment_name\
             , c.comment\
          FROM post p\
          LEFT OUTER JOIN post_tag pt ON pt.post_id = p.id\
          LEFT OUTER JOIN tag t ON pt.tag_id = t.id\
          LEFT OUTER JOIN comment c ON c.post_id = p.id\
         WHERE p.id = ?\
        """);
    // @formatter:off
    final ParameterMap parameterMap = ParameterMap.builder(config,"defaultParameterMap", Author.class)
        .addMapping("id", int.class)
        .build();
    final ResultMap tagResultMap = ResultMap.builder(config, "tagResultMap", Tag.class)
        .addMapping("id",   "tag_id",   int.class,    ResultFlag.ID)
        .addMapping("name", "tag_name", String.class)
        .build();
    final ResultMap commentResultMap = ResultMap.builder(config, "commentResultMap", Comment.class)
        .addMapping("id",      "comment_id",   int.class,    ResultFlag.ID)
        .addMapping("name",    "comment_name", String.class)
        .addMapping("comment", "comment",      String.class)
        .build();
    // @formatter:on
    config.addResultMap(tagResultMap);
    config.addResultMap(commentResultMap);
    // @formatter:off
    final ResultMap postResultMap = ResultMap.builder(config, "", Post.class)
        .addMapping("id",        "id",         int.class,   ResultFlag.ID)
        .addNestedMapping("blog", "blog_id", int.class, Blog.class, "selectBlogById")
        .addMapping("createdOn", "created_on", Date.class)
        .addMapping("section",   "section",    Section.class)
        .addMapping("subject",   "subject",    String.class)
        .addMapping("body",      "body",       String.class)
        .addNestedMapping("tags", tagResultMap.getId())
        .addNestedMapping("comments", commentResultMap.getId())
        .build();
    // @formatter:on

    return new MappedStatement.Builder(config, "selectPostsForBlog", sqlSource, SqlCommandType.SELECT)
        .parameterMap(parameterMap).resultMap(postResultMap).build();
  }

  static MappedStatement prepareSelectPostWithBlogByAuthorMappedStatement(final Configuration config) {
    final SqlSource sqlSource = new StaticSqlSource("""
        SELECT p.id\
             , p.created_on\
             , p.blog_id\
             , p.author_id\
             , p.section\
             , p.subject\
             , p.body\
             , pt.tag_id\
             , t.name as tag_name\
             , c.id as comment_id\
             , c.name as comment_name\
             , c.comment\
          FROM post p\
          LEFT OUTER JOIN post_tag pt ON pt.post_id = p.id\
          LEFT OUTER JOIN tag t ON pt.tag_id = t.id\
          LEFT OUTER JOIN comment c ON c.post_id = p.id\
         WHERE p.id = ?\
        """);
    // @formatter:off
    final ParameterMap parameterMap = ParameterMap.builder(config,"defaultParameterMap", Author.class)
        .addMapping("id", int.class)
        .build();
    final ResultMap tagResultMap = ResultMap.builder(config, "tagResultMap", Tag.class)
        .addMapping("id",   "tag_id",   int.class,    ResultFlag.ID)
        .addMapping("name", "tag_name", String.class)
        .build();
    final ResultMap commentResultMap = ResultMap.builder(config, "commentResultMap", Comment.class)
        .addMapping("id",      "comment_id",   int.class,    ResultFlag.ID)
        .addMapping("name",    "comment_name", String.class)
        .addMapping("comment", "comment",      String.class)
        .build();
    // @formatter:on
    config.addResultMap(tagResultMap);
    config.addResultMap(commentResultMap);
    // @formatter:off
    final ResultMap postResultMap = ResultMap.builder(config, "postResultMap", Post.class)
        .addMapping("id", "id", int.class, ResultFlag.ID)
        .addMapping(new ResultMapping.Builder(config, "blog").nestedQueryId("selectBlogByIdAndAuthor")
          .composite("authorId", "author_id", int.class)
          .composite("blogId", "blog_id", int.class)
          .build())
        .addMapping("createdOn", "created_on", Date.class)
        .addMapping("section", "section", Section.class)
        .addMapping("subject",   "subject",    String.class)
        .addMapping("body",      "body",       String.class)
        .addNestedMapping("tags", tagResultMap.getId())
        .addNestedMapping("comments", commentResultMap.getId())
        .build();
    // @formatter:on

    return new MappedStatement.Builder(config, "selectPostsForBlog", sqlSource, SqlCommandType.SELECT)
        .parameterMap(parameterMap).resultMap(postResultMap).build();
  }

  static MappedStatement prepareInsertAuthorMappedStatementWithBeforeAutoKey(final Configuration config) {
    final ResultMap rm = ResultMap.create("keyResultMap", Integer.class);

    MappedStatement kms = new MappedStatement.Builder(config, "insertAuthor!selectKey",
        new StaticSqlSource("SELECT 123456 as id FROM SYSIBM.SYSDUMMY1"), SqlCommandType.SELECT).keyProperty("id")
            .resultMap(rm).build();
    config.addMappedStatement(kms);
    return new MappedStatement.Builder(config, "insertAuthor", new DynamicSqlSource(new TextSqlNode(
        "INSERT INTO author (id,username,password,email,bio,favourite_section) values(#{id},#{username},#{password},#{email},#{bio:VARCHAR},#{favouriteSection})")),
        SqlCommandType.INSERT)
    // @formatter:off
            .parameterMap(ParameterMap.builder(config,"defaultParameterMap", Author.class)
                .addMapping("id",               Integer.class)
                .addMapping("username",          String.class)
                .addMapping("password",          String.class)
                .addMapping("email",             String.class)
                .addMapping("bio",               String.class,  JdbcType.VARCHAR)
                .addMapping("favouriteSection",  Section.class, JdbcType.VARCHAR)
                .build())
            .cache(authorCache)
            .keyGenerator(new SelectKeyGenerator(kms, true))
            .keyProperty("id")
            .build();
            // @formatter:on
  }

  private ExecutorTestHelper() {
  }

}
