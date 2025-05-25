package org.apache.ibatis.scripting.xmltags;

import java.util.List;

import org.apache.ibatis.domain.blog.Post;

public interface Mapper {

  List<Post> findPost();
}
