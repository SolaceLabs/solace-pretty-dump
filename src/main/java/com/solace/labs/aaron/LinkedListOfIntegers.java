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

package com.solace.labs.aaron;

/**
 * This utility class is to maintain a ordered-by-insertion-time list of integers, representing
 * either the length of a topic or the number of levels in the topic, capped to some max.
 */
class LinkedListOfIntegers {

	private static int DEFAULT_MAX_SIZE = 50;
	
	private final int maxSize;
	private int max = 0;
	private int size = 0;
	private LinkedInteger head = null;
	private LinkedInteger tail = null;
	
	public LinkedListOfIntegers() {
		this(DEFAULT_MAX_SIZE);
	}
	
	public LinkedListOfIntegers(int maxSize) {
		this.maxSize = maxSize;
	}
	
	int getMax() {
		return max;
	}
	
	void insert(int value) {
		if (head == null) {  // very first one
			head = new LinkedInteger(value);
			tail = head;
			max = value;
			size = 1;
		} else {
//			int curMax = max;
			max = Math.max(max, value);
			tail.next = new LinkedInteger(value);  // insert the new link at the tail
			tail = tail.next;  // update the tail pointer
			if (size == maxSize) {  // at capacity, have to pop off the front
				int headVal = head.value;
				head = head.next;  // repoint the pointer to the next guy
				if (headVal == max) {  // popping off (one of) the max, need to rescan
					max = rescanForMax();
				}
//				System.out.printf("Inserting: %d, popping: %d, size: %d, oldMax: %d, newMax: %d%n", value, headVal, size, curMax, max);
			} else {  // still growing
				size++;
//				System.out.printf("Inserting: %d, size: %d, oldMax: %d, newMax: %d%n", value, size, curMax, max);
			}
		}
	}
	
	@Override
	public String toString() {
		LinkedInteger cursor = head;
		StringBuilder sb = new StringBuilder();
		sb.append(cursor.value).append(", ");
		while (cursor.next != null) {
			cursor = cursor.next;
			sb.append(cursor.value).append(", ");
		}
		return sb.toString();
	}
	
	private int rescanForMax() {
//		System.out.println("Rescanning for max");
		LinkedInteger current = head;
		int curMax = current.value;
		while (current.next != null) {
			current = current.next;
			curMax = Math.max(curMax, current.value);
		}
		return curMax;
	}
	
	private static class LinkedInteger {
		
		LinkedInteger next = null;
		final int value;
		
		LinkedInteger(int value) {
			this.value = value;
		}
	}	
}
