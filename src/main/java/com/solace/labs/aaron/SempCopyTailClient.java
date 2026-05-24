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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SempCopyTailClient {

	private static final Pattern RGMID_PATTERN = Pattern.compile("<replication-group-msg-id>([^<]+)</replication-group-msg-id>");

	private final String baseUrl;
	private final String username;
	private final String password;

	SempCopyTailClient(String baseUrl, String username, String password) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.username = username;
		this.password = password;
	}

	int copyNewestMessages(String vpn, String sourceQueue, String destinationQueue, int count) throws IOException {
		List<String> rgmids = newestRgmids(vpn, sourceQueue, count);
		int copied = 0;
		for (String rgmid : rgmids) {
			copyMessage(vpn, sourceQueue, destinationQueue, rgmid);
			copied++;
		}
		return copied;
	}

	List<String> newestRgmids(String vpn, String queue, int count) throws IOException {
		String request = "<rpc><show><queue><name>" + xml(queue) + "</name><vpn-name>" + xml(vpn)
				+ "</vpn-name><messages/><newest/><detail/><count/><num-elements>" + count
				+ "</num-elements></queue></show></rpc>";
		String response = post(request);
		ArrayList<String> rgmids = new ArrayList<>();
		Matcher matcher = RGMID_PATTERN.matcher(response);
		while (matcher.find()) {
			rgmids.add(matcher.group(1));
		}
		if (rgmids.isEmpty()) throw new IOException("SEMP did not return any replication-group-msg-id values");
		return rgmids;
	}

	private void copyMessage(String vpn, String sourceQueue, String destinationQueue, String rgmid) throws IOException {
		String request = "<rpc><admin><message-spool><vpn-name>" + xml(vpn)
				+ "</vpn-name><copy-message><source/><queue-name>" + xml(sourceQueue)
				+ "</queue-name><destination/><queue-name>" + xml(destinationQueue)
				+ "</queue-name><message/><replication-group-msg-id>" + xml(rgmid)
				+ "</replication-group-msg-id></copy-message></message-spool></admin></rpc>";
		String response = post(request);
		if (!response.contains("<ok/>") && !response.contains("<ok />")) {
			throw new IOException("SEMP copy-message failed for " + rgmid + ": " + response);
		}
	}

	private String post(String xml) throws IOException {
		HttpURLConnection conn = (HttpURLConnection)new URL(baseUrl + "/SEMP").openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
		conn.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
		byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
		conn.setFixedLengthStreamingMode(bytes.length);
		try (OutputStream out = conn.getOutputStream()) {
			out.write(bytes);
		}
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		try (java.io.InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
			byte[] buffer = new byte[4096];
			int read;
			while (in != null && (read = in.read(buffer)) >= 0) {
				response.write(buffer, 0, read);
			}
		}
		String responseBody = new String(response.toByteArray(), StandardCharsets.UTF_8);
		if (conn.getResponseCode() >= 400) throw new IOException("SEMP HTTP " + conn.getResponseCode() + ": " + responseBody);
		return responseBody;
	}

	private String xml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
