package com.solace.labs.aaron;

import org.fusesource.jansi.AnsiConsole;

import com.solace.labs.aaron.PayloadHelper.PayloadSection;
import com.solace.labs.aaron.utils.BoundedLinkedList;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessage;

public class MessageHelper {
	
    private static BoundedLinkedList.Comparable<Integer> maxLengthUserPropsList = new BoundedLinkedList.Comparable<>(200);
    private static BoundedLinkedList.Comparable<Integer> minLengthPayloadLength = new BoundedLinkedList.Comparable<>(50);  // max min-length of payload to display for -1 mode

	final BytesXMLMessage orig;
	final long msgCountNumber;
	final String[] headerLines;
//	final String msgDestName;  // this would only be used in the 1-line version
	AaAnsi msgDestNameFormatted;
    String msgType;  // could change
    boolean hasPrintedMsgTypeYet = false;  // have we printed out the message type?

    PayloadSection binary;
    PayloadSection xml = null;
    PayloadSection userProps = null;
    PayloadSection userData = null;
            
    public MessageHelper(BytesXMLMessage message, long msgCountNumber) {
    	orig = message;
    	this.msgCountNumber = msgCountNumber;
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
    		if (PayloadHelper.Helper.isOneLineMode()) {  // shouldn't be calling this yet!!?!?!!
        		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a(msgDestName);
    		} else {
        		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a("Topic '").colorizeTopic(msgDestName, PayloadHelper.Helper.getHighlightedTopicLevel()).a('\'');
    		}
    	}
    	msgDestNameFormatted.reset();
    }
    
    // make sure this is called before printMessage()
