package com.solace.labs.aaron;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import com.solace.labs.aaron.Banner.Which;
import com.solacesystems.jcsmp.JCSMPException;

public class PrettyWrap {

    private static final Logger logger = LogManager.getLogger(PrettyWrap.class);
//    private static Map<Sub, Method> protobufCallbacks = new HashMap<>();

    /** ensures that c is either 0-9a-f */
    private static boolean invalidHex(char c) {
    	return c < 0x30 || c > 0x66 || (c >= 0x3a && c <= 0x60);
    }
    
    private static int convertChar(char c) {
		if (c < 64) {  // means it's a number
			return (c - 48);
		} else {  // letter a-f
			return (c - 87);
		}
    }
    
    private static int parseSdkPerf(String line, byte[] array) {
    	int count = 0;
    	int pos = 2;
    	byte b = 0;
    	while (pos < 55 && count < 16) {
    		char c = line.charAt(pos);
    		if (c == ' ') {
    			pos++;
    			continue;
    		}
    		char c2 = line.charAt(pos+1);
    		if (invalidHex(c) || invalidHex(c2)) {
    			throw new IllegalArgumentException("Line contains non-hex chars: '" + line + "'");
    		}
    		// should check to make sure char is in range!
    		b = (byte)(convertChar(c) << 4 | convertChar(c2));
    		array[count++] = b;
    		pos += 3;
    	}
    	return count;
    }
    
    private static long lineIndentCount = 0;
//    private static final int[] bgCols = new int[] { 17, 53, 52, 58, 22, 23 };
    
    private static void wrapPrintln(AaAnsi aa) {
    	wrapPrintln(aa.toString());
    }
    
    private static void wrapPrintln(String s) {
    	if ("indent".equals("indent") && (AaAnsi.getColorMode() == AaAnsi.ColorMode.VIVID || AaAnsi.getColorMode() == AaAnsi.ColorMode.LIGHT)) {
	    	String[] lines = s.split("\n");
	    	Ansi ansi = new Ansi();
//	    	AaAnsi aa = new AaAnsi();
	    	for (String line : lines) {
	    		ansi.fg(AaAnsi.rainbowTable[(int)((lineIndentCount++)/2 % AaAnsi.rainbowTable.length)]).a('│').reset().a(' ').a(line).a('\n');
//	    		ansi.bg(bgCols[(int)((lineIndentCount++)/4 % bgCols.length)]).a('│').reset().a(' ').a(line).a('\n');
//	    		ansi.fg(AaAnsi.rainbowTable[(int)((lineIndentCount++)/2 % AaAnsi.rainbowTable.length)]).bg(bgCols[(int)((lineIndentCount++)/4 % bgCols.length)]).a('│').reset().a(' ').a(line).a('\n');
//	    		aa.makeFaint().a("│ ").reset().aRaw(line);
	    	}
	    	System.out.print(ansi.toString());
//	    	System.out.println(aa.toString());
    	} else {
    		System.out.println(s);
    	}
    }
    
    /*
Destination:                            Topic 'q1/abc'
	*/
    static String extractTopic(String line) {
    	if (line.startsWith("Destination:                            Topic '") && line.endsWith("'")) {
    		String topic = line.substring(47, line.length()-1);
    		return topic;
    	}
    	return "";
    }
    
    static volatile boolean shutdown = false;
    
	public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
		
