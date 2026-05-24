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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.Queue;

final class StructuredMessageRenderer {

	JsonObject render(MessageObject msg) {
		JsonObject json = new JsonObject();
		BytesXMLMessage original = msg.orig;

		json.addProperty("messageNumber", msg.lockedMsgCountNumber);
		json.addProperty("prettyDumpReceiveTime", msg.lockedTimestamp);
		json.add("timestamps", TimestampSupport.render(msg));
		addDestination(json, original.getDestination());
		json.addProperty("messageType", msg.msgType);
		addString(json, "deliveryMode", original.getDeliveryMode());
		json.addProperty("priority", original.getPriority());
		addString(json, "classOfService", original.getCos());
		addString(json, "applicationMessageId", original.getApplicationMessageId());
		addString(json, "applicationMessageType", original.getApplicationMessageType());
		addString(json, "correlationId", original.getCorrelationId());
		addString(json, "replyTo", original.getReplyTo());
		addString(json, "httpContentType", original.getHTTPContentType());
		addString(json, "httpContentEncoding", original.getHTTPContentEncoding());
		addString(json, "senderId", original.getSenderId());
		addString(json, "sequenceNumber", original.getSequenceNumber());
		addString(json, "topicSequenceNumber", original.getTopicSequenceNumber());
		addString(json, "senderTimestampMillis", original.getSenderTimestamp());
		json.addProperty("receiveTimestampMillis", original.getReceiveTimestamp());
		addString(json, "expirationMillis", original.getExpiration());
		json.addProperty("timeToLiveMillis", original.getTimeToLive());
		json.addProperty("redelivered", original.getRedelivered());
		json.addProperty("discardIndication", original.getDiscardIndication());
		json.addProperty("dmqEligible", original.isDMQEligible());
		addString(json, "replicationGroupMessageId", original.getReplicationGroupMessageId());
		JsonObject cloudEvent = msg.getConfig().getCloudEventsMode() == ConfigState.CloudEventsMode.OFF ? null : CloudEventsSupport.detect(original);
		if (cloudEvent != null) json.add("cloudEvent", cloudEvent);
		else if (msg.getConfig().getCloudEventsMode() == ConfigState.CloudEventsMode.REQUIRE) json.addProperty("cloudEventMissing", true);
		if (msg.hasValidationMessages()) {
			JsonArray validation = new JsonArray();
			for (String validationMessage : msg.validationMessages) {
				validation.add(validationMessage);
			}
			json.add("validationMessages", validation);
		}

		JsonArray payloads = new JsonArray();
		addPayload(payloads, "binaryAttachment", msg.binary);
		addPayload(payloads, "xmlContent", msg.xml);
		addPayload(payloads, "userData", msg.userData);
		json.add("payloads", payloads);

		if (msg.userProps != null) {
			JsonObject props = new JsonObject();
			props.addProperty("elementCount", msg.userProps.numElements);
			props.addProperty("formatted", msg.userProps.formatted.toRawString());
			json.add("userProperties", props);
		}
		return json;
	}

	private void addDestination(JsonObject json, Destination destination) {
		JsonObject dest = new JsonObject();
		dest.addProperty("type", destination instanceof Queue ? "queue" : "topic");
		dest.addProperty("name", destination.getName());
		json.add("destination", dest);
	}

	private void addPayload(JsonArray payloads, String section, PayloadSection payload) {
		if (payload == null) return;
		JsonObject json = new JsonObject();
		json.addProperty("section", section);
		json.addProperty("sizeBytes", payload.size);
		if (payload.type != null) json.addProperty("type", payload.type);
		json.addProperty("formatted", payload.formatted.toRawString());
		payloads.add(json);
	}

	private void addString(JsonObject json, String name, Object value) {
		if (value != null) json.addProperty(name, value.toString());
	}
}
