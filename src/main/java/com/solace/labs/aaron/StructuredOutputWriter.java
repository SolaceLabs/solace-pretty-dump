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

import java.io.PrintStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.solace.labs.aaron.ConfigState.OutputMode;

final class StructuredOutputWriter {

	private final OutputMode outputMode;
	private final PrintStream out;
	private final StructuredMessageRenderer renderer = new StructuredMessageRenderer();
	private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	private boolean firstJsonObject = true;
	private boolean closed = false;

	StructuredOutputWriter(OutputMode outputMode, PrintStream out) {
		if (outputMode == OutputMode.TEXT) throw new IllegalArgumentException("Structured writer requires JSON or JSONL mode");
		this.outputMode = outputMode;
		this.out = out;
		if (outputMode == OutputMode.JSON) {
			out.println("[");
		}
	}

	synchronized void print(MessageObject message) {
		if (closed) return;
		JsonObject json = renderer.render(message);
		if (outputMode == OutputMode.JSONL) {
			out.println(gson.toJson(json));
		} else {
			if (!firstJsonObject) out.println(",");
			out.print(gson.toJson(json));
			firstJsonObject = false;
		}
		out.flush();
	}

	synchronized void finish() {
		if (closed) return;
		if (outputMode == OutputMode.JSON) {
			if (!firstJsonObject) out.println();
			out.println("]");
		}
		out.flush();
		closed = true;
	}
}
