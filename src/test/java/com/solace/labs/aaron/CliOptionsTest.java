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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class CliOptionsTest {

	@Test
	public void noArgsUsesLegacyDefaults() {
		CliOptions options = CliOptions.parse();

		assertEquals("localhost", options.getHost());
		assertEquals("default", options.getVpn());
		assertEquals("foo", options.getUsername());
		assertEquals("bar", options.getPassword());
		assertArrayEquals(new String[] { "#noexport/>" }, options.getTopics());
		assertNull(options.getIndentArg());
		assertFalse(options.isShortcutMode());
	}

	@Test
	public void shortcutTopicAddsDefaultConnectionArgs() {
		CliOptions options = CliOptions.parse("solace/>", "-30", "--trim");

		assertTrue(options.isShortcutMode());
		assertEquals("localhost", options.getHost());
		assertEquals("default", options.getVpn());
		assertEquals("foo", options.getUsername());
		assertEquals("bar", options.getPassword());
		assertArrayEquals(new String[] { "solace/>" }, options.getTopics());
		assertEquals("-30", options.getIndentArg());
		assertEquals(Arrays.asList("--trim"), options.getSpecialArgs());
		assertEquals(Arrays.asList("localhost", "default", "foo", "bar", "solace/>", "-30"), options.getRegularArgs());
	}

	@Test
	public void shortcutQueueBrowseUsesDefaultConnectionArgs() {
		CliOptions options = CliOptions.parse("b:q1", "00", "--count=-10");

		assertTrue(options.isShortcutMode());
		assertArrayEquals(new String[] { "b:q1" }, options.getTopics());
		assertEquals("00", options.getIndentArg());
		assertEquals(Arrays.asList("--count=-10"), options.getSpecialArgs());
	}

	@Test
	public void singleIndentUsesDefaultTopicShortcut() {
		CliOptions options = CliOptions.parse("-1");

		assertTrue(options.isShortcutMode());
		assertArrayEquals(new String[] { "#noexport/>" }, options.getTopics());
		assertEquals("-1", options.getIndentArg());
	}

	@Test
	public void explicitConnectionArgumentsArePreserved() {
		CliOptions options = CliOptions.parse("tcps://broker:55443", "vpn", "user", "pw", "a/>,b/>", "2");

		assertFalse(options.isShortcutMode());
		assertEquals("tcps://broker:55443", options.getHost());
		assertEquals("vpn", options.getVpn());
		assertEquals("user", options.getUsername());
		assertEquals("pw", options.getPassword());
		assertArrayEquals(new String[] { "a/>", "b/>" }, options.getTopics());
		assertEquals("2", options.getIndentArg());
	}

	@Test
	public void helpAndWrapModesAreDetected() {
		assertTrue(CliOptions.parse("--help").isHelp());
		assertTrue(CliOptions.parse("-hm").isHelpMore());
		assertTrue(CliOptions.parse("-he").isHelpExamples());
		assertTrue(CliOptions.parse("wrap").isWrapMode());
	}

	@Test
	public void structuredOutputFlagsAreDetectedButRemainSpecialArgs() {
		CliOptions options = CliOptions.parse(">", "--output=jsonl", "--no-ansi", "--no-banner", "--quiet");

		assertEquals("jsonl", options.getOutputMode());
		assertTrue(options.isNoAnsi());
		assertTrue(options.isNoBanner());
		assertTrue(options.isQuiet());
		assertTrue(options.getSpecialArgs().contains("--output=jsonl"));
		assertTrue(options.getSpecialArgs().contains("--no-ansi"));
	}

	@Test
	public void nonInteractiveFlagsAreDetectedButRemainSpecialArgs() {
		CliOptions options = CliOptions.parse("b:q1", "--non-interactive", "--yes-consume",
				"--exit-on-empty", "--empty-timeout-ms=250", "--max-runtime-ms=1000");

		assertTrue(options.isNonInteractive());
		assertTrue(options.isYesConsume());
		assertTrue(options.isExitOnEmpty());
		assertEquals("250", options.getEmptyTimeoutMs());
		assertEquals("1000", options.getMaxRuntimeMs());
		assertTrue(options.getSpecialArgs().contains("--non-interactive"));
		assertTrue(options.getSpecialArgs().contains("--max-runtime-ms=1000"));
	}

	@Test
	public void avroSchemaFlagsAreDetectedButRemainSpecialArgs() {
		CliOptions options = CliOptions.parse(">", "--avro-schema=order.avsc",
				"--avro-schema-dir=schemas", "--schema-map=orders/>=order.avsc");

		assertEquals(Arrays.asList("order.avsc"), options.getAvroSchemas());
		assertEquals("schemas", options.getAvroSchemaDir());
		assertEquals(Arrays.asList("orders/>=order.avsc"), options.getSchemaMaps());
		assertTrue(options.getSpecialArgs().contains("--schema-map=orders/>=order.avsc"));
	}

	@Test
	public void cloudEventsAndValidationFlagsAreDetectedButRemainSpecialArgs() {
		CliOptions options = CliOptions.parse(">", "--cloudevents=require", "--validate-schema=strict");

		assertEquals("require", options.getCloudEventsMode());
		assertEquals("strict", options.getValidationMode());
		assertTrue(options.getSpecialArgs().contains("--cloudevents=require"));
		assertTrue(options.getSpecialArgs().contains("--validate-schema=strict"));
	}

	@Test
	public void timestampFlagsAreDetectedButRemainSpecialArgs() {
		CliOptions options = CliOptions.parse(">", "--time=all", "--clock-source=white-rabbit");

		assertEquals("all", options.getTimeMode());
		assertEquals("white_rabbit", options.getClockSource());
		assertTrue(options.getSpecialArgs().contains("--time=all"));
		assertTrue(options.getSpecialArgs().contains("--clock-source=white-rabbit"));
	}

	@Test
	public void sempCopyTailFlagsAreDetectedButRemainSpecialArgs() {
		CliOptions options = CliOptions.parse("b:q1", "--copy-tail=25", "--semp-url=http://localhost:8080",
				"--semp-user=admin", "--semp-password-env=SEMP_PASSWORD");

		assertEquals("25", options.getCopyTail());
		assertEquals("http://localhost:8080", options.getSempUrl());
		assertEquals("admin", options.getSempUser());
		assertEquals("SEMP_PASSWORD", options.getSempPasswordEnv());
		assertTrue(options.getSpecialArgs().contains("--copy-tail=25"));
	}
}
