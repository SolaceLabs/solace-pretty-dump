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

package com.solace.labs.aaron.decoder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solace.labs.aaron.AaAnsi;
import com.solace.labs.aaron.ConfigState;
import com.solace.labs.topic.Sub;

final class AvroPayloadDecoder implements PayloadDecoder {

	private static final Logger logger = LogManager.getLogger(AvroPayloadDecoder.class);

	private final ConfigState config;
	private final Map<String, Schema> schemaCache = new HashMap<>();
	private final List<SchemaMapping> schemaMappings = new ArrayList<>();
	private boolean mappingsLoaded = false;

	AvroPayloadDecoder(ConfigState config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return "avro";
	}

	@Override
	public boolean supports(DecodeContext context) {
		return isAvroContainer(context.getPayload())
				|| context.getContentType().toLowerCase().contains("avro")
				|| schemaForTopic(context.getTopicName()) != null
				|| config.getAvroSchemaFiles().size() == 1;
	}

	@Override
	public DecodeResult decode(DecodeContext context) throws Exception {
		if (isAvroContainer(context.getPayload())) {
			return decodeContainer(context.getPayload());
		}
		Schema schema = schemaForTopic(context.getTopicName());
		if (schema == null && config.getAvroSchemaFiles().size() == 1) {
			schema = loadSchema(config.getAvroSchemaFiles().get(0));
		}
		if (schema == null) return null;
		GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
		BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(context.getPayload(), null);
		GenericRecord record = reader.read(null, decoder);
		return new DecodeResult("Avro " + schema.getFullName(), AaAnsi.n().a(record.toString()));
	}

	private DecodeResult decodeContainer(byte[] payload) throws IOException {
		GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>();
		StringBuilder sb = new StringBuilder();
		Schema schema = null;
		try (DataFileStream<GenericRecord> stream = new DataFileStream<>(new ByteArrayInputStream(payload), reader)) {
			schema = stream.getSchema();
			int count = 0;
			while (stream.hasNext()) {
				if (count > 0) sb.append('\n');
				sb.append(stream.next().toString());
				count++;
			}
		}
		return new DecodeResult("Avro DataFile " + (schema == null ? "" : schema.getFullName()), AaAnsi.n().a(sb.toString()));
	}

	private Schema schemaForTopic(String topic) {
		loadMappingsIfNeeded();
		for (SchemaMapping mapping : schemaMappings) {
			if (mapping.subscription.matches(topic)) {
				return loadSchema(mapping.schemaPath);
			}
		}
		return null;
	}

	private void loadMappingsIfNeeded() {
		if (mappingsLoaded) return;
		mappingsLoaded = true;
		for (String spec : config.getSchemaMapSpecs()) {
			int equals = spec.indexOf('=');
			if (equals <= 0 || equals == spec.length() - 1) {
				logger.warn("Ignoring invalid schema map '{}'. Expected topic-subscription=path", spec);
				continue;
			}
			schemaMappings.add(new SchemaMapping(new Sub(spec.substring(0, equals)), spec.substring(equals + 1)));
		}
	}

	private Schema loadSchema(String path) {
		try {
			String resolved = resolveSchemaPath(path);
			Schema cached = schemaCache.get(resolved);
			if (cached != null) return cached;
			Schema parsed = new Schema.Parser().parse(new String(Files.readAllBytes(new File(resolved).toPath()), "UTF-8"));
			schemaCache.put(resolved, parsed);
			return parsed;
		} catch (IOException | RuntimeException e) {
			logger.warn("Could not load Avro schema {}", path, e);
			return null;
		}
	}

	private String resolveSchemaPath(String path) {
		File file = new File(path);
		if (file.isAbsolute() || config.getAvroSchemaDir() == null) return file.getPath();
		return new File(config.getAvroSchemaDir(), path).getPath();
	}

	private boolean isAvroContainer(byte[] payload) {
		return payload.length >= 4 && payload[0] == 'O' && payload[1] == 'b' && payload[2] == 'j' && payload[3] == 1;
	}

	private static final class SchemaMapping {
		private final Sub subscription;
		private final String schemaPath;

		SchemaMapping(Sub subscription, String schemaPath) {
			this.subscription = subscription;
			this.schemaPath = schemaPath;
		}
	}
}
