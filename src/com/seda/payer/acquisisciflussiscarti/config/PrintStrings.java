package com.seda.payer.acquisisciflussiscarti.config;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author polenta
 *
 */

public enum PrintStrings {
	EMPTY,
	ROOT;
	
    private static ResourceBundle rb;

    public String format( Object... args ) {
        synchronized(PrintStrings.class) {
            if(rb==null)
                rb = ResourceBundle.getBundle(PrintStrings.class.getName());
            return MessageFormat.format(rb.getString(name()),args);
        }
    }
}
