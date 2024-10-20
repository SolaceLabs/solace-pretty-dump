/*
 * Copyright 2024 Solace Corporation. All rights reserved.
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

import org.fusesource.jansi.AnsiConsole;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessage;

import dev.solace.aaron.useful.BoundedLinkedList;

public class MessageObject {
	
    private static BoundedLinkedList.ComparableList<Integer> maxLengthUserPropsList = new BoundedLinkedList.ComparableList<>(200);
    private static BoundedLinkedList.ComparableList<Integer> minLengthPayloadLength = new BoundedLinkedList.ComparableList<>(50);  // max min-length of payload to display for -1 mode

	final BytesXMLMessage orig;
	final long lockedMsgCountNumber;
	final String lockedTimestamp;
	final String[] headerLines;
//	final String msgDestName;  // this would only be used in the 1-line version
	AaAnsi msgDestNameFormatted;
    String msgType;  // could change
    boolean hasPrintedMsgTypeYet = false;  // have we printed out the message type?

    PayloadSection binary;
    PayloadSection xml = null;
    PayloadSection userProps = null;
    PayloadSection userData = null;
    
    private final ConfigState config;
            
    public MessageObject(ConfigState config, BytesXMLMessage message, long msgCountNumber) {
    	this.config = config;
    	orig = message;
    	this.lockedMsgCountNumber = msgCountNumber;
    	this.lockedTimestamp = UsefulUtils.getCurrentTimestamp();
//    	this.msgCountNumber = config.;
//    	this.msgDestName = message.getDestination().getName();
    	headerLines = orig.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
    	msgType = orig.getClass().getSimpleName();  // will be "Impl" unless overridden later
    }

    /** this only gets called in non-one-line mode, otherwise we might have to do some trimming first */
    private void colorizeDestForRegularMode() {
    	String msgDestName = orig.getDestination().getName();
    	if (orig.getDestination() instanceof Queue) {
    		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a("Queue '").a(msgDestName).a('\'');
    	} else {  // a Topic
    		if (config.isOneLineMode()) {  // shouldn't be calling this yet!!?!?!!
        		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a(msgDestName);
        		throw new AssertionError("we shouldn't be here, this method is not for one-line mode");
    		} else {
        		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a("Topic '").colorizeTopic(msgDestName, config.getHighlightedTopicLevel()).fg(Elem.DESTINATION).a('\'');
    		}
    	}
    	msgDestNameFormatted.reset();
    }
    
    /** this is for the regex filtering, need to construct the whole payload */
    String buildFullStringObject() {
    	StringBuilder sb = new StringBuilder();
    	for (String line : headerLines) {
    		if (line.startsWith("User Data") && userData != null) {
    			sb.append(line).append('\n').append(userData).append('\n');
    		} else if (line.startsWith("User Property Map") && userProps != null) {
    			sb.append(line).append('\n').append(userProps).append('\n');
    		} else if (line.startsWith("Binary Attachment") && binary != null) {
    			sb.append(getMessageTypeLine().toRawString()).append('\n').append(line).append('\n').append(binary).append('\n');
    		} else if (line.startsWith("XML") && xml != null) {
    			sb.append(getMessageTypeLine().toRawString()).append('\n').append(line).append('\n').append(xml).append('\n');
    		} else {
    			sb.append(line).append('\n');
    		}
    	}
    	return sb.toString();
    }
    
    /** Only call this once all the filtering is done and time to space out the topic */
    void updateTopicSpacing() {
    	String msgDestName = orig.getDestination().getName();
    	AaAnsi ansiDest = AaAnsi.n().fg(Elem.DESTINATION);  // temporary
    	if (orig.getDestination() instanceof Queue) {
    		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
//        	config.updateTopicIndentValue(msgDestName.length());
    		ansiDest.a(msgDestName);
    	} else {  // a Topic
    		msgDestName = orig.getDestination().getName();
			msgDestName = config.updateTopicSpaceOutLevels(msgDestName);
//       		config.updateTopicIndentValue(msgDestName.length());
        	if (!config.isOneLineMode()) {
        		ansiDest.a("Topic '").colorizeTopic(msgDestName, config.getHighlightedTopicLevel()).fg(Elem.DESTINATION).a('\'');
        	} else {
        		ansiDest.colorizeTopic(msgDestName, config.getHighlightedTopicLevel());
        	}
    	}
//    	if (config.isOneLineMode() && config.includeTimestamp) {
//    		msgDestNameFormatted = AaAnsi.n().a(config.getTimestamp()).a(' ').aa(ansiDest).reset();
//    	} else {
    		msgDestNameFormatted = ansiDest.reset();
//    	}
    	config.updateTopicIndentValue(msgDestNameFormatted.length());
//    	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());        	
    }
    
    AaAnsi getMessageTypeLine() {
    	AaAnsi ansi = AaAnsi.n().a("Message Type:                           ");
		ansi.fg(Elem.PAYLOAD_TYPE);
//		if (includeEmpty) ansi.a("<EMPTY> ");
		ansi.a(msgType).reset();
		return ansi;
    }

    
    void printMsgTypeIfRequired(SystemOutHelper systemOut) {
		if (!hasPrintedMsgTypeYet) {
			systemOut.println(getMessageTypeLine());
			hasPrintedMsgTypeYet = true;
		}
    }
    
	private static final int DIVIDER_LENGTH = 60;  // same as SdkPerf JCSMP

	private static AaAnsi printMessageBoundary(String header) {
        String headPre = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - header.length()) / 2.0) , '^');
        String headPost = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - header.length()) / 2.0) , '^');
        return AaAnsi.n().fg(Elem.MSG_BREAK).a(headPre).a(header).a(headPost).reset();
	}
	
	AaAnsi printMessageStart() {
		if (config.includeTimestamp) {
			return printMessageBoundary(String.format(" %s- Start Message #%d ", lockedTimestamp, lockedMsgCountNumber));
		} else {
            return printMessageBoundary(String.format(" Start Message #%d ", lockedMsgCountNumber));
		}
	}
	
	AaAnsi printMessageEnd() {
		if (config.includeTimestamp) {
			return printMessageBoundary(String.format(" %s- End Message #%d ", lockedTimestamp, lockedMsgCountNumber));
		} else {
            return printMessageBoundary(String.format(" End Message #%d ", lockedMsgCountNumber));
		}
	}

	static AaAnsi printMessageStart(int num) {
		return printMessageBoundary(String.format(" %s- Start Message #%d ", UsefulUtils.getCurrentTimestamp(), num));
	}
	
	static AaAnsi printMessageEnd(int num) {
		return printMessageBoundary(String.format(" %s- End Message #%d ", UsefulUtils.getCurrentTimestamp(), num));
	}
	
	private void handlePayloadSection(PayloadSection ps, AaAnsi ansi) {
		boolean invalid = ps.type != null && (ps.type.contains("non") || ps.type.contains("INVALID"));
		if (config.getFormattingIndent() == 0 || config.noPayload) {  // so compressed and/or no payload
    		if (ps.type != null) {
    			ansi.a(',').a(' ');
    			if (invalid) ansi.invalid(ps.type);
    			else ansi.fg(Elem.PAYLOAD_TYPE).a(ps.type).reset();
    		}
    		if (!config.noPayload) ansi.a('\n').aa(ps.formatted);
    		else ansi.a('\n');
		} else {
			ansi.a('\n');
    		if (ps.type != null) {
    			if (invalid) ansi.invalid(UsefulUtils.capitalizeFirst(ps.type));
    			else ansi.fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(ps.type));//.reset();
        		if (!ps.type.contains("EMPTY")) ansi.a(':');
        		ansi.a('\n');
    		}
        	if (!config.noPayload && (ps.type == null || !ps.type.contains("EMPTY"))) {
        		ansi.aa(ps.formatted).a('\n');
        	}
		}
	}



    SystemOutHelper printMessage() {
        // now it's time to try printing it!
        SystemOutHelper systemOut = new SystemOutHelper();
        if (!config.isOneLineMode()) {
            systemOut.println(printMessageStart());
            for (String line : headerLines) {
            	if (line.isEmpty() || line.matches("\\s*")) continue;  // testing 
				if (line.startsWith("User Property Map:") && userProps != null) {
            		if (config.getFormattingIndent() == 0) {
            			if (config.isAutoTrimPayload()) {
//            				systemOut.println(line);  // hide the user props
                    		systemOut.println("User Property Map:                      " + userProps.numElements + " elements");
            			} else {
            				systemOut.println("User Property Map:                      " + userProps.formatted);
            			}
            		} else {
//                		systemOut.println(new AaAnsi().a(line));
                		systemOut.println("User Property Map:                      " + userProps.numElements + " elements");
                		systemOut.println(userProps.formatted);
            		}
            		if (config.getFormattingIndent() > 0 && !config.isNoPayload()) systemOut.println();
            	} else if (line.startsWith("User Data:") && userData != null) {
            		if (config.getFormattingIndent() == 0) {
            			if (config.isAutoTrimPayload()) {
	                		systemOut.println(line);
            			} else {
	                		systemOut.println("User Data:                              " + userData.formatted);
            			}
            		} else {
                		systemOut.println(AaAnsi.n().a(line).a(" bytes"));
                		systemOut.println(userData.formatted);
            		}
            		if (config.getFormattingIndent() > 0 && !config.isNoPayload()) systemOut.println();
            	} else if (line.startsWith("SDT Map:") || line.startsWith("SDT Stream:")) {
            		// skip (handled as part of the binary attachment)
            	} else if (line.startsWith("Binary Attachment:")) {
//            		assert binary != null;
                	printMsgTypeIfRequired(systemOut);
                	AaAnsi payloadText = AaAnsi.n().a(line).a(" bytes");
                	if (binary != null) handlePayloadSection(binary, payloadText);  // in no payload mode, with no filter, we won't even parse the payload
                	else if (config.getFormattingIndent() > 0) payloadText.a('\n');  // still add a carriage return for '00' indent mode
                	try {
                		systemOut.println(payloadText);
                	} catch (Exception e) {
                		System.err.println("Caught this!!" + e.toString());
                		e.printStackTrace();
                	}
            	} else if (line.startsWith("XML:")) {
//            		assert xml != null;
                	printMsgTypeIfRequired(systemOut);
                	AaAnsi payloadText = AaAnsi.n().fg(Elem.WARN).a("XML Payload section:           ").reset();
                	payloadText.a("         ").a(line.substring(40)).a(" bytes").reset();
                	if (xml != null) handlePayloadSection(xml, payloadText);
                	systemOut.println(payloadText);
            	} else if (line.startsWith("Destination:           ")) {
            		systemOut.print("Destination:                            ");
            		colorizeDestForRegularMode();
            		systemOut.println(msgDestNameFormatted);  // just print out since it's already formatted
            	} else if (line.startsWith("Message Id:") && orig.getDeliveryMode() == DeliveryMode.DIRECT) {
            		// skip it, hide the auto-generated message ID on Direct messages
            	} else {  // everything else
//            		System.out.println(new AaAnsi().a(line));
            		systemOut.println(UsefulUtils.guessIfMapLookingThing(line));
            	}
            }
            if (!orig.hasContent() && !orig.hasAttachment()) {
            	printMsgTypeIfRequired(systemOut);
//            	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
            	systemOut.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a("<NO PAYLOAD>").reset().toString());
            } else if (orig.hasAttachment() && orig.getAttachmentContentLength() == 0) {
            	printMsgTypeIfRequired(systemOut);
//            	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
            	systemOut.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a("<EMPTY PAYLOAD>").reset().toString());
            	
            }
			if (config.getFormattingIndent() > 0) systemOut.println(printMessageEnd());
    	} else {  // one-line mode!
            updateTopicSpacing();  // now that we've determined if we're gonna filter this message, do the topic stuff
    		String payloadSizeString;
    		if (binary != null && xml != null) {
    			// that's not great for one-line printing!  but probably pretty rare!!!!
    			systemOut.println(AaAnsi.n().warn("Message contains both binary and XML payloads! Dunno which one to show for one-line mode."));
    			systemOut.println(orig.dump().trim());  // raw JCSMP full dump
    		} else if (config.isNoPayload()) {  // "-0" mode
    	    	if (config.includeTimestamp) {
    	    		systemOut.print(lockedTimestamp);
    	    	}
				systemOut.println(msgDestNameFormatted);
    		} else {  // one payload section defined, or empty
				AaAnsi payload = null;
				if (binary != null && (binary.type == null || (binary.type != null && !binary.type.startsWith("<EMPTY")))) {
					payload = binary.formatted;
					payloadSizeString = binary.getSizeString();  // like: (bytes=2345)
				}
				else if (xml != null && (xml.type == null || (xml.type != null && !xml.type.startsWith("<EMPTY")))) {
					payload = xml.formatted;
					payloadSizeString = xml.getSizeString();
				}
				else {
//					payload = AaAnsi.n().faintOn().a("<EMPTY> ").a(msgType).reset();  // hopefully an EMPTY message
					payload = AaAnsi.n().faintOn().a("<EMPTY>").reset();  // hopefully an EMPTY message
					payloadSizeString = "";  // empty payload is already the payload
				}
				minLengthPayloadLength.add(payloadSizeString.length());
				int currentScreenWidth = AnsiConsole.getTerminalWidth();
				if (currentScreenWidth == 0) currentScreenWidth = 80;  // force
				// we need all that in case we're in -1 mode and we need to trim
				
				if (userProps != null) {
					if (userProps.numElements == 0) maxLengthUserPropsList.add(0);
					if (userProps.numElements < 10) maxLengthUserPropsList.add(1);
					else if (userProps.numElements < 100) maxLengthUserPropsList.add(2);
					else if (userProps.numElements < 1000) maxLengthUserPropsList.add(3);
					else maxLengthUserPropsList.add(4);  // assume 9999 is the most!
				}
				else  maxLengthUserPropsList.add(0);
				if (config.getCurrentIndent() == 2) {  // two-line mode
//					AaAnsi props = AaAnsi.n();//.fg(Elem.PAYLOAD_TYPE);
					AaAnsi minProps = AaAnsi.n();
					if (userProps != null) {
						minProps.a('(').fg(Elem.PAYLOAD_TYPE).a(userProps.numElements).reset().a(" prop" + (userProps.numElements == 1 ? ")" : "s)")).reset();
					} else {
						minProps.faintOn().a('-').reset();
//						props = minProps;
					}
					if (config.includeTimestamp) {
						systemOut.print(AaAnsi.n().fg(Elem.MSG_BREAK).a(lockedTimestamp));
					}
					int spaceToAdd = currentScreenWidth - msgDestNameFormatted.length() - (maxLengthUserPropsList.max() > 0 ? (userProps != null ? userProps.formatted.length() : 1)+2 : 0) - config.getTimestampIndentIfEnabled();  // -1 for extra space between topic and user props
					if (spaceToAdd < 0) {  // need to trim!  something.  Trim User Props first, then maybe trim again?
		    	    	// what if we just include the minProps string (e.g. "3 UserProps"
		    	    	int spaceToAdd2 = currentScreenWidth - msgDestNameFormatted.length() - (maxLengthUserPropsList.max() > 0 ? minProps.length()+2 : 0) - config.getTimestampIndentIfEnabled();
		    	    	if (spaceToAdd2 < 0) {  // ok, so definitely need to trim the topic
//		    	    		System.out.println("topic trim " + maxLengthUserPropsList.max());
		    	    		systemOut.print(msgDestNameFormatted.trim(msgDestNameFormatted.length() + spaceToAdd2));
		    	    		if (maxLengthUserPropsList.max() > 0) systemOut.print("  ").print(minProps);
		    	    	} else {  // can leave topic alone, but need to trim props
//		    	    		System.out.printf("props trim, maxUP=%d, sta=%d, sta2=%d%n", maxLengthUserPropsList.max(), spaceToAdd, spaceToAdd2);
		    	    		systemOut.print(msgDestNameFormatted);
		    	    		if (maxLengthUserPropsList.max() > 0) {
		    	    			systemOut.print("  ");
			    	    		if (spaceToAdd2 == 0 || userProps == null) systemOut.print(minProps);
			    	    		// shouldn't be able to make it into this block if userProps == null
			    	    		else systemOut.print(AaAnsi.n().fg(Elem.MSG_BREAK).a(userProps.formatted.toRawString().substring(0, spaceToAdd2-1)).a('…').aa(minProps));
		    	    		}
		    	    	}
					} else {  // no trimming needed!
//	    	    		System.out.println("no trim");
	    				systemOut.print(msgDestNameFormatted);
	    				if (maxLengthUserPropsList.max() > 0) {
		    				systemOut.print(UsefulUtils.pad(spaceToAdd + 2, ' '));
		    				if (userProps != null) systemOut.print(AaAnsi.n().fg(Elem.MSG_BREAK).a(userProps.formatted.toRawString()));
		    				else  systemOut.print(minProps);  // at least some messages have user props
	    				}
					}
					systemOut.println();
					if (!payloadSizeString.isEmpty()) {  // don't add the extra line just for empty payload
						systemOut.print("  ");
	    				if (config.isAutoTrimPayload()) {
	    					if (payload.length() > currentScreenWidth - config.getCurrentIndent()) {
	        					systemOut.print(payload.trim(currentScreenWidth - config.getCurrentIndent() - payloadSizeString.length()));
	        					systemOut.println(payloadSizeString);
	    					} else {  // enough space
	    						systemOut.println(payload);
	    					}
	    				} else {
	    					systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
	    					if (config.getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
	    				}
					}
				} else {  // proper one-line mode!
					final int PADDING_CORRECTION = 1;
					// what is the smallest we can trim the payload portion to?
    				int minPayloadLength = minLengthPayloadLength.max();
    				// if we haven't seen any user props in a long time, don't need to add any space for them; otherwise add some space
    				if (maxLengthUserPropsList.max() > 0) minPayloadLength += maxLengthUserPropsList.max() + 2 - PADDING_CORRECTION;  // user prop spacing + 1 + 1 for blackspace
    				// need some more padding if ts is enabled
    				minPayloadLength += config.getTimestampIndentIfEnabled();
    				int effectiveIndent = config.getCurrentIndent();
//    				System.out.printf("minPayloadLength=%d, maxLengthUserProps=%d%n", minLengthPayloadLength.getMax(), maxLengthUserPropsList.getMax());
    				if (config.isAutoResizeIndent()) {  // mode -1
    					if (config.isAutoTrimPayload() && config.getCurrentIndent() > currentScreenWidth - minPayloadLength - 1) {  // need to trim the topic!   minPL = 11
    						effectiveIndent = currentScreenWidth - minPayloadLength - 1;
    						if (config.includeTimestamp) systemOut.print(lockedTimestamp);
    						systemOut.print(msgDestNameFormatted.trim(effectiveIndent-1));
    						int spaceToAdd = effectiveIndent - Math.min(msgDestNameFormatted.length(), effectiveIndent-1);
    						systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
    					} else {
    						if (config.includeTimestamp) systemOut.print(lockedTimestamp);
    						systemOut.print(msgDestNameFormatted);
    						int spaceToAdd = config.getCurrentIndent() - msgDestNameFormatted.length();
    						systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
    					}
    				} else {
						if (config.includeTimestamp) systemOut.print(lockedTimestamp);
    					systemOut.print(msgDestNameFormatted.trim(config.getCurrentIndent()-1));
    					int spaceToAdd = config.getCurrentIndent() - Math.min(msgDestNameFormatted.length(), config.getCurrentIndent()-1);
    					systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
    				}
    				
					AaAnsi props = AaAnsi.n().fg(Elem.PAYLOAD_TYPE);
//					AaAnsi props = AaAnsi.n().insertBlackSpace().a(' ').fg(Elem.BYTES);
					if (maxLengthUserPropsList.max() > 0) {
//						props.blackOn();
						if (userProps == null || userProps.numElements == 0) props.faintOn().a('-');
						else props.a(userProps.numElements);
//						props.blackOff();
						props.a(' ');
					}
					systemOut.print(props.reset());
					if (config.isAutoTrimPayload()) {
						int trimLength = currentScreenWidth - effectiveIndent - PADDING_CORRECTION - props.length() - config.getTimestampIndentIfEnabled();
						if (payload.length() <= trimLength) {
							systemOut.println(payload.reset());
						} else {
//							System.out.printf("w=%d,in=%d,efIn=%d,MXpay=%d,len=%d,trim=%d%n", currentScreenWidth, getCurrentIndent(), effectiveIndent, minLengthPayloadLength.getMax(), payloadSizeString.length(), trimLength);
							systemOut.print(payload.trim(trimLength - payloadSizeString.length())).println(payloadSizeString);
						}
					}
					else {
						systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
//					System.out.printf("charCount=%d, calcLength=%d, width=%d, size=%d%n", binary.formatted.getCharCount(), AaAnsi.length(binary.formatted.toString()),currentScreenWidth,Math.abs(INDENT)+binary.formatted.getCharCount());
						if (config.getTimestampIndentIfEnabled() + config.getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
					}
				}
    		}
    	}
		return systemOut;
    }
}
