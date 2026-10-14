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

import java.time.Instant;

import com.google.gson.JsonObject;
import com.solace.labs.aaron.ConfigState.TimeMode;

final class TimestampSupport {

	private TimestampSupport() {
	}

	static JsonObject render(MessageObject message) {
		ConfigState config = message.getConfig();
		JsonObject timestamps = new JsonObject();
		timestamps.addProperty("clockSource", config.getClockSource().name().toLowerCase());
		timestamps.addProperty("localReceiveTimeMillis", message.lockedEpochMillis);
		timestamps.addProperty("localReceiveTimeIso", message.lockedInstant);
		if (shouldInclude(config.getTimeMode(), TimeMode.JCSMP)) {
			long receiveTimestamp = message.orig.getReceiveTimestamp();
			if (receiveTimestamp > 0) {
				timestamps.addProperty("jcsmpReceiveTimestampMillis", receiveTimestamp);
				timestamps.addProperty("jcsmpReceiveTimestampIso", Instant.ofEpochMilli(receiveTimestamp).toString());
			}
		}
		if (shouldInclude(config.getTimeMode(), TimeMode.SENDER)) {
			Long senderTimestamp = message.orig.getSenderTimestamp();
			if (senderTimestamp != null && senderTimestamp > 0) {
				timestamps.addProperty("senderTimestampMillis", senderTimestamp);
				timestamps.addProperty("senderTimestampIso", Instant.ofEpochMilli(senderTimestamp).toString());
				timestamps.addProperty("senderToLocalReceiveLatencyMillis", message.lockedEpochMillis - senderTimestamp);
			}
		}
		if (shouldInclude(config.getTimeMode(), TimeMode.TRACE)) {
			timestamps.addProperty("traceTimestampSource", "decoded-payload");
			timestamps.addProperty("traceTimestampNote", "Broker/OpenTelemetry nanosecond timestamps are shown inside decoded trace payloads when present.");
		}
		return timestamps;
	}

	static String renderSummary(MessageObject message) {
		JsonObject json = render(message);
		return json.toString();
	}

	private static boolean shouldInclude(TimeMode current, TimeMode requested) {
		return current == TimeMode.ALL || current == requested;
	}
}
