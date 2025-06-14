package org.apache.ibatis.internal.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class ArraySet<E> extends AbstractSet<E> implements Serializable, Cloneable, RandomAccess {

  private static final long serialVersionUID = 6518404858591999463L;

  /**
   * The ArrayList on which this set implementation is based
   */
  @NotNull
  private ArrayList<E> list;

  /**
   * Constructs an empty set with an initial capacity of ten.
   */
  public ArraySet() {
    list = new ArrayList<>();
  }

  /**
   * Constructs an empty set with the specified initial capacity.
   *
   * @param initialCapacity
   *          the initial capacity of the set
   *
   * @throws IllegalArgumentException
   *           if the specified initial capacity is negative
   */
  public ArraySet(int initialCapacity) {
    list = new ArrayList<>(initialCapacity);
  }

  /**
   * Constructs a set containing the unique elements of the specified collection, in the order they are returned by the
   * collection's iterator.
   *
   * @param c
   *          the collection whose elements are to be placed into this list
   *
   * @throws NullPointerException
   *           if the specified collection is null
   */
  public ArraySet(Collection<? extends E> c) {
    if (c instanceof Set) {
      list = new ArrayList<>(c);
    } else {
      list = new ArrayList<>(c.size() / 2);
      int index = 0;
      for (E element : c) {
        if (element == null) {
          throw new IllegalArgumentException("element at #" + index + " is null");
        }
        if (contains(element)) {
          continue;
        }
        list.add(element);
      }
      list.trimToSize();
    }
  }

  /**
   * Add an element to the set. Will always be successful if the element isn't yet contained in the set.
   *
   * @param element
   *          The element to be added
   *
   * @return <tt>true</tt> if the element was added, <tt>false</tt> otherwise
   *
   * @throws IllegalArgumentException
   *           if element was set to null
   */
  @Override
  public boolean add(final E element) {
    if (element == null) {
      throw new IllegalArgumentException();
    }
    if (contains(element)) {
      return false;
    }
    return list.add(element);
  }

  /**
   * Inserts the specified element at the specified position in this set if it isn't yet contained in the set. Shifts
   * the element currently at that position (if any) and any subsequent elements to the right (adds one to their
   * indices).
   *
   * @param index
   *          index at which the specified element is to be inserted
   * @param element
   *          element to be inserted
   *
   * @throws IndexOutOfBoundsException
   *           if the index is out of range (<tt>index < 0 || index > size()</tt>)
   * @throws IllegalArgumentException
   *           if element was set to null
   * @throws UnsupportedOperationException
   *           if the element is already contained in the set
   */
  public void add(int index, E element) {
    if (element == null) {
      throw new IllegalArgumentException();
    }
    if (contains(element)) {
      throw new UnsupportedOperationException("element exits!");
    }
    list.add(index, element);
  }

  /**
   * Checks whether this set contains the element.
   *
   * @param element
   *          The element to be checked
   *
   * @return <tt>true</tt> if this set contains the element, <tt>false</tt> otherwise
   *
   * @throws IllegalArgumentException
   *           if element was set to null
   */
  @Override
  public boolean contains(Object element) {
    // Check input parameter
    if (element == null) {
      throw new IllegalArgumentException("element is null");
    }
    return list.contains(element);
  }

  /**
   * Returns an iterator over the elements in this set in proper sequence.
   *
   * @return an iterator over the elements in this set in proper sequence
   */
  @Override
  public @NotNull Iterator<E> iterator() {
    return list.iterator();
  }

  /**
   * Returns the number of elements in this set.
   *
   * @return the number of elements in this set
   */
  @Override
  public int size() {
    return list.size();
  }

  /**
   * Trims the capacity of this <tt>ArraySet</tt> instance to be the set's current size. An application can use this
   * operation to minimize the storage of an <tt>ArraySet</tt> instance.
   */
  public void trimToSize() {
    list.trimToSize();
  }

  /**
   * Increases the capacity of this <tt>ArraySet</tt> instance, if necessary, to ensure that it can hold at least the
   * number of elements specified by the minimum capacity argument.
   *
   * @param minCapacity
   *          the desired minimum capacity
   */
  public void ensureCapacity(int minCapacity) {
    list.ensureCapacity(minCapacity);
  }

  /**
   * Returns the index of the specified element in this set, or -1 if this set does not contain the element.
   *
   * @param element
   *          The element whose index is requested
   *
   * @return The index of the element or -1 if the element isn't contained in the set
   *
   * @throws IllegalArgumentException
   *           if element was set to null
   */
  public int indexOf(E element) {
    if (element == null) {
      throw new IllegalArgumentException("element is null");
    }
    return list.indexOf(element);
  }

  /**
   * Returns a shallow copy of this <tt>ArraySet</tt> instance. (The elements themselves are not copied.)
   *
   * @return a clone of this <tt>ArraySet</tt> instance
   */
  @Override
  @SuppressWarnings("unchecked")
  public ArraySet<E> clone() {
    try {
      ArraySet<E> set = (ArraySet<E>) super.clone();
      set.list = new ArrayList<>(list);
      return set;
    } catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  /**
   * Returns an array containing all the elements in this set in proper sequence (from first to last element).
   * <p>
   * The returned array will be "safe" in that no references to it are maintained by this set. (In other words, this
   * method must allocate a new array). The caller is thus free to modify the returned array.
   * <p>
   * This method acts as bridge between array-based and collection-based APIs.
   *
   * @return an array containing all the elements in this set in proper sequence
   */
  @Override
  public Object @NotNull [] toArray() {
    return list.toArray();
  }

  /**
   * Returns an array containing all the elements in this set in proper sequence (from first to last element); the
   * runtime type of the returned array is that of the specified array. If the set fits in the specified array, it is
   * returned therein. Otherwise, a new array is allocated with the runtime type of the specified array and the size of
   * this set.
   * <p>
   * If the set fits in the specified array with room to spare (i.e., the array has more elements than the set), the
   * element in the array immediately following the end of the collection is set to <tt>null</tt>. Since this set
   * doesn't contain a null element, this helps to determine its size in the array.
   *
   * @param a
   *          the array into which the elements of the set are to be stored, if it is big enough; otherwise, a new array
   *          of the same runtime type is allocated for this purpose.
   *
   * @return an array containing the elements of the set
   *
   * @throws ArrayStoreException
   *           if the runtime type of the specified array is not a supertype of the runtime type of every element in
   *           this set
   * @throws NullPointerException
   *           if the specified array is null
   */
  @Override
  public <T> T @NotNull [] toArray(T @NotNull [] a) {
    return list.toArray(a);
  }

  /**
   * Returns the element at the specified position in this set.
   *
   * @param index
   *          index of the element to return
   *
   * @return the element at the specified position in this set
   *
   * @throws IndexOutOfBoundsException
   *           if the index is out of range (<tt>index < 0 || index > size()</tt>)
   */
  public E get(final int index) {
    return list.get(index);
  }

  /**
   * Replaces the element at the specified position in this set with the specified element if it isn't yet contained in
   * the set.
   *
   * @param index
   *          index of the element to replace
   * @param element
   *          element to be stored at the specified position
   *
   * @return the element previously at the specified position
   *
   * @throws IndexOutOfBoundsException
   *           if the index is out of range (<tt>index < 0 || index > size()</tt>)
   * @throws IllegalArgumentException
   *           if element was set to null
   * @throws UnsupportedOperationException
   *           if element is already contained in the set
   */
  public E set(int index, E element) {
    // Check input parameter
    if (element == null) {
      throw new IllegalArgumentException();
    }
    // Set element if it isn't yet contained in the list
    if (contains(element)) {
      throw new UnsupportedOperationException();
    }
    return list.set(index, element);
  }

  /**
   * Removes the element at the specified position in this set. Shifts any subsequent elements to the left (subtracts
   * one from their indices).
   *
   * @param index
   *          the index of the element to be removed
   *
   * @return the element that was removed from the list
   *
   * @throws IndexOutOfBoundsException
   *           if the index is out of range (<tt>index < 0 || index > size()</tt>)
   */
  public E remove(int index) {
    E element = list.remove(index);
    trimToSize();
    return element;
  }

  /**
   * Removes the specified element from this set, if it is present. If the set does not contain the element, it is
   * unchanged. Returns <tt>true</tt> if this set contained the specified element (or equivalently, if this set changed
   * as a result of the call).
   *
   * @param element
   *          Element to be removed from this list, if present
   *
   * @return <tt>true</tt> if this list contained the specified element
   *
   * @throws IllegalArgumentException
   *           if the element was set to null
   */
  @Override
  public boolean remove(Object element) {
    // Check input parameter
    if (element == null) {
      throw new IllegalArgumentException();
    }
    // Remove element
    boolean success = list.remove(element);
    trimToSize();
    return success;
  }

  /**
   * Removes all the elements from this set. The set will be empty after this call returns.
   */
  @Override
  public void clear() {
    list.clear();
    trimToSize();
  }

  /**
   * Appends each element of the specified collection to the end of this set if the element isn't yet contained in the
   * set, in the order that each element is returned by the specified collection's Iterator. The behavior of this
   * operation is undefined if the specified collection is modified while the operation is in progress. (This implies
   * that the behavior of this call is undefined if the specified collection is this list, and this list is nonempty.)
   *
   * @param c
   *          collection containing elements to be added to this list
   *
   * @return <tt>true</tt> if this list changed as a result of the call
   *
   * @throws NullPointerException
   *           if the specified collection is null
   */
  @Override
  public boolean addAll(Collection<? extends E> c) {
    boolean changed = false;
    for (E element : c) {
      if (contains(element)) {
        continue;
      }
      changed |= add(element);
    }
    return changed;
  }

  /**
   * Inserts each element of the specified collection to the end of this set if the element isn't yet contained in the
   * set, starting at the specified position. Shifts the element currently at that position (if any) and any subsequent
   * elements to the right (increases their indices). The new elements will appear in the set in the order that they are
   * returned by the specified collection's Iterator.
   *
   * @param index
   *          index at which to insert the first element from the specified collection
   * @param c
   *          collection containing elements to be added to this list
   *
   * @return <tt>true</tt> if this list changed as a result of the call
   *
   * @throws IndexOutOfBoundsException
   *           if the index is out of range (<tt>index < 0 || index > size()</tt>)
   * @throws NullPointerException
   *           if the specified collection is null
   */
  public boolean addAll(int index, Collection<? extends E> c) {
    boolean changed = false;
    // Create an ArraySet to be inserted
    ArraySet<E> set = new ArraySet<>(c);
    // Try to add each single element of the set if it contains any elements
    if (set.size() > 0) {
      for (E e2 : set) {
        try {
          add(index, e2);
          changed = true;
          index++;
        } catch (UnsupportedOperationException ignored) {
        }
      }
    }
    return changed;
  }

  /**
   * Replace the old element with the new one if the set contains the old, but doesn't yet contain the new element.
   *
   * @param oldElement
   *          The old element to be replaced
   * @param newElement
   *          The new element to replace the old one with.
   *
   * @return <tt>true</tt> if and only if the set was changed because the new element replaced the old one,
   *         <tt>false</tt> otherwise.
   *
   * @throws IllegalArgumentException
   *           if oldElement or newElement was set to null
   */
  public boolean replace(E oldElement, E newElement) {
    // Check input parameters
    if (oldElement == null || newElement == null) {
      throw new IllegalArgumentException();
    }
    // Init variables
    boolean changed = false;
    int index;

    // Replace old element if it exists in the set
    if ((index = indexOf(oldElement)) > -1) {
      E result = set(index, newElement);
      if (result.equals(oldElement)) {
        changed = true;
      }
    }
    // Return changed state
    return changed;
  }
}
