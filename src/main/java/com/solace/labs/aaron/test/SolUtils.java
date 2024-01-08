/*
 * Copyright 2023 Solace Corporation. All rights reserved.
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

package com.solace.labs.aaron.test;

import java.lang.reflect.Field;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class SolUtils {

	
	
	
	@SuppressWarnings("deprecation")
	public static void getCapabilities(JCSMPSession session) {
		
		for (CapabilityType ct : CapabilityType.values()) {
			try {
				Object o = session.getCapability(ct);
				System.out.println(ct.toString() + ": " + o.toString());
			} catch (JCSMPException e) {
				e.printStackTrace();
			}
		}
		// use reflection to get all properties, dump to console, and paste in here...
		Field[] declaredFields = JCSMPProperties.class.getDeclaredFields();
		for (Field field : declaredFields) {
		    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
		    		java.lang.reflect.Modifier.isFinal(field.getModifiers()) &&
		    		java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
//		        System.out.printf("System.out.println(\"%s: \" + session.getProperty(JCSMPProperties.%s));%n", field.getName(), field.getName());
//		        System.out.println("SECURE_PROPS: " + session.getProperty(JCSMPProperties.SECURE_PROPS));
		    }
		}

		System.out.println("SUPPORTED_MESSAGE_ACK_AUTO: " + session.getProperty(JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO));
		System.out.println("SUPPORTED_MESSAGE_ACK_CLIENT: " + session.getProperty(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT));
		System.out.println("SUPPORTED_MESSAGE_ACK_CLIENT_WINDOWED: " + session.getProperty(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT_WINDOWED));
		System.out.println("SUPPORTED_ACK_EVENT_MODE_PER_MSG: " + session.getProperty(JCSMPProperties.SUPPORTED_ACK_EVENT_MODE_PER_MSG));
		System.out.println("SUPPORTED_ACK_EVENT_MODE_WINDOWED: " + session.getProperty(JCSMPProperties.SUPPORTED_ACK_EVENT_MODE_WINDOWED));
		System.out.println("CLIENT_MODE: " + session.getProperty(JCSMPProperties.CLIENT_MODE));
		System.out.println("AUTHENTICATION_SCHEME_BASIC: " + session.getProperty(JCSMPProperties.AUTHENTICATION_SCHEME_BASIC));
		System.out.println("AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE: " + session.getProperty(JCSMPProperties.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE));
		System.out.println("AUTHENTICATION_SCHEME_GSS_KRB: " + session.getProperty(JCSMPProperties.AUTHENTICATION_SCHEME_GSS_KRB));
		System.out.println("AUTHENTICATION_SCHEME_OAUTH2: " + session.getProperty(JCSMPProperties.AUTHENTICATION_SCHEME_OAUTH2));
		System.out.println("OAUTH2_ACCESS_TOKEN: " + session.getProperty(JCSMPProperties.OAUTH2_ACCESS_TOKEN));
		System.out.println("OAUTH2_ISSUER_IDENTIFIER: " + session.getProperty(JCSMPProperties.OAUTH2_ISSUER_IDENTIFIER));
		System.out.println("OIDC_ID_TOKEN: " + session.getProperty(JCSMPProperties.OIDC_ID_TOKEN));
		System.out.println("CONNECTION_TYPE_BASIC: " + session.getProperty(JCSMPProperties.CONNECTION_TYPE_BASIC));
		System.out.println("CONNECTION_TYPE_XA: " + session.getProperty(JCSMPProperties.CONNECTION_TYPE_XA));
		System.out.println("TRANSPORT_PROTOCOL_PLAIN_TEXT: " + session.getProperty(JCSMPProperties.TRANSPORT_PROTOCOL_PLAIN_TEXT));
		System.out.println("HOST: " + session.getProperty(JCSMPProperties.HOST));
		System.out.println("SESSION_NAME: " + session.getProperty(JCSMPProperties.SESSION_NAME));
		System.out.println("LOCALHOST: " + session.getProperty(JCSMPProperties.LOCALHOST));
		System.out.println("USERNAME: " + session.getProperty(JCSMPProperties.USERNAME));
		System.out.println("PASSWORD: " + session.getProperty(JCSMPProperties.PASSWORD));
		System.out.println("SECURE_PROPS: " + session.getProperty(JCSMPProperties.SECURE_PROPS));
		System.out.println("CLIENT_CHANNEL_PROPERTIES: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES));
		System.out.println("MESSAGE_ACK_MODE: " + session.getProperty(JCSMPProperties.MESSAGE_ACK_MODE));
		System.out.println("APPLICATION_DESCRIPTION: " + session.getProperty(JCSMPProperties.APPLICATION_DESCRIPTION));
		System.out.println("CLIENT_NAME: " + session.getProperty(JCSMPProperties.CLIENT_NAME));
		System.out.println("P2PINBOX_IN_USE: " + session.getProperty(JCSMPProperties.P2PINBOX_IN_USE));
		System.out.println("GENERATE_SENDER_ID: " + session.getProperty(JCSMPProperties.GENERATE_SENDER_ID));
		System.out.println("GENERATE_SEND_TIMESTAMPS: " + session.getProperty(JCSMPProperties.GENERATE_SEND_TIMESTAMPS));
		System.out.println("GENERATE_RCV_TIMESTAMPS: " + session.getProperty(JCSMPProperties.GENERATE_RCV_TIMESTAMPS));
		System.out.println("GENERATE_SEQUENCE_NUMBERS: " + session.getProperty(JCSMPProperties.GENERATE_SEQUENCE_NUMBERS));
		System.out.println("CALCULATE_MESSAGE_EXPIRATION: " + session.getProperty(JCSMPProperties.CALCULATE_MESSAGE_EXPIRATION));
		System.out.println("VPN_NAME: " + session.getProperty(JCSMPProperties.VPN_NAME));
		System.out.println("VPN_NAME_IN_USE: " + session.getProperty(JCSMPProperties.VPN_NAME_IN_USE));
		System.out.println("REAPPLY_SUBSCRIPTIONS: " + session.getProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS));
		System.out.println("PUB_MULTI_THREAD: " + session.getProperty(JCSMPProperties.PUB_MULTI_THREAD));
		System.out.println("PUB_USE_INTERMEDIATE_DIRECT_BUF: " + session.getProperty(JCSMPProperties.PUB_USE_INTERMEDIATE_DIRECT_BUF));
		System.out.println("MESSAGE_CALLBACK_ON_REACTOR: " + session.getProperty(JCSMPProperties.MESSAGE_CALLBACK_ON_REACTOR));
		System.out.println("IGNORE_DUPLICATE_SUBSCRIPTION_ERROR: " + session.getProperty(JCSMPProperties.IGNORE_DUPLICATE_SUBSCRIPTION_ERROR));
		System.out.println("IGNORE_SUBSCRIPTION_NOT_FOUND_ERROR: " + session.getProperty(JCSMPProperties.IGNORE_SUBSCRIPTION_NOT_FOUND_ERROR));
		System.out.println("VIRTUAL_ROUTER_NAME: " + session.getProperty(JCSMPProperties.VIRTUAL_ROUTER_NAME));
		System.out.println("NO_LOCAL: " + session.getProperty(JCSMPProperties.NO_LOCAL));
		System.out.println("ACK_EVENT_MODE: " + session.getProperty(JCSMPProperties.ACK_EVENT_MODE));
		System.out.println("SSL_EXCLUDED_PROTOCOLS: " + session.getProperty(JCSMPProperties.SSL_EXCLUDED_PROTOCOLS));
		System.out.println("SSL_VALIDATE_CERTIFICATE: " + session.getProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE));
		System.out.println("SSL_VALIDATE_CERTIFICATE_DATE: " + session.getProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE));
		System.out.println("SSL_VALIDATE_CERTIFICATE_HOST: " + session.getProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_HOST));
		System.out.println("SSL_CIPHER_SUITES: " + session.getProperty(JCSMPProperties.SSL_CIPHER_SUITES));
		System.out.println("SSL_TRUST_STORE: " + session.getProperty(JCSMPProperties.SSL_TRUST_STORE));
		System.out.println("SSL_TRUST_STORE_PASSWORD: " + session.getProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD));
		System.out.println("SSL_TRUST_STORE_FORMAT: " + session.getProperty(JCSMPProperties.SSL_TRUST_STORE_FORMAT));
		System.out.println("SSL_TRUSTED_COMMON_NAME_LIST: " + session.getProperty(JCSMPProperties.SSL_TRUSTED_COMMON_NAME_LIST));
		System.out.println("SSL_KEY_STORE: " + session.getProperty(JCSMPProperties.SSL_KEY_STORE));
		System.out.println("SSL_KEY_STORE_PASSWORD: " + session.getProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD));
		System.out.println("SSL_KEY_STORE_FORMAT: " + session.getProperty(JCSMPProperties.SSL_KEY_STORE_FORMAT));
		System.out.println("SSL_KEY_STORE_NORMALIZED_FORMAT: " + session.getProperty(JCSMPProperties.SSL_KEY_STORE_NORMALIZED_FORMAT));
		System.out.println("SSL_PRIVATE_KEY_ALIAS: " + session.getProperty(JCSMPProperties.SSL_PRIVATE_KEY_ALIAS));
		System.out.println("SSL_PRIVATE_KEY_PASSWORD: " + session.getProperty(JCSMPProperties.SSL_PRIVATE_KEY_PASSWORD));
		System.out.println("SSL_IN_MEMORY_KEY_STORE: " + session.getProperty(JCSMPProperties.SSL_IN_MEMORY_KEY_STORE));
		System.out.println("SSL_IN_MEMORY_TRUST_STORE: " + session.getProperty(JCSMPProperties.SSL_IN_MEMORY_TRUST_STORE));
		System.out.println("KRB_SERVICE_NAME: " + session.getProperty(JCSMPProperties.KRB_SERVICE_NAME));
		System.out.println("SUBSCRIBER_LOCAL_PRIORITY: " + session.getProperty(JCSMPProperties.SUBSCRIBER_LOCAL_PRIORITY));
		System.out.println("SUBSCRIBER_NETWORK_PRIORITY: " + session.getProperty(JCSMPProperties.SUBSCRIBER_NETWORK_PRIORITY));
		System.out.println("SSL_PROTOCOL: " + session.getProperty(JCSMPProperties.SSL_PROTOCOL));
		System.out.println("AUTHENTICATION_SCHEME: " + session.getProperty(JCSMPProperties.AUTHENTICATION_SCHEME));
		System.out.println("SSL_CONNECTION_DOWNGRADE_TO: " + session.getProperty(JCSMPProperties.SSL_CONNECTION_DOWNGRADE_TO));
		System.out.println("GD_RECONNECT_FAIL_ACTION: " + session.getProperty(JCSMPProperties.GD_RECONNECT_FAIL_ACTION));
		System.out.println("GD_RECONNECT_FAIL_ACTION_AUTO_RETRY: " + session.getProperty(JCSMPProperties.GD_RECONNECT_FAIL_ACTION_AUTO_RETRY));
		System.out.println("GD_RECONNECT_FAIL_ACTION_DISCONNECT: " + session.getProperty(JCSMPProperties.GD_RECONNECT_FAIL_ACTION_DISCONNECT));
		System.out.println("KRB_MUTUAL_AUTHENTICATION: " + session.getProperty(JCSMPProperties.KRB_MUTUAL_AUTHENTICATION));
		System.out.println("P2PTOPICDESCRIPTION: " + session.getProperty(JCSMPProperties.P2PTOPICDESCRIPTION));
		System.out.println("TOPIC_DISPATCH: " + session.getProperty(JCSMPProperties.TOPIC_DISPATCH));
		System.out.println("TOPIC_DISPATCH_OPTIMIZE_DIRECT: " + session.getProperty(JCSMPProperties.TOPIC_DISPATCH_OPTIMIZE_DIRECT));
		System.out.println("CLIENT_INFO_PROVIDER: " + session.getProperty(JCSMPProperties.CLIENT_INFO_PROVIDER));
		System.out.println("AD_PUB_ROUTER_WINDOWED_ACK: " + session.getProperty(JCSMPProperties.AD_PUB_ROUTER_WINDOWED_ACK));
		System.out.println("CONNECTION_TYPE: " + session.getProperty(JCSMPProperties.CONNECTION_TYPE));
		System.out.println("CONTROL_CHANNEL_PROPERTIES: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_SMF_PORT: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_SMF_PORT));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_CONNECT_RETRIES: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_CONNECT_RETRIES));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_RECONNECT_RETRIES: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_RECONNECT_RETRIES));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_STACK: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_STACK));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_TCP_NO_DELAY: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_TCP_NO_DELAY));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_SEND_BUFFER: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_SEND_BUFFER));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_RECEIVE_BUFFER: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_RECEIVE_BUFFER));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_COMPRESSION_LEVEL: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_COMPRESSION_LEVEL));
		System.out.println("CLIENT_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST: " + session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_SMF_PORT: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_SMF_PORT));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_CONNECT_RETRIES: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_CONNECT_RETRIES));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_RECONNECT_RETRIES: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_RECONNECT_RETRIES));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_STACK: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_STACK));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_TCP_NO_DELAY: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_TCP_NO_DELAY));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_SEND_BUFFER: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_SEND_BUFFER));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_RECEIVE_BUFFER: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_RECEIVE_BUFFER));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_COMPRESSION_LEVEL: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_COMPRESSION_LEVEL));
		System.out.println("CONTROL_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST: " + session.getProperty(JCSMPProperties.CONTROL_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_SMF_PORT: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_SMF_PORT));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRIES: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRIES));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_STACK: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_STACK));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_TCP_NO_DELAY: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_TCP_NO_DELAY));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_SEND_BUFFER: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_SEND_BUFFER));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_RECEIVE_BUFFER: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_RECEIVE_BUFFER));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_COMPRESSION_LEVEL: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_COMPRESSION_LEVEL));
		System.out.println("PUBLISHER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST: " + session.getProperty(JCSMPProperties.PUBLISHER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_CONNECT_TIMEOUT_IN_MILLIS));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_SMF_PORT: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_SMF_PORT));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_INTERVAL_IN_MILLIS));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_KEEP_ALIVE_LIMIT));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_READ_TIMEOUT_IN_MILLIS));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRIES: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRIES));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_RECONNECT_RETRY_WAIT_IN_MILLIS));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_STACK: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_STACK));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_TCP_NO_DELAY: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_TCP_NO_DELAY));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_SEND_BUFFER: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_SEND_BUFFER));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_RECEIVE_BUFFER: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_RECEIVE_BUFFER));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_COMPRESSION_LEVEL: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_COMPRESSION_LEVEL));
		System.out.println("SUBSCRIBER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST: " + session.getProperty(JCSMPProperties.SUBSCRIBER_DATA_CHANNEL_PROPERTIES_CONNECT_RETRIES_PER_HOST));
		System.out.println("SUB_ACK_WINDOW_SIZE: " + session.getProperty(JCSMPProperties.SUB_ACK_WINDOW_SIZE));
		System.out.println("PUB_ACK_WINDOW_SIZE: " + session.getProperty(JCSMPProperties.PUB_ACK_WINDOW_SIZE));
		System.out.println("SUB_ACK_TIME: " + session.getProperty(JCSMPProperties.SUB_ACK_TIME));
		System.out.println("PUB_ACK_TIME: " + session.getProperty(JCSMPProperties.PUB_ACK_TIME));
		System.out.println("SUB_ACK_WINDOW_THRESHOLD: " + session.getProperty(JCSMPProperties.SUB_ACK_WINDOW_THRESHOLD));
		System.out.println("MAX_RESENDS: " + session.getProperty(JCSMPProperties.MAX_RESENDS));
		System.out.println("LARGE_MESSAGING: " + session.getProperty(JCSMPProperties.LARGE_MESSAGING));
		System.out.println("LARGE_MESSAGING_SEGMENT_SIZE: " + session.getProperty(JCSMPProperties.LARGE_MESSAGING_SEGMENT_SIZE));
		System.out.println("LARGE_MESSAGING_CONSUME_TIMEOUT: " + session.getProperty(JCSMPProperties.LARGE_MESSAGING_CONSUME_TIMEOUT));
		System.out.println("LARGE_MESSAGING_MAX_MSG_SIZE: " + session.getProperty(JCSMPProperties.LARGE_MESSAGING_MAX_MSG_SIZE));
		System.out.println("MESSAGE_CONSUMER_INTERCEPTOR_CLASS_NAME: " + session.getProperty(JCSMPProperties.MESSAGE_CONSUMER_INTERCEPTOR_CLASS_NAME));
		System.out.println("MESSAGE_CONSUMER_INTERCEPTOR_CONSTRUCTOR_ARGUMENT: " + session.getProperty(JCSMPProperties.MESSAGE_CONSUMER_INTERCEPTOR_CONSTRUCTOR_ARGUMENT));
		System.out.println("MAX_AD_FLOWCTRL_RETRIES: " + session.getProperty(JCSMPProperties.MAX_AD_FLOWCTRL_RETRIES));
		System.out.println("DEFAULT_SESSION_NAME: " + session.getProperty(JCSMPProperties.DEFAULT_SESSION_NAME));
		System.out.println("KRB_SERVICE_DEFAULT: " + session.getProperty(JCSMPProperties.KRB_SERVICE_DEFAULT));
		System.out.println("JAAS_CONFIG_FILE_RELOAD_ENABLED: " + session.getProperty(JCSMPProperties.JAAS_CONFIG_FILE_RELOAD_ENABLED));
		System.out.println("JAAS_LOGIN_CONTEXT: " + session.getProperty(JCSMPProperties.JAAS_LOGIN_CONTEXT));
	}
	
	
	private static final JCSMPFactory f = JCSMPFactory.onlyInstance();

	/** Might want to use a Builder pattern here?   */
	public static BytesXMLMessage copy(BytesXMLMessage msg, boolean includeTtl, boolean includeExpiration, boolean includeElidingEligible) {
		BytesXMLMessage dstMsg;
		// need to create specific message type... can't just use generic message and set payload as bytes
		if (msg instanceof TextMessage) {
			System.out.println("TextMessage");
			dstMsg = f.createMessage(TextMessage.class);
		}
		else if (msg instanceof MapMessage) {
			System.out.println("MapMessage");
			dstMsg = f.createMessage(MapMessage.class);
		}
		else if (msg instanceof StreamMessage) {
			System.out.println("StreamMessage");
			dstMsg = f.createMessage(StreamMessage.class);
		}
		else {  // default
			System.out.println("BytesMessage");
			dstMsg = f.createMessage(BytesMessage.class);
		}
		
        // metadata / headers
        if (msg.getApplicationMessageId() != null)			dstMsg.setApplicationMessageId(msg.getApplicationMessageId());
        if (msg.getApplicationMessageType() != null)		dstMsg.setApplicationMessageType(msg.getApplicationMessageType());
        // no set on this...
//        if (msg.getConsumerIdList() != null && !msg.getConsumerIdList().isEmpty()) {
//            JsonArrayBuilder jab = Json.createArrayBuilder();
//            for (Long l : msg.getConsumerIdList()) {
//                jab.add(l);
//            }
//            job.add("consumerIdList", jab.build());
//        }
        // this will get set automatically by populating 
//        if (msg.getContentLength() > 0) job.add("contentLength", msg.getContentLength());
        if (msg.getCorrelationId() != null)					dstMsg.setCorrelationId(msg.getCorrelationId());
        dstMsg.setCos(msg.getCos());
//        try {
//            job.add("deliveryCount", msg.getDeliveryCount());
//        } catch (UnsupportedOperationException e) {
//            // ignore
//        }
        dstMsg.setDeliveryMode(msg.getDeliveryMode());
        if (msg.isDMQEligible() && includeElidingEligible)	dstMsg.setDMQEligible(true);
        if (msg.getExpiration() > 0 && includeExpiration)	dstMsg.setExpiration(msg.getExpiration());  // obviously this might not be great if cloning messages elsewhere later
        if (msg.getHTTPContentEncoding() != null)			dstMsg.setHTTPContentEncoding(msg.getHTTPContentEncoding());
        if (msg.getHTTPContentType() != null)				dstMsg.setHTTPContentType(msg.getHTTPContentType());
        // this is automatically set by the broker, as the message spool ID
//        if (msg.getMessageId() != null) job.add("mesageId", msg.getMessageId());  // deprecated, but still dump it out
        dstMsg.setPriority(msg.getPriority());
        // can't set this...
//        if (msg.getRedelivered()) job.add("redelivered", msg.getRedelivered());
        // can't set the RGMID..!
//        if (msg.getReplicationGroupMessageId() != null) job.add("replicationGroupMessageId", msg.getReplicationGroupMessageId().toString());
        if (msg.isReplyMessage())							dstMsg.setAsReplyMessage(true);
        if (msg.getReplyTo() != null)						dstMsg.setReplyTo(msg.getReplyTo());
        if (msg.getSenderId() != null)						dstMsg.setSenderId(msg.getSenderId());
        if (msg.getSenderTimestamp() != null)				dstMsg.setSenderTimestamp(msg.getSenderTimestamp());
        if (msg.getSequenceNumber() != null)				dstMsg.setSequenceNumber(msg.getSequenceNumber());
        if (msg.getTimeToLive() > 0 && includeTtl)			dstMsg.setTimeToLive(msg.getTimeToLive());

        
        
        // properties
        if (msg.getProperties() != null)					dstMsg.setProperties(msg.getProperties());

        // binary payload portion
        if (msg.hasAttachment()) {
        	// need to use specific message type
        	if (msg instanceof TextMessage) ((TextMessage)dstMsg).setText(((TextMessage) msg).getText());
        	else if (msg instanceof MapMessage) ((MapMessage)dstMsg).setMap(((MapMessage) msg).getMap());
        	else if (msg instanceof StreamMessage) ((StreamMessage)dstMsg).setStream(((StreamMessage) msg).getStream());
        	else ((BytesMessage)dstMsg).setData(((BytesMessage) msg).getData());
        }
        	
        // XML payload
        if (msg.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
        	// one way
//        	byte[] attachment = new byte[msg.getContentLength()];
//        	msg.readContentBytes(attachment);
//        	dstMsg.writeAttachment(attachment);
        	// another way
        	dstMsg.writeBytes(msg.getBytes());  // this is apparently the XML portion
        	// switch from XML!
//        	dstMsg.setData(msg.getBytes());  // switch from XML portion to binary portion
        }
        
		return dstMsg;
	}


	
	static void sendCopiedMessage(BytesXMLMessage msg) throws JCSMPException {
		BytesXMLMessage dstMsg = copy(msg, false, false, false);
		
        // live testing...
        if ("republish".equals("republish")) {
	        JCSMPProperties props = new JCSMPProperties();
	        props.setProperty(JCSMPProperties.HOST, "localhost");
	        props.setProperty(JCSMPProperties.VPN_NAME, "default");
	        props.setProperty(JCSMPProperties.USERNAME, "aaron");
	        JCSMPSession session = f.createSession(props);
	        session.connect();
	        XMLMessageProducer producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
				
				@Override
				public void responseReceivedEx(Object arg0) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void handleErrorEx(Object arg0, JCSMPException arg1, long arg2) {
					// TODO Auto-generated method stub
				}
			});
	        // can't republish received message!!!!
	        // 09:29:15.639 [Context_1_ConsumerDispatcher] WARN  com.solac.jcsmp.impl.flow.FlowHandleImpl - Exception occurred in async delivery for flow 0
	        // java.lang.IllegalAccessError: May not modify read-only message.
	        //         at com.solacesystems.jcsmp.impl.JCSMPXMLMessage.checkReadOnlyBeforeModify(JCSMPXMLMessage.java:1330) ~[sol-jcsmp-10.21.0.jar:?]