		AnsiConsole.systemInstall();
		
//		String test = "{traceId=de72a50e1dac200c342efe52bfc07746, spanId=93ee944623c1e6a1, sampled=true, traceState=}";
//		test = "{JMS_Solace_DeliverToOne:false,JMS_Solace_DeadMsgQueueEligible:false,JMS_Solace_ElidingEligible:false,Solace_JMS_Prop_IS_Reply_Message:false,JMSXDeliveryCount:1}";
//		System.out.println(formatMapLookingThing(test));
//		System.exit(0);
//        protobufCallbacks = ProtoBufUtils.loadProtobufDefinitions();
//    	PayloadHelper payloadHelper = new PayloadHelper(StandardCharsets.UTF_8);
//    	PayloadHelper payloadHelper;
    	PayloadHelper.init(StandardCharsets.UTF_8);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		// test code
		
//		String binaryPayload = "^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^\n"
//				+ "Destination:                            Topic 'a'\n"
//				+ "Priority:                               4\n"
//				+ "Class Of Service:                       COS_1\n"
//				+ "DeliveryMode:                           DIRECT\n"
//				+ "Binary Attachment:                      len=1029\n"
////				+ "  1c 10 00 04 14 00 00 00  08 00 4d 19 57 58 e4 44      PK......   ..M.WX.D\n\n"
//				+ "  2b 00 00 00 10 00 00 00  08 00 4d 19 57 58 e4 44      PK......   ..M.WX.D\n\n"
//				+ "^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^";
//		BufferedReader in2 = new BufferedReader(new StringReader(binaryPayload));
		
		
		
		
		
		
		boolean insideMessage = false;
		boolean insidePayloadSection = false;
		boolean startingPayload = false;
		byte[] perLineArray = new byte[16];  // for reuse
		ByteBuffer bb = ByteBuffer.allocate(1024 * 1024);  // 1MB, allocate once and reuse!
		Arrays.fill(perLineArray, (byte)0);
//		int msgCount = 0;
		boolean legalPayload = true;
		boolean ignore = false;
//		String topic = "";
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		
        System.out.println(Banner.printBanner(Which.WRAP));
        System.out.print(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(String.format("PrettyDump WRAP mode for SdkPerf enabled... 😎%n%n")).toString());
        AaAnsi.resetAnsi(System.out);
		try {
			while (!shutdown) {
				String input = in.readLine();
				if (input == null) {  // nothing to do
					Thread.sleep(50);
//					System.out.print(".");
				} else {
//					seenInput = true;
//					if (!seenAtLeastSomeInputInOneSecond.get()) {  // first time seeing something
//				        seenAtLeastSomeInputInOneSecond.set(true);
//				        System.out.println(Banner.printBanner(Which.WRAP));
//				        System.out.print(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(String.format("PrettyDump WRAP mode for SdkPerf enabled... 😎%n%n")).toString());
//				        AaAnsi.resetAnsi(System.out);
//					}
					// probably should put the most likely/common lines at the top here..!
					if (ignore == true && input.isEmpty()) {
						ignore = false;  // end of ignoring SDTMap section!
						// but now what if we're wrapping C SdkPerf and there is no binary section? Just need to dump out the map
					} else if (ignore) {
						// continue
					} else if (input.contains("^^^ Start Message ^^^")) {
						assert !insideMessage;
						insideMessage = true;
//						System.out.println( input);
//						wrapPrintln(PayloadHelper.printMessageStart(++msgCount));
						wrapPrintln(MessageHelper.printMessageStart());
					} else if (input.contains("^^^ End Message ^^^")) {
						assert insideMessage;
						assert !insidePayloadSection;
						insideMessage = false;
//						System.out.println(input);
//						wrapPrintln(PayloadHelper.printMessageEnd(msgCount));
						wrapPrintln(MessageHelper.printMessageEnd());
						AaAnsi.resetAnsi(System.out);
//					} else if (input.matches("^(?:XML:|Binary Attachment:).*len.*")) {
					} else if (input.startsWith("XML:       ") || input.startsWith("Binary Attachment:  ") || input.startsWith("Binary Attachment String:  ") || input.startsWith("User Data:   ")) {
						// else if we're wrapping CCSMP, then there is no raw payload section, but it will say "Binary Attachment Map:" and we'll just keep dumping stuff out
						assert insideMessage;
						assert !insidePayloadSection;
						insidePayloadSection = true;
						startingPayload = true;  // should we reset the ByteBuffer just in case?
						bb.clear();
						legalPayload = true;
//						payloadSb = new StringBuilder();
						wrapPrintln(input);
//					} else if (input.startsWith("Binary Attachment Map:    ") || input.startsWith("Binary Attachment Stream:    ")) {
						// commented out because otherwise can't detect end of payload section in C SdkPerf
//						makeFancyString = true;  // make each line slightly fancier formatting
//						wrapPrintln(input);
					} else if (input.startsWith("Destination:      ") || input.startsWith("JMSDestination:    ")) {
						if (input.startsWith("Dest")) {
//							topic = extractTopic(input);  // if it is an actual topic, this will return it; otherwise empty string
						}
						wrapPrintln(PayloadHelper.colorizeDestination(input));
//						System.out.println(new AaAnsi().fg(Elem.DESTINATION).a(input).toString());
					} else if (input.startsWith("SDT Map:     ") || input.startsWith("SDT Stream:     ")) {  // this is only for JCSMP and derivatives
						ignore = true;
					} else if (startingPayload) {  // we just started, so we know this line is the first line
						assert insideMessage;
						assert insidePayloadSection;
						try {
							bb.put(perLineArray, 0, parseSdkPerf(input, perLineArray));
						} catch (IllegalArgumentException e) {
							legalPayload = false;
							bb.put(input.getBytes());
						}
						startingPayload = false;
					} else if (insidePayloadSection) {
						if (input.isEmpty() || !input.startsWith(" ")) {  // end of payload section!
							insidePayloadSection = false;
							bb.flip();  // ready to read!
							com.solace.labs.aaron.PayloadHelper.PayloadSection payload = PayloadHelper.Helper.buildPayloadSection(bb);
							if (payload.getType().contains("Non ") || payload.getType().contains("INVALID")) {
								wrapPrintln(new AaAnsi().invalid(payload.getType()).toString());
							} else {
								wrapPrintln(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(payload.getType()).toString());
							}
							wrapPrintln(payload.getFormattedPayload());
							if (!input.isEmpty()) wrapPrintln(input);
						} else {  // just gathering payload data here
							if (legalPayload) {  // continue
								bb.put(perLineArray, 0, parseSdkPerf(input, perLineArray));
							} else {
								bb.put(input.getBytes());
							}
						}
//					} else if (input.startsWith("JMSProperties:   ")) {
//						AaAnsi ansi = new AaAnsi().a(input.substring(0,40)).aStyledString(input.substring(40));
//						System.out.println(ansi);
					} else if (insideMessage) {
//						if (input.length() > 42 && input.charAt(40) == '{' && input.endsWith("}")) {
//							String sub = formatMapLookingThing(input.substring(40));
//							wrapPrintln(new StringBuilder().append(input.substring(0,40)).append(sub).toString());
//						} else {
//							wrapPrintln(input);
////							wrapPrintln(AaAnsi.n().aStyledString(input).toString());
//						}
						wrapPrintln(UsefulUtils.guessIfMapLookingThing(input));
//						if (input.isEmpty()) makeFancyString = false;
//						if (makeFancyString) wrapPrintln(AaAnsi.n().aStyledString(input).toString());
//						else wrapPrintln(input);
					} else {  // outside a message...
						if (input.startsWith("PUB MR(5s)")) {
							try {
								// PUB MR(5s)=    0, SUB MR(5s)=    0, CPU=0
								// PUB MR(5s)=    0, SUB MR(5s)=    0, CPU=0
								String[] pieces = input.split("=");
								// [PUB MR(5s)][    0, SUB MR(5s)][    0, CPU][0]
								String[] pub = pieces[1].split(",");
								// [PUB MR(5s)][    0][ SUB MR(5s)][    0, CPU][0]
								String[] sub = pieces[2].split(",");
								// [PUB MR(5s)][    0][ SUB MR(5s)][    0][ CPU][0]
								AaAnsi aa = new AaAnsi().a(pieces[0]).a('=').fg(Elem.KEY);
								if (pub[0].endsWith(" 0")) aa.faintOn();
								aa.fg(Elem.KEY).a(pub[0]).a('↑').reset().a(',').a(pub[1]).a('=').fg(Elem.STRING);
								if (sub[0].endsWith(" 0")) aa.faintOn();
								aa.fg(Elem.STRING).a(sub[0]).a('↓').reset().a(',').a(sub[1]).a('=');
								if (pieces[3].equals("0")) aa.fg(Elem.MSG_BREAK).a(pieces[3]);
								else {
									aa.fg(Elem.FLOAT);
									if (pieces[3].length() == 1) aa.faintOn();
									aa.a(pieces[0]);
								}
								wrapPrintln(aa.reset().toString());
							} catch (RuntimeException e) {  // oh well!
								logger.info("Had an issue trying to pretty-print the message rates",e);
								wrapPrintln(input);
							}
						} else if (input.startsWith("CPU usage")) {  // detected shutdown of SdkPerf
							pool.schedule(new Runnable() {
								@Override
								public void run() {
									shutdown = true;
								}
							}, 500, TimeUnit.MILLISECONDS);
						} else if (input.startsWith("CLASSPATH: ")) {
							// skip it... too long!
						} else {  // outside message and not a rate, so like a log or something
							wrapPrintln(input);
//							wrapPrintln(AaAnsi.n().aStyledString(input).toString());
						}
					}
				}
				if (input == "abc") break;
			}
    	} catch (RuntimeException e) {
    		System.out.println(e);
    		e.printStackTrace();
    		Runtime.getRuntime().halt(1);
    	} finally {
        	System.out.println("Goodbye! 👋🏼");
            AnsiConsole.systemUninstall();
    	}
	}
}
