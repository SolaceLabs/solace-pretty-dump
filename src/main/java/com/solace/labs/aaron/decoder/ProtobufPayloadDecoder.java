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

import java.lang.reflect.Method;
import java.util.Map.Entry;

import com.google.protobuf.MessageOrBuilder;
import com.solace.labs.aaron.ConfigState;
import com.solace.labs.aaron.ProtoBufUtils;
import com.solace.labs.topic.Sub;

final class ProtobufPayloadDecoder implements PayloadDecoder {

	private final ConfigState config;

	ProtobufPayloadDecoder(ConfigState config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return "protobuf";
	}

	@Override
	public boolean supports(DecodeContext context) {
		Entry<Sub, Method> matchingEntry = null;
		for (Entry<Sub, Method> entry : config.getProtobufCallbacks().entrySet()) {
			if (entry.getKey().matches(context.getTopicName())) {
				if (matchingEntry != null) return false;
				matchingEntry = entry;
			}
		}
		return matchingEntry != null;
	}

	@Override
	public DecodeResult decode(DecodeContext context) throws Exception {
		Entry<Sub, Method> matchingEntry = null;
		for (Entry<Sub, Method> entry : config.getProtobufCallbacks().entrySet()) {
			if (entry.getKey().matches(context.getTopicName())) {
				if (matchingEntry != null) return null;
				matchingEntry = entry;
			}
		}
		if (matchingEntry == null) return null;
		Object decoded = matchingEntry.getValue().invoke(null, context.getPayload());
		MessageOrBuilder protoMsg = (MessageOrBuilder)decoded;
		return new DecodeResult(protoMsg.getClass().getSimpleName() + " ProtoBuf",
				ProtoBufUtils.decode(protoMsg, config.getFormattingIndent()));
	}
}
