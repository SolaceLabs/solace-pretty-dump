package com.solace.labs.aaron;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.solace.labs.aaron.utils.BoundedLinkedList;
import com.solace.labs.topic.Sub;

public class ConfigState {

	private static final int TOPICS_LENGTH_LIST_SIZE = 50;
	private static final int TOPIC_LEVELS_LENGTH_LIST_SIZE = 200;
	
	
	boolean isShutdown = false;          // are we done yet?
	boolean isConnected = false;
	boolean isFlowActive = false;
	boolean includeTimestamp = false;
	boolean noExport = true;
	boolean isCompressed = false;

    int highlightTopicLevel = -1;
    int INDENT = 2;  // default starting value, keeping it all-caps for retro v0.0.1 value
    boolean oneLineMode = false;
    boolean noPayload = false;
    boolean autoResizeIndent = false;  // specify -1 as indent for this MODE
    boolean autoSpaceTopicLevels = false;  // specify +something to space out the levels
    boolean autoTrimPayload = false;
    boolean rawPayload = true;
    
    BoundedLinkedList.ComparableList<Integer> topicsLengthList = new BoundedLinkedList.ComparableList<>(TOPICS_LENGTH_LIST_SIZE);
    List<BoundedLinkedList.ComparableList<Integer>> topicLevelsLengthList = new ArrayList<>();
    BoundedLinkedList<MessageObject> lastNMessagesList = null;

    long currentMsgCount = 0;
    long filteredCount = 0;
    Pattern filterRegexPattern = null;

    Charset charset = StandardCharsets.UTF_8;
	CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

    Map<Sub, Method> protobufCallbacks = new HashMap<>();

	
	static String DTF_FORMAT = "HH:mm:ss.SS ";

	public void setAutoTrimPayload(boolean trim) {
		autoTrimPayload = trim;
	}

	public void toggleAutoTrimPayload() {
		autoTrimPayload = !autoTrimPayload;
	}

	public void enableLastNMessage(int amount) {
		lastNMessagesList = new BoundedLinkedList<>(amount);
	}
	
	public boolean isOneLineMode() {
		return oneLineMode;
	}
	
	public boolean isNoPayload() {
		return noPayload;
	}
	
	public boolean isAutoResizeIndent() {
		return autoResizeIndent;
	}
	
	public boolean isAutoTrimPayload() {
		return autoTrimPayload;
	}
	
	public void setAutoSpaceTopicLevels(boolean enabled) {
		autoSpaceTopicLevels = enabled;
	}
	
	public boolean isAutoSpaceTopicLevelsEnabled() {
		return autoSpaceTopicLevels;
	}
    
	public boolean isLastNMessagesEnabled() {
		return lastNMessagesList != null;
	}
	
	public int getHighlightedTopicLevel() {
		return highlightTopicLevel;
	}

	public void setHighlightedTopicLevel(int level) {
		highlightTopicLevel = level;
	}

	public int getLastNMessagesSize() {
		if (isLastNMessagesEnabled()) return lastNMessagesList.size();
		else return 0;
	}

	public int getLastNMessagesCapacity() {
		if (isLastNMessagesEnabled()) return lastNMessagesList.capacity();
		else return 0;
	}

	public BoundedLinkedList<MessageObject> getLastNMessages() {
		return lastNMessagesList;
	}
	


