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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.solace.labs.aaron.ConfigState.ValidationMode;
import com.solace.labs.topic.Sub;

final class SchemaValidationSupport {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
	private static final Map<String, JsonSchema> SCHEMA_CACHE = new HashMap<>();

	private SchemaValidationSupport() {
	}

	static void validateIfConfigured(ConfigState config, MessageObject message) {
		if (config.getValidationMode() == ValidationMode.OFF) return;
		String schemaPath = schemaPathForMessage(config, message);
		if (schemaPath == null || !looksLikeJsonSchema(schemaPath)) return;
		String payload = payloadForValidation(message);
		if (payload == null || payload.trim().isEmpty()) return;
		try {
			JsonSchema schema = schemaForPath(schemaPath);
			JsonNode payloadNode = MAPPER.readTree(payload);
			Set<ValidationMessage> errors = schema.validate(payloadNode);
			for (ValidationMessage error : errors) {
				message.addValidationMessage(error.getMessage());
			}
			if (!errors.isEmpty() && config.getValidationMode() == ValidationMode.STRICT) {
				config.exitCode = ExitCodes.VALIDATION_FAILED;
			}
		} catch (Exception e) {
			message.addValidationMessage("Could not validate schema '" + schemaPath + "': " + e.getMessage());
			if (config.getValidationMode() == ValidationMode.STRICT) {
				config.exitCode = ExitCodes.VALIDATION_FAILED;
			}
		}
	}

	private static String schemaPathForMessage(ConfigState config, MessageObject message) {
		String topic = message.orig.getDestination() == null ? "" : message.orig.getDestination().getName();
		for (String spec : config.getSchemaMapSpecs()) {
			int equals = spec.indexOf('=');
			if (equals <= 0 || equals == spec.length() - 1) continue;
			if (new Sub(spec.substring(0, equals)).matches(topic)) {
				String schemaPath = spec.substring(equals + 1);
				if (config.getAvroSchemaDir() != null && !new File(schemaPath).isAbsolute()) {
					return new File(config.getAvroSchemaDir(), schemaPath).getPath();
				}
				return schemaPath;
			}
		}
		return null;
	}

	private static String payloadForValidation(MessageObject message) {
		if (message.binary != null && message.binary.type != null && message.binary.type.toLowerCase().contains("json")) {
			return message.binary.formatted.toRawString();
		}
		if (message.xml != null && message.xml.type != null && message.xml.type.toLowerCase().contains("json")) {
			return message.xml.formatted.toRawString();
		}
		return null;
	}

	private static JsonSchema schemaForPath(String path) throws IOException {
		JsonSchema schema = SCHEMA_CACHE.get(path);
		if (schema != null) return schema;
		JsonSchema parsed = SCHEMA_FACTORY.getSchema(MAPPER.readTree(new File(path)));
		SCHEMA_CACHE.put(path, parsed);
		return parsed;
	}

	private static boolean looksLikeJsonSchema(String path) {
		String lower = path.toLowerCase();
		return lower.endsWith(".json") || lower.endsWith(".schema") || lower.endsWith(".schema.json");
	}
}
