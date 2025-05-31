/*
 *    Copyright 2009-2025 the original author or authors.
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
package org.apache.ibatis.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CollectionUtilsTest {

  @Test
  void testAsList() {
    List<Integer> list = CollectionUtils.asList();
    Assertions.assertSame(list, Collections.emptyList());
    List<Integer> list1 = CollectionUtils.asList(1);
    Assertions.assertSame(list1.getClass(), Collections.singletonList(1).getClass());
  }

  @Test
  void shouldCheckUnmodifiableCollection() {
    Assertions.assertTrue(CollectionUtils.isUnmodifiable(Arrays.asList(1, 2)));
    Assertions.assertTrue(CollectionUtils.isUnmodifiable(Collections.singletonList(1)));
    Assertions.assertTrue(CollectionUtils.isUnmodifiable(Collections.unmodifiableList(CollectionUtils.asArrayList(1))));
    Assertions.assertTrue(CollectionUtils.isModifiable(CollectionUtils.asArrayList(1)));
  }

  @Test
  void groupingBy() {
    final int count = 10;
    List<Blog> blogs = createBlogs(count);

    // duplicate id
    blogs.add(blogs.get(0));

    Map<Long, Blog> map = CollectionUtils.toMap(blogs, Blog::getId);
    Assertions.assertEquals(count, map.size());

    Map<String, Blog> map1 = CollectionUtils.toMap(blogs, Blog::getTitle);
    Map<Long, List<Blog>> groupById = CollectionUtils.groupingBy(blogs, Blog::getId);
    Map<String, List<Blog>> groupByTitle = CollectionUtils.groupingBy(blogs, Blog::getTitle);

    MultiValueMap<Long, Blog> groupById1 = CollectionUtils.toMultiValueMap(blogs, Blog::getId);

    List<Blog> blogs1 = groupById1.flattenValues();
    Set<Blog> blogs2 = groupById1.flattenValues(Collectors.toSet());

    groupById1.forEach(new BiConsumer<Long, List<Blog>>() {
      @Override
      public void accept(Long id, List<Blog> blogs) {

      }
    });

    groupById1.flatForEach(new BiConsumer<Long, Blog>() {
      @Override
      public void accept(Long id, Blog blog) {

      }
    });
  }

  private static List<Blog> createBlogs(int count) {
    List<Blog> blogs = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Blog blog = new Blog();
      blog.setId((long) i);
      blog.setTitle("blog " + i);
      blogs.add(blog);
    }
    return blogs;
  }

  static class Blog {

    String title;
    Long id;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return "Blog{" + "id=" + id + ", title='" + title + '\'' + '}';
    }
  }
}
