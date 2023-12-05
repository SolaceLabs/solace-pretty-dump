package com.solace.labs.aaron;

class LinkedListOfIntegers {

	private static int MAX_SIZE = 50;
	
	private int max = 0;
	private int size = 0;
	private LinkedInteger head = null;
	private LinkedInteger tail = null;
	
	int getMax() {
		return max;
	}
	
	void insert(int value) {
		if (head == null) {
			head = new LinkedInteger(value);
			tail = head;
			max = value;
			size = 1;
		} else {
			tail.next = new LinkedInteger(value);  // insert the new link
			tail = tail.next;  // update the tail pointer
			if (size == MAX_SIZE) {  // at capacity, have to pop off the front
				int headVal = head.value;
				head = head.next;  // repoint the pointer to the next guy
				if (headVal == max) {  // popping off the max, need to rescan
					max = rescanForMax();
				}
			} else {
				size++;
				max = Math.max(max, value);
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
	
	int rescanForMax() {
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