//    private void prepMessageForPrinting() {
//    	headerLines = orig.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
//    }
    
    public String buildFullStringObject() {
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
    	AaAnsi aaDest = new AaAnsi().fg(Elem.DESTINATION);
    	if (orig.getDestination() instanceof Queue) {
    		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
        	PayloadHelper.Helper.updateTopicIndentValue(msgDestName.length());
    		aaDest.a(msgDestName);
    	} else {  // a Topic
    		msgDestName = orig.getDestination().getName();
			msgDestName = PayloadHelper.Helper.updateTopicSpaceOutLevels(msgDestName);
       		PayloadHelper.Helper.updateTopicIndentValue(msgDestName.length());
        	if (!PayloadHelper.Helper.isOneLineMode()) {
        		aaDest.a("Topic '").colorizeTopic(msgDestName, PayloadHelper.Helper.getHighlightedTopicLevel()).fg(Elem.DESTINATION).a('\'');
        	} else {
        		aaDest.colorizeTopic(msgDestName, PayloadHelper.Helper.getHighlightedTopicLevel());
        	}
    	}
    	msgDestNameFormatted = aaDest.reset();
//    	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());        	
    }
    
    AaAnsi getMessageTypeLine() {
    	AaAnsi aa = AaAnsi.n().a("Message Type:                           ");
		aa.fg(Elem.PAYLOAD_TYPE).a(msgType).reset();
		return aa;
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
	
	private static AaAnsi printMessageStart(long num) {
        return printMessageBoundary(String.format(" Start Message #%d ", num));
	}
	
	private static AaAnsi printMessageEnd(long num) {
        return printMessageBoundary(String.format(" End Message #%d ", num));
	}

	public static AaAnsi printMessageStart() {
        return printMessageStart(PayloadHelper.Helper.getMessageCount());
	}
	
	public static AaAnsi printMessageEnd() {
        return printMessageEnd(PayloadHelper.Helper.getMessageCount());
	}

    SystemOutHelper printMessage() {
        // now it's time to try printing it!
        SystemOutHelper systemOut = new SystemOutHelper();
        if (!PayloadHelper.Helper.isOneLineMode()) {
            systemOut.println(printMessageStart(msgCountNumber));
            for (String line : headerLines) {
            	if (line.isEmpty() || line.matches("\\s*")) continue;  // testing 
				if (line.startsWith("User Property Map:") && userProps != null) {
            		if (PayloadHelper.Helper.getFormattingIndent() == 0) {
            			if (PayloadHelper.Helper.isAutoTrimPayload()) {
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
            		if (PayloadHelper.Helper.getFormattingIndent() > 0 && !PayloadHelper.Helper.isNoPayload()) systemOut.println();
            	} else if (line.startsWith("User Data:") && userData != null) {
            		if (PayloadHelper.Helper.getFormattingIndent() == 0) {
            			if (PayloadHelper.Helper.isAutoTrimPayload()) {
	                		systemOut.println(line);
            			} else {
	                		systemOut.println("User Data:                              " + userData.formatted);
            			}
            		} else {
                		systemOut.println(new AaAnsi().a(line).a(" bytes"));
                		systemOut.println(userData.formatted);
            		}
            		if (PayloadHelper.Helper.getFormattingIndent() > 0 && !PayloadHelper.Helper.isNoPayload()) systemOut.println();
            	} else if (line.startsWith("SDT Map:") || line.startsWith("SDT Stream:")) {
            		// skip (handled as part of the binary attachment)
            	} else if (line.startsWith("Binary Attachment:")) {
//            		assert binary != null;
                	printMsgTypeIfRequired(systemOut);
                	AaAnsi payloadText = AaAnsi.n().a(line).a(" bytes");
                	if (binary != null) PayloadHelper.Helper.handlePayloadSection(line, binary, payloadText);
                	systemOut.println(payloadText);
            	} else if (line.startsWith("XML:")) {
//            		assert xml != null;
                	printMsgTypeIfRequired(systemOut);
                	AaAnsi payloadText = AaAnsi.n().fg(Elem.WARN).a("XML Payload section:           ").reset();
                	payloadText.a("         ").a(line.substring(40)).a(" bytes").reset();
                	if (xml != null) PayloadHelper.Helper.handlePayloadSection(line, xml, payloadText);
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
            	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<NO PAYLOAD>").reset().toString());
            } else if (orig.hasAttachment() && orig.getAttachmentContentLength() == 0) {
            	printMsgTypeIfRequired(systemOut);
//            	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
            	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<EMPTY PAYLOAD>").reset().toString());
            	
            }
			if (PayloadHelper.Helper.getFormattingIndent() > 0) systemOut.println(printMessageEnd(msgCountNumber));
    	} else {  // one-line mode!
            updateTopicSpacing();  // now that we've determined if we're gonna filter this message, do the topic stuff
    		String payloadSizeString;
    		if (binary != null && xml != null) {
    			// that's not great for one-line printing!  but probably pretty rare!!!!
    			systemOut.println("Message contains both binary and XML payloads! Dunno which one to show for one-line mode.");
    			systemOut.println(orig.dump().trim());  // raw JCSMP full dump
    		} else if (PayloadHelper.Helper.isNoPayload()) {  // "-0" mode
				systemOut.println(msgDestNameFormatted);
    		} else {  // one payload section defined, or empty
				AaAnsi payload = null;
				if (binary != null) {
					payload = binary.formatted;
					payloadSizeString = binary.getSizeString();
				}
				else if (xml != null) {
					payload = xml.formatted;
					payloadSizeString = xml.getSizeString();
				}
				else {
					payload = AaAnsi.n().faintOn().a("<EMPTY> ").a(msgType).reset();  // hopefully an EMPTY message
					payloadSizeString = "";  // empty payload is already the payload
				}
//				int minPayloadStringSizeLength = payloadSizeString.length();
				minLengthPayloadLength.add(payloadSizeString.length());
				if (userProps != null) {
					if (userProps.numElements == 0) maxLengthUserPropsList.add(0);
					if (userProps.numElements < 10) maxLengthUserPropsList.add(1);
					else if (userProps.numElements < 100) maxLengthUserPropsList.add(2);
					else if (userProps.numElements < 100) maxLengthUserPropsList.add(3);  // assume 999 is the most!
				}
				else  maxLengthUserPropsList.add(0);
				int currentScreenWidth = AnsiConsole.getTerminalWidth();
				if (currentScreenWidth == 0) currentScreenWidth = 80;  // force
				// we need all that in case we're in -1 mode and we need to trim
				
				if (PayloadHelper.Helper.getCurrentIndent() == 2) {  // two-line mode
					AaAnsi props = AaAnsi.n().fg(Elem.PAYLOAD_TYPE);
					if (userProps != null) {
						props.a(userProps.numElements + (userProps.numElements == 1 ? " UserProp " : " UserProps")).reset();
//						props.a(userProps.numElements + " UserProps").reset();
					} else {
						props.faintOn().a("- UserProps").reset();
					}
					int spaceToAdd = currentScreenWidth - msgDestNameFormatted.length() - props.length();
					if (spaceToAdd < 0) {
	    				systemOut.print(msgDestNameFormatted.trim(msgDestNameFormatted.length() + spaceToAdd - 1)).print(" ");
					} else {
	    				systemOut.print(msgDestNameFormatted);
	    				systemOut.print(UsefulUtils.pad(spaceToAdd, ' '));
					}
					systemOut.println(props);
					systemOut.print("  ");
    				if (PayloadHelper.Helper.isAutoTrimPayload()) {
    					if (payload.length() > currentScreenWidth - PayloadHelper.Helper.getCurrentIndent()) {
        					systemOut.print(payload.trim(currentScreenWidth - PayloadHelper.Helper.getCurrentIndent() - payloadSizeString.length()));
        					systemOut.println(payloadSizeString);
    					} else {  // enough space
    						systemOut.println(payload);
    					}
    				}
    				else {
    					systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
    					if (PayloadHelper.Helper.getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
    				}
				} else {  // proper one-line mode!
					final int PADDING_CORRECTION = 1;
    				int minPayloadLength = minLengthPayloadLength.getMax();
    				if (maxLengthUserPropsList.getMax() > 0) minPayloadLength += maxLengthUserPropsList.getMax() + 2 - PADDING_CORRECTION;  // user prop spacing + 1 + 1 for blackspace
    				int effectiveIndent = PayloadHelper.Helper.getCurrentIndent();
//    				System.out.printf("minPayloadLength=%d, maxLengthUserProps=%d%n", minLengthPayloadLength.getMax(), maxLengthUserPropsList.getMax());
    				if (PayloadHelper.Helper.isAutoResizeIndent()) {  // mode -1
    					if (PayloadHelper.Helper.getCurrentIndent() > currentScreenWidth - minPayloadLength - 1) {  // need to trim the topic!   minPL = 11
    						effectiveIndent = currentScreenWidth - minPayloadLength - 1;
    						systemOut.print(msgDestNameFormatted.trim(effectiveIndent-1));
    						int spaceToAdd = effectiveIndent - Math.min(msgDestNameFormatted.length(), effectiveIndent-1);
    						systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
    					} else {
    						systemOut.print(msgDestNameFormatted);
    						int spaceToAdd = PayloadHelper.Helper.getCurrentIndent() - msgDestNameFormatted.length();
    						systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
    					}
    				} else {
    					systemOut.print(msgDestNameFormatted.trim(PayloadHelper.Helper.getCurrentIndent()-1));
    					int spaceToAdd = PayloadHelper.Helper.getCurrentIndent() - Math.min(msgDestNameFormatted.length(), PayloadHelper.Helper.getCurrentIndent()-1);
    					systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
    				}
    				
					AaAnsi props = AaAnsi.n().fg(Elem.PAYLOAD_TYPE);
//					AaAnsi props = AaAnsi.n().insertBlackSpace().a(' ').fg(Elem.BYTES);
					if (maxLengthUserPropsList.getMax() > 0) {
//						props.blackOn();
						if (userProps == null || userProps.numElements == 0) props.faintOn().a('-');
						else props.a(userProps.numElements);
//						props.blackOff();
						props.a(' ');
					}
					systemOut.print(props.reset());
					if (PayloadHelper.Helper.isAutoTrimPayload()) {
						int trimLength = currentScreenWidth - effectiveIndent - PADDING_CORRECTION - props.length();// - payloadSizeString.length();
						if (payload.length() <= trimLength) {
							systemOut.println(payload);
						} else {
//							System.out.printf("w=%d,in=%d,efIn=%d,MXpay=%d,len=%d,trim=%d%n", currentScreenWidth, getCurrentIndent(), effectiveIndent, minLengthPayloadLength.getMax(), payloadSizeString.length(), trimLength);
							systemOut.print(payload.trim(trimLength - payloadSizeString.length())).println(payloadSizeString);
						}
					}
					else {
						systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
//					System.out.printf("charCount=%d, calcLength=%d, width=%d, size=%d%n", binary.formatted.getCharCount(), AaAnsi.length(binary.formatted.toString()),currentScreenWidth,Math.abs(INDENT)+binary.formatted.getCharCount());
						if (PayloadHelper.Helper.getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
					}
				}
    		}
    	}
		return systemOut;
    }
}
