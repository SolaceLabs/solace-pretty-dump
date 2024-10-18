/*
 * Copyright 2023-2024 Solace Corporation. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class KeyboardHandler {

	
    static void sttyCooked() {
        doStty(false);
    }
    
    static void sttyRaw() {
        doStty(true);
    }

    /**
     * Call 'stty' to set raw or cooked mode.
     *
     * @param mode if true, set raw mode, otherwise set cooked mode
     */
    private static void doStty(final boolean mode) {
        String [] cmdRaw = {
            "/bin/sh", "-c", "stty -ignbrk -brkint -parmrk -istrip -inlcr -igncr -icrnl -ixon -opost -echo -echonl -icanon -isig -iexten -parenb cs8 min 1 < /dev/tty"
        };
        String [] cmdCooked = {
            "/bin/sh", "-c", "stty sane cooked < /dev/tty"
        };
        try {
            Process process;
            if (mode) {
                process = Runtime.getRuntime().exec(cmdRaw);
            } else {
                process = Runtime.getRuntime().exec(cmdCooked);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line = in.readLine();
            if ((line != null) && (line.length() > 0)) {
                System.err.println("WEIRD?! Normal output from stty: " + line);
            }
            while (true) {
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
                line = err.readLine();
                if ((line != null) && (line.length() > 0)) {
                    System.err.println("Error output from stty: " + line);
                }
                try {
                    process.waitFor();
                    break;
                } catch (InterruptedException e) {
//                    if (debugToStderr) {
                        e.printStackTrace();
//                    }
                }
            }
            int rc = process.exitValue();
            if (rc != 0) {
                System.err.println("stty returned error code: " + rc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    
    static void enableSignalOverride() {
    	
//		sun.misc.Signal.handle(new sun.misc.Signal("INT"),  // SIGINT
//	    signal -> {
//	    	System.out.println("Interrupted by Ctrl+C");
//	    	System.exit(0);
//	    });
    	
    	
    }
    
    public static void main(String... args) throws IOException {

		final Thread shutdownThread = new Thread(() -> {
			sttyCooked();
    		System.out.println();
		});
		Runtime.getRuntime().addShutdownHook(shutdownThread);
    	
    	try {
    		sttyRaw();
    		
    		while (true) {
    			int keypress;
    			if (System.in.available() > 0) {
    				keypress = System.in.read();
    				System.out.print(keypress + ": " + (char)keypress + ", ");
    				if (keypress == 113) break;
    				continue;
    			}
    			try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					break;
				}
    		}
    	} finally {
    		sttyCooked();
    		System.out.println();
    	}
    }
    
    
    
    
    
    
}