    /** this method tracks what the longest topic string has been for the last 50 messages, so things line up nicely with indent mode "-1"
	 *  only used in "-1" auto-indent mode
     */
    public void updateTopicIndentValue(int maxTopicLength) {
    	if (autoResizeIndent) {
	    	topicsLengthList.add(maxTopicLength);
	    	if (topicsLengthList.max() + 1 != INDENT) {  // changed our current max
	//    		int from = Math.abs(INDENT);
	    		INDENT = topicsLengthList.max() + 1;  // so INDENT will always be at least 3 (even MQTT spec states topic must be length > 1)
	//    		System.out.println(new org.fusesource.jansi.Ansi().reset().a(org.fusesource.jansi.Ansi.Attribute.INTENSITY_FAINT).a("** changing INDENT from " + from + " to " + Math.abs(INDENT) + "**").reset().toString());
	//    	} else {
	//    		System.out.println(new org.fusesource.jansi.Ansi().reset().a(org.fusesource.jansi.Ansi.Attribute.INTENSITY_FAINT).a("** keeping INDENT = " + Math.abs(INDENT) + "**").reset().toString());
	    	}
    	}
    }
    
    
    /** returns a topic string that has had all its levels spaced out by the appropriate amount accoding to the topic lengths list */
    public String updateTopicSpaceOutLevels(String topic) {
    	if (autoSpaceTopicLevels) {
	    	String[] levels = topic.split("/");
	    	if (topicLevelsLengthList.size() < levels.length) {
	    		for (int i=topicLevelsLengthList.size(); i < levels.length; i++) {
	    			topicLevelsLengthList.add(new BoundedLinkedList.ComparableList<Integer>(TOPIC_LEVELS_LENGTH_LIST_SIZE));  // Keep column sizes for 200 msgs
	    		}
	    	}
	    	for (int i=0; i < topicLevelsLengthList.size(); i++) {
	    		if (i < levels.length) {
	    			topicLevelsLengthList.get(i).add(levels[i].length());
	    		} else {
	    			topicLevelsLengthList.get(i).add(0);
	    		}
	    	}
	    	StringBuilder sb = new StringBuilder();
	    	for (int i=0; i < levels.length; i++) {
	    		sb.append(levels[i]);
	    		int max = topicLevelsLengthList.get(i).max();
	    		if (i < levels.length-1) {
		    		if (max > levels[i].length()) {
						sb.append(UsefulUtils.pad(max - levels[i].length(), /*'⋅'*/ '·' ));
		    		}
		    		if (INDENT == Integer.MIN_VALUE) sb.append("·/");// ("⋅/");  // always space out topic-only mode
		    		else sb.append('/');
		    	}
	    	}
	    	return sb.toString();
    	} else {
    		return topic;
    	}
    }
    
    
    
    public int getCurrentIndent() {
    	return INDENT;
    }
    
    /** for auto-indent one-line "-1" mode */
    int getFormattingIndent() {
    	if (oneLineMode) return 0;
    	return INDENT;
//    	return Math.min(INDENT, currentScreenWidth - 15);
    }
    
    /** Throws NumberFormat if it can't be parsed, or IllegalArgument if it is a number, but invalid */
    public void dealWithIndentParam(String indentStr) throws NumberFormatException, IllegalArgumentException {
    	// first, switch any pluses to minuses
    	if (indentStr.startsWith("+") && indentStr.length() >= 2) {
    		autoSpaceTopicLevels = true;
    		indentStr = "-" + indentStr.substring(1);
    	}
		int indent = Integer.parseInt(indentStr);  // might throw
		if (indent < -250 || indent > 8) throw new IllegalArgumentException();
		INDENT = indent;
		if (INDENT < 0) {
			oneLineMode = true;
			if (INDENT == -1) {
				autoResizeIndent = true;  // use auto-resizing based on max topic length
				INDENT = 3;  // starting value (1 + 2 for padding)
				updateTopicIndentValue(2);  // now update it
			} else if (INDENT == -2) {  // two line mode
				INDENT = Math.abs(INDENT);
			} else {
				INDENT = Math.abs(INDENT) + 2; // TODO why is this 2?  I think 1 for ... and 1 for space
//				updateTopicIndentValue(INDENT);  // now update it  TODO why do we need to update if not auto-indenting?
			}
		} else if (INDENT == 0) {
			if (indentStr.equals("-0")) {  // special case, print topic only
				INDENT = Integer.MIN_VALUE;  // not necessary anymore
				oneLineMode = true;
				noPayload = true;
			} else if (indentStr.equals("00")) {
				INDENT = 2;
				noPayload = true;
			} else if (indentStr.equals("000")) {
				noPayload = true;
//				INDENT = 0;  // that's already done above!
			} else if (indentStr.equals("0")) {
				// nothing, normal
			} else if (indentStr.equals("0000")) {  // something new, no user proper or data
				INDENT = 0;
				noPayload = true;
				autoTrimPayload = true;
			} else {  // shouldn't be anything else (e.g. "0000")
				throw new IllegalArgumentException();
			}
		}
    }
	
	public void setRegexFilterPattern(Pattern regex) {
		filterRegexPattern = regex;
	}
	
	public void incMessageCount() {
		currentMsgCount++;
	}
	
	public void incFilteredCount() {
		filteredCount++;
	}
	
	public long getMessageCount() {
		return currentMsgCount;
    }
	
	public long getFilteredCount() {
		return filteredCount;
	}
	
	public void setProtobufCallbacks(Map<Sub, Method> map) {
		protobufCallbacks = map;
	}



	
	/** used when calculating one-line indent values, includes a space at the end of the timestamp */
	public int getTimestampIndentIfEnabled() {
		if (includeTimestamp) return DTF_FORMAT.length();
		else return 0;
	}
	
	public void setCharset(Charset charset) {
		this.charset = charset;
		// I think replace is the default action anyhow?  This must be leftover from when I was erroring?
		decoder = this.charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
	}
}
