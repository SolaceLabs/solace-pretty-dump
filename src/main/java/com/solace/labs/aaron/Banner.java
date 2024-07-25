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

package com.solace.labs.aaron;

import java.util.HashMap;
import java.util.Map;

import org.fusesource.jansi.AnsiConsole;

public class Banner {

	static int maxLen = 0;
	static int pieceLen = 0;
	
	enum Which {
		DUMP,
		WRAP,
		;
	}
	
	private static Map<Which, String[]> banners = new HashMap<>();
	static {
		banners.put(Which.DUMP, new String[] {
	    	" __________                 __    __          ________",
	    	" \\______   \\_______   _____/  |__/  |_ ___.__.\\______ \\  __ __  _____ ______ ",
	    	"  |     ___/\\_  __ \\_/ __ \\   __\\   __<   |  | |    |  \\|  |  \\/     \\\\____ \\   ",
	    	"  |    |     |  | \\/\\  ___/|  |  |  |  \\___  | |    `   \\  |  /  Y Y  \\  |_> >  ",
	    	"  |____|     |__|    \\___  >__|  |__|  / ____|/_______  /____/|__|_|  /   __/   ",
	    	"                         \\/            \\/             \\/   by Aaron \\/|__|      ",
	    	"                                                                                "
		});
//		banners.put(Which.DUMP, new String[] {
//			" __________               __    __          __________       .__        __     ",
//			" \\______   \\______  _____/  |__/  |_ ___.__.\\______   \\______|__| _____/  |_   ",
//			"  |     ___|_  __ \\/ __ \\   __\\   __<   |  | |     ___|_  __ \\  |/    \\   __\\  ",
//			"  |    |    |  | \\|  ___/|  |  |  |  \\___  | |    |    |  | \\/  |   |  \\  |    ",
//			"  |____|    |__|   \\___  >__|  |__|  / ____| |____|    |__|  |__|___|  /__|    ",
//			"                       \\/            \\/                    by Aaron  \\/        ",
//			"                                                                               "
//		});
		banners.put(Which.WRAP, new String[] {
			" __________                 __    __           __      __                         ",
			" \\______   \\_______   _____/  |__/  |_ ___.__./  \\    /  \\____________  ______    ",
			"  |     ___/\\_  __ \\_/ __ \\   __\\   __<   |  |\\   \\/\\/   /\\_  __ \\__  \\ \\____ \\   ",
			"  |    |     |  | \\/\\  ___/|  |  |  |  \\___  | \\        /  |  | \\// __ \\|  |_> >  ",
			"  |____|     |__|    \\___  >__|  |__|  / ____|  \\__/\\  /   |__|  (____  /   __/   ",
			"                         \\/            \\/            \\/    by Aaron   \\/|__|      ",
	    	"                                                                                   "
		});
	}
	
    private static String printBannerStandard(String[] banner) {
    	AaAnsi ansi = new AaAnsi();
    	ansi.fg(10).a(banner[0]).a('\n');
    	ansi.fg(2).a(banner[1]).a('\n');
    	ansi.fg(14).a(banner[2]).a('\n');
    	ansi.fg(6).a(banner[3]).a('\n');
    	ansi.fg(12).a(banner[4]).a('\n');
    	ansi.fg(4).a(banner[5]).a('\n');
    	return ansi.reset().toString();
    }
    

    private static String printBannerLight(String[] banner) {
    	AaAnsi ansi = new AaAnsi();
    	ansi.fg(20).a(banner[0]).a('\n');
    	ansi.fg(90).a(banner[1]).a('\n');
    	ansi.fg(124).a(banner[2]).a('\n');
    	ansi.fg(100).a(banner[3]).a('\n');
    	ansi.fg(28).a(banner[4]).a('\n');  // should be 40
    	ansi.fg(30).a(banner[5]).a('\n');
    	return ansi.reset().toString();
    }

	// 22, 28, 34, 40, 46, 83, 120, 157, 194, 231
    private static String printBannerMatrix(String[] banner) {
    	AaAnsi ansi = new AaAnsi();
    	ansi.fg(83).a(banner[0]).a('\n');
    	ansi.fg(120).a(banner[1]).a('\n');
    	ansi.fg(46).a(banner[2]).a('\n');
    	ansi.fg(40).a(banner[3]).a('\n');
    	ansi.fg(34).a(banner[4]).a('\n');  // should be 40
    	ansi.fg(22).a(banner[5]).a('\n');
    	return ansi.reset().toString();
    }

    static String printBanner(Which which) {
    	String[] banner = banners.get(which);
    	switch (AaAnsi.getColorMode()) {
    	case VIVID:
    		return printBannerVivid2(banner);
    	case MATRIX:
    		return printBannerMatrix(banner);
    	case LIGHT:
    		return printBannerLight(banner);
    	default:
    		return printBannerStandard(banner);
    	}
    }