//	        producer.send(msg, f.createTopic("copy/orig"));
	        producer.send(dstMsg, f.createTopic("copy/copy"));
	        session.closeSession();
        }        
		
		
	}
	
	
	/*	
	private static SDTMap shallowCopyMap(SDTMap map) {
		SDTMap dstMap = f.createMap();
        try {
            for (String key : map.keySet()) {
                Object o = map.get(key);
                if (o instanceof String) {
                	dstMap.put
                    job.add(key, (String)o);
                } else if (o instanceof SDTMap) {
                    job.add(key, sdtMapToJson((SDTMap)o));
                } else if (o instanceof SDTStream) {
                    job.add(key, sdtStreamToJson((SDTStream)o));
                } else if (o instanceof Double) {
                    job.add(key, (Double)o);
                } else if (o instanceof Float) {
                    job.add(key, (Float)o);
                } else if (o instanceof Integer) {
                    job.add(key, (Integer)o);
                } else if (o instanceof Long) {
                    job.add(key, (Long)o);
                } else if (o instanceof Boolean) {
                    job.add(key, (Boolean)o);
                } else if (o instanceof Short) {
                    job.add(key, (Short)o);
                } else if (o instanceof Byte) {
                    job.add(key, (Byte)o);
                } else if (o instanceof ByteArray) {
                    job.add(key, new String(Base64.getEncoder().encode(((ByteArray)o).asBytes())));
                } else if (o instanceof Character) {
                    job.add(key, (Character)o);
                } else if (o instanceof Destination) {
                    job.add(key, ((Destination)o).getName());
                } else {
                    System.err.println("Unhandled type "+o.getClass().getName()+"!!  "+key+", "+o);
                }
            }
        } catch (SDTException e) {
            e.printStackTrace();
        }
		return dstMap;
	}
	
	private static SDTStream deepCopyStream(SDTStream srcStream) {
		SDTStream dstStream = f.createStream();
		
		return dstStream;
	}
*/	

	
	private SolUtils() {
		throw new AssertionError("Utility class, don't instantiate");
	}

	
}


