/*
 * Copyright 2023-2024 Solace Corporation. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.solace.labs.aaron.utils;

import java.util.Iterator;

/**
 * This utility class is to maintain a ordered-by-insertion-time list of integers, representing
 * either the length of a topic or the number of levels in the topic, capped to some max.
 * NOT THREAD SAFE!
 */
public class BoundedLinkedList<T> implements Iterable<T> {
	
	public static class ComparableList<U extends java.lang.Comparable<U>> extends BoundedLinkedList<U> {
		
		protected LinkedNode<U> max = null;
		protected LinkedNode<U> min = null;
		
		public ComparableList(int maxSize) {
			super(maxSize);
		}

		public U getMax() {
			if (max == null) return null;
			return max.item;
		}
		
		public U getMin() {
			if (min == null) return null;
			return min.item;
		}
		
		public U add(U item) {
			LinkedNode<U> prev = addPrv(item);
			return prev == null ? null : prev.item;
		}
		
		protected LinkedNode<U> addPrv(U item) {
			LinkedNode<U> oldHead = super.addPrv(item);
			if (head == tail) {  // first one
				max = tail;
				min = tail;
				return null;
			} else {
				if (item.compareTo(max.item) >= 0) max = tail;  // always inserting at the tail
				else if (oldHead == max) {  // popped off the max
					rescanForMax();
				}
				if (item.compareTo(min.item) <= 0) min = tail;
				else if (oldHead == min) {
					rescanForMin();
				}
				return oldHead;
			}
		}
		
		private void rescanForMax() {
//			System.out.println("Rescanning for max");
			max = head;  // needed in case list is only length 1
			LinkedNode<U> current = head;
			while (current.next != null) {
				current = current.next;
				if (current.item.compareTo(max.item) >= 0) max = current;
			}
		}
		
		private void rescanForMin() {
//			System.out.println("Rescanning for min");
			min = head;
			LinkedNode<U> current = head;
			while (current.next != null) {
				current = current.next;
				if (current.item.compareTo(min.item) <= 0) min = current;
			}
		}
	}  // end of INNER class Comparable

	protected final int capacity;
	protected int size = 0;
	protected LinkedNode<T> head = null;
	protected LinkedNode<T> tail = null;
	
	public BoundedLinkedList(int maxSize) {
		assert maxSize > 0;
		this.capacity = maxSize;
	}

	public int size() {
		return size;
	}

	public int capacity() {
		return capacity;
	}
	
	public T add(T item) {
		LinkedNode<T> prev = addPrv(item);
		return prev == null ? null : prev.item;
	}

	/** If the linked list is full, returns the head that got popped off; null otherwise */
	protected LinkedNode<T> addPrv(T item) {
		if (head == null) {  // very first one
			head = new LinkedNode<T>(item);
			tail = head;
			size = 1;
			return null;
		} else {
			tail.next = new LinkedNode<T>(item);  // insert the new link at the tail
			tail = tail.next;  // update the tail pointer
			if (size == capacity) {  // at capacity, have to pop off the front
				LinkedNode<T> oldHead = head;  // pop off the head
				head = head.next;  // repoint the pointer to the next guy
//				System.out.printf("Inserting: %d, popping: %d, size: %d, oldMax: %d, newMax: %d%n", value, headVal, size, curMax, max);
				return oldHead;
			} else {  // still growing
				size++;
//				System.out.printf("Inserting: %d, size: %d, oldMax: %d, newMax: %d%n", value, size, curMax, max);
				return null;
			}
		}
	}
	
	@Override
	public String toString() {
		LinkedNode<T> cursor = head;
		StringBuilder sb = new StringBuilder();
		sb.append(cursor.item).append(", ");
		while (cursor.next != null) {
			cursor = cursor.next;
			sb.append(cursor.item).append(", ");
		}
		return sb.toString();
	}
	
	private class LinkedListIterator implements Iterator<T> {

		LinkedNode<T> cursor = new LinkedNode<T>(head);  // start before the head
		
		@Override
		public boolean hasNext() {
			return cursor.next != null;
		}

		@Override
		public T next() {
			assert cursor.next != null;
			cursor = cursor.next;
			return cursor.item;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new LinkedListIterator();
	}	

	// my nodes
	private static class LinkedNode<T> {
		
		private LinkedNode<T> next;
		private final T item;

		/** An empty node not pointing to anything yet */
		private LinkedNode(T item) {
			this.item = item;
		}

		private LinkedNode(LinkedNode<T> next) {
			this.item = null;
			this.next = next;
		}
	}
}