    /* Original one, without the "shadow" or 3d effect */
    /*private static String printBannerVivid() {
    	for (String s : banner) {
    		maxLen = Math.max(maxLen, s.length());
    	}
//    	System.out.println("maxLen = " + maxLen);
    	pieceLen = maxLen / 6;
//    	System.out.println("pieceLen = " + pieceLen);
    	
    	AaAnsi ansi = new AaAnsi();
    	int col = 196;
    	for (int i=0; i<6; i++) {
    		ansi.fg(col);
//    		ansi.a(piece(banner[i], 0, 0, i*5));
    		ansi.a(piece(banner[i], 0, 0, 0));
	    	for (int j=1; j<6; j++) {
	    		ansi.fg(col + j);
//	    		ansi.a(piece(banner[i], j, i*5, i*5));
	    		ansi.a(piece(banner[i], j, 0, 0));
	    	}
	    	ansi.a('\n');
//	    	col -= (201-171);
	    	col -= (196-166);
    	}    	
    	return ansi.reset().toString();
    }

	private static String piece(String s, int p, int i, int o) {
		int len = s.length();
		if (p == 5) return (s.substring(Math.min(p*pieceLen + i, len), len));  // all of it
		return (s.substring(Math.min(p*pieceLen + i, len), Math.min(p*pieceLen + o + pieceLen, len)));
	}*/

    
    private static final int STARTING_VIVID_COLOR = 196;
    
    private static int getCol(int row, int pieceLen, int pos) {
    	return (STARTING_VIVID_COLOR - (row*30)) + Math.min(5, (pos / pieceLen));
    }
    
    private static boolean anythingBelow(String[] banner, int i, int j) {
    	if (i == 6) return false;
    	if (i == 5) return banner[6].charAt(j) != ' ';
    	if (i == 4) return banner[6].charAt(j) != ' ' || banner[5].charAt(j) != ' ';
    	if (i == 3) return banner[6].charAt(j) != ' ' || banner[5].charAt(j) != ' ' || banner[4].charAt(j) != ' ';
    	throw new IllegalStateException();
    }
    
    // RAELLY REALLY BAD CODE, BAD LOGIC, ALGORITHM... I'm kind of embarrassed
    // all this does is add kind of a shadow/perspective view of the banner text down-right of the text
    private static void preprocessBanner(String[] banner) {
    	for (int i=3; i<7; i++) {
    		StringBuilder sb = new StringBuilder();
    		for (int j=0; j < banner[i].length(); j++) {
    			if (banner[i].charAt(j) != ' ') {
    				sb.append(banner[i].charAt(j));
    				continue;
    			}
    			if (j > 1) {
    				if (j > banner[i-1].length()-1) {
    					sb.append(banner[i].charAt(j));
    					continue;
    				}
    				if (anythingBelow(banner,i,j)) {
    					sb.append(banner[i].charAt(j));
    					continue;
    				}
    				char ul = banner[i-1].charAt(j-2);
    				if (ul == '*') {
    					sb.append(banner[i].charAt(j));
    					continue;
    				}
//    				char u = banner[i-1].charAt(j);
//    				char l = banner[i].charAt(j-2);
//					if (ul != ' '/* || (u != ' ' && l != ' ') */) {
					if (ul == '_' || ul == '|' || ul == '\\' || ul == '/' || ul == '>') {
						if (Character.isAlphabetic(banner[i].charAt(j-1)) ) { //|| Character.isAlphabetic(banner[i].charAt(j+1))) {
	    					sb.append(banner[i].charAt(j));
	    					continue;
						}
    					sb.append('*');
    					continue;
    				}
    			}
    			sb.append(banner[i].charAt(j));
				continue;
    		}
    		banner[i] = sb.toString();
    	}
    }
    

    private static String printBannerVivid2(String[] banner) {
    	int maxLen = 1;
    	for (String s : banner) {
    		maxLen = Math.max(maxLen, s.length());
    	}
    	int pieceLen = maxLen / 6;
    	preprocessBanner(banner);  // make the drop shadow
    	AaAnsi ansi = new AaAnsi();
    	for (int i=0; i<banner.length; i++) {
    		for (int j=0; j<banner[i].length(); j++) {
    			ansi.reset();
    			if (banner[i].charAt(j) == '*') {
    				ansi.faintOn().fg(getCol(i-1, pieceLen, j-2)).a(banner[i-1].charAt(j-2));
    			} else {
    				ansi.fg(getCol(i, pieceLen, j)).a(banner[i].charAt(j));
    			}
    		}
	    	ansi.a('\n');
    	}
    	return ansi.reset().toString();
    }
    
    public static void main(String... args) {
    	AnsiConsole.systemInstall();
    	System.out.println(printBannerVivid2(banners.get(Which.WRAP)));
    }
}
