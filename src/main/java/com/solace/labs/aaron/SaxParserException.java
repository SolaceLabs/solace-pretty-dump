/**
 * Copyright 2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company.
 * If there are no contracts in place use of this source
 * is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported
 * by Solace unless such support is provided for under 
 * an agreement signed between you and Solace.
 */
package com.solace.labs.aaron;

public class SaxParserException extends Exception {

    private static final long serialVersionUID = 1L;

    public SaxParserException(String message) {
        super(message);
    }

    public SaxParserException(Throwable cause) {
        super(cause);
    }

    public SaxParserException(String message,Throwable cause) {
        super(message,cause);
    }
}
