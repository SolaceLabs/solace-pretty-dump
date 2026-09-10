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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.solace.labs.aaron.AaAnsi;
import com.solace.labs.aaron.ConfigState;

public class PayloadDecoderRegistryTest {

	@Test
	public void registryReturnsFirstSupportingDecoder() {
		PayloadDecoderRegistry registry = new PayloadDecoderRegistry();
		registry.register(new FixedDecoder("first", false));
		registry.register(new FixedDecoder("second", true));

		DecodeResult result = registry.decodeFirst(new DecodeContext(new ConfigState(), null, new byte[0], null));

		assertEquals("second", result.getType());
		assertEquals("decoded-second", result.getFormatted().toRawString());
	}

	@Test
	public void registryReturnsNullWhenNothingSupportsPayload() {
		PayloadDecoderRegistry registry = new PayloadDecoderRegistry();
		registry.register(new FixedDecoder("first", false));

		assertNull(registry.decodeFirst(new DecodeContext(new ConfigState(), null, new byte[0], null)));
	}

	private static final class FixedDecoder implements PayloadDecoder {
		private final String name;
		private final boolean supports;

		FixedDecoder(String name, boolean supports) {
			this.name = name;
			this.supports = supports;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean supports(DecodeContext context) {
			return supports;
		}

		@Override
		public DecodeResult decode(DecodeContext context) {
			return new DecodeResult(name, AaAnsi.n().a("decoded-" + name));
		}
	}
}
