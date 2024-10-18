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

public class Col {

	
	final int value;
	final boolean faint;
	final boolean italics;
	
	public Col(int value) {
		this.value = value;
		faint = false;
		italics = false;
	}
	
	public Col(int value, boolean faint) {
		this.value = value;
		this.faint = faint;
		italics = false;
	}

	public Col(int value, boolean faint, boolean italics) {
		this.value = value;
		this.faint = faint;
		this.italics = italics;
	}

	@Override
	public String toString() {
		return (faint ? "Faint " : "") + value;
	}
}
