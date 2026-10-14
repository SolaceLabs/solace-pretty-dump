/*
 * Copyright 2026 Solace Corporation. All rights reserved.
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

final class ExitCodes {
	static final int OK = 0;
	static final int CLI_USAGE = 2;
	static final int SAFETY_CONFIRMATION_REQUIRED = 3;
	static final int CONNECTION_OR_BROKER = 4;
	static final int INTERNAL_ERROR = 5;

	private ExitCodes() {
	}
}
