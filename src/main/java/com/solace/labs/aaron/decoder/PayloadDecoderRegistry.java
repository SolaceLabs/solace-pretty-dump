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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solace.labs.aaron.ConfigState;

public final class PayloadDecoderRegistry {

	private static final Logger logger = LogManager.getLogger(PayloadDecoderRegistry.class);

	private final List<PayloadDecoder> decoders = new ArrayList<>();

	public static PayloadDecoderRegistry createDefault(ConfigState config) {
		PayloadDecoderRegistry registry = new PayloadDecoderRegistry();
		registry.register(new AvroPayloadDecoder(config));
		registry.register(new ProtobufPayloadDecoder(config));
		return registry;
	}

	public void register(PayloadDecoder decoder) {
		decoders.add(decoder);
	}

	public List<PayloadDecoder> getDecoders() {
		return Collections.unmodifiableList(decoders);
	}

	public DecodeResult decodeFirst(DecodeContext context) {
		for (PayloadDecoder decoder : decoders) {
			try {
				if (decoder.supports(context)) {
					return decoder.decode(context);
				}
			} catch (Exception e) {
				logger.warn("Decoder {} failed", decoder.getName(), e);
				return null;
			}
		}
		return null;
	}
}
