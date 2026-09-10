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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.solace.labs.aaron.ConfigState;

public class AvroPayloadDecoderTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void decodesRawAvroWithConfiguredSchema() throws Exception {
		String schemaJson = "{"
				+ "\"type\":\"record\","
				+ "\"name\":\"Order\","
				+ "\"namespace\":\"example\","
				+ "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"}]"
				+ "}";
		File schemaFile = temp.newFile("order.avsc");
		Files.write(schemaFile.toPath(), schemaJson.getBytes(StandardCharsets.UTF_8));
		Schema schema = new Schema.Parser().parse(schemaJson);
		GenericRecord record = new GenericData.Record(schema);
		record.put("id", "A123");
		record.put("quantity", 7);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
		BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(bytes, null);
		writer.write(record, encoder);
		encoder.flush();

		ConfigState config = new ConfigState();
		config.addAvroSchemaFile(schemaFile.getAbsolutePath());
		AvroPayloadDecoder decoder = new AvroPayloadDecoder(config);
		DecodeResult result = decoder.decode(new DecodeContext(config, null, bytes.toByteArray(), "avro/binary"));

		assertTrue(result.getType().contains("example.Order"));
		assertTrue(result.getFormatted().toRawString().contains("A123"));
		assertTrue(result.getFormatted().toRawString().contains("quantity"));
	}
}
