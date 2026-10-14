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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.JsonObject;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;

final class CloudEventsSupport {

	private static final Set<String> CORE_ATTRIBUTES = new HashSet<>(Arrays.asList(
			"specversion", "id", "source", "type", "subject", "time", "datacontenttype", "dataschema"));

	private CloudEventsSupport() {
	}

	static JsonObject detect(BytesXMLMessage message) {
		JsonObject attributes = new JsonObject();
		addIfPresent(attributes, "datacontenttype", message.getHTTPContentType());
		SDTMap props = message.getProperties();
		if (props != null) {
			try {
				Iterator<String> it = props.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					String normalized = normalizeAttributeName(key);
					if (normalized != null) {
						Object value = props.get(key);
						if (value != null) attributes.addProperty(normalized, value.toString());
					}
				}
			} catch (SDTException e) {
				return null;
			}
		}
		if (!attributes.has("specversion") || !attributes.has("id") || !attributes.has("source") || !attributes.has("type")) {
			return null;
		}
		JsonObject event = new JsonObject();
		event.add("attributes", attributes);
		event.addProperty("mode", "binary");
		return event;
	}

	private static String normalizeAttributeName(String key) {
		String lower = key.toLowerCase();
		if (lower.startsWith("ce_") || lower.startsWith("ce-")) {
			return lower.substring(3);
		}
		return CORE_ATTRIBUTES.contains(lower) ? lower : null;
	}

	private static void addIfPresent(JsonObject json, String key, String value) {
		if (value != null && !value.isEmpty()) json.addProperty(key, value);
	}
}
