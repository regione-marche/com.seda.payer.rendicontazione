/**
 * 
 */
package com.seda.payer.acquisisciflussiscarti.config;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author polenta
 *
 */
public enum PropertiesPath {
	//baseCatalogName,
	//baseLogger
	//, wsCommonsUrl
	//codiceSocietaSeda;//Giulia
	DEFAULT_NODE
	, NOTIFICHE_ENDPOINTURL
	, REJECT_PATH
	, REJECT_IN_PROGRESS_PATH
	, REJECT_PROCESSED_PATH
	, REJECT_REJECTED_PATH
	, wsEMailSenderUrl
	;

    private static ResourceBundle rb;

    public String format( Object... args ) {
        synchronized(PropertiesPath.class) {
            if(rb==null)
                rb = ResourceBundle.getBundle(PropertiesPath.class.getName());
            return MessageFormat.format(rb.getString(name()),args);
        }
    }
}
