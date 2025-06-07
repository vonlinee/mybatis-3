package org.apache.ibatis.internal;

/**
 * Consumer for 'forEach' operation.
 *
 * @param <T>
 *          context object, can be an {@link Iterable}
 * @param <E>
 *          Element type of iterable
 */
public interface ForEachConsumer<T, E> {

  /**
   * Accept an iterable element with index.
   *
   * @param ctx
   *          context object, can be an {@link Iterable}
   * @param element
   *          an iterable element
   * @param elementIndex
   *          an element index
   */
  void accept(T ctx, E element, int elementIndex);
}
