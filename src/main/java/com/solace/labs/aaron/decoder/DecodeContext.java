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

import com.solace.labs.aaron.ConfigState;
import com.solacesystems.jcsmp.BytesXMLMessage;

public final class DecodeContext {

	private final ConfigState config;
	private final BytesXMLMessage message;
	private final byte[] payload;
	private final String contentType;

	public DecodeContext(ConfigState config, BytesXMLMessage message, byte[] payload, String contentType) {
		this.config = config;
		this.message = message;
		this.payload = payload;
		this.contentType = contentType == null ? "" : contentType;
	}

	public ConfigState getConfig() {
		return config;
	}

	public BytesXMLMessage getMessage() {
		return message;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getContentType() {
		return contentType;
	}

	public String getTopicName() {
		if (message == null) return "";
		if (message.getDestination() == null) return "";
		return message.getDestination().getName();
	}
}
