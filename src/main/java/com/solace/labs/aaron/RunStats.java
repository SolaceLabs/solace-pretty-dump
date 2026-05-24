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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

final class RunStats {

	private final long startMs = System.currentTimeMillis();
	private long lastLivePrintMs = startMs;
	private long printedMessages = 0;
	private long malformedPayloads = 0;
	private int largestPayloadBytes = 0;
	private String largestPayloadDestination = "";
	private final Map<String, Integer> topics = new HashMap<>();
	private final Map<String, Integer> payloadTypes = new HashMap<>();

	synchronized void record(MessageObject message) {
		printedMessages++;
		String destination = message.orig.getDestination() == null ? "<unknown>" : message.orig.getDestination().getName();
		increment(topics, destination);
		recordPayload(message.binary, destination);
		recordPayload(message.xml, destination);
		if (message.hasValidationMessages()) malformedPayloads++;
	}

	synchronized boolean shouldPrintLive(long intervalMs) {
		long now = System.currentTimeMillis();
		if (now - lastLivePrintMs >= intervalMs) {
			lastLivePrintMs = now;
			return true;
		}
		return false;
	}

	synchronized String render() {
		long elapsedMs = Math.max(1, System.currentTimeMillis() - startMs);
		StringBuilder sb = new StringBuilder();
		sb.append("PrettyDump summary: printed=").append(printedMessages)
				.append(", rate=").append(String.format("%.2f", printedMessages * 1000.0 / elapsedMs)).append("/s")
				.append(", malformedOrInvalid=").append(malformedPayloads)
				.append(", largestPayloadBytes=").append(largestPayloadBytes);
		if (!largestPayloadDestination.isEmpty()) sb.append(", largestPayloadDestination=").append(largestPayloadDestination);
		sb.append(", payloadTypes=").append(topEntries(payloadTypes, 5));
		sb.append(", topDestinations=").append(topEntries(topics, 5));
		return sb.toString();
	}

	private void recordPayload(PayloadSection payload, String destination) {
		if (payload == null) return;
		increment(payloadTypes, payload.type == null ? "<unknown>" : payload.type);
		if (payload.type != null && (payload.type.contains("INVALID") || payload.type.startsWith("non "))) malformedPayloads++;
		if (payload.size > largestPayloadBytes) {
			largestPayloadBytes = payload.size;
			largestPayloadDestination = destination;
		}
	}

	private void increment(Map<String, Integer> counts, String key) {
		Integer count = counts.get(key);
		counts.put(key, count == null ? 1 : count + 1);
	}

	private String topEntries(Map<String, Integer> counts, int max) {
		TreeMap<String, Integer> sorted = new TreeMap<>(new Comparator<String>() {
			@Override
			public int compare(String left, String right) {
				int byCount = counts.get(right).compareTo(counts.get(left));
				return byCount != 0 ? byCount : left.compareTo(right);
			}
		});
		sorted.putAll(counts);
		StringBuilder sb = new StringBuilder("[");
		int printed = 0;
		for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
			if (printed > 0) sb.append(", ");
			sb.append(entry.getKey()).append("=").append(entry.getValue());
			printed++;
			if (printed >= max) break;
		}
		return sb.append("]").toString();
	}
}
