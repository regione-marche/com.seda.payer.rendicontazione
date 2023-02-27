package com.seda.payer.unisciflussi.config;

import java.text.MessageFormat;
import java.util.ResourceBundle;
 

/**
 * PG130100
 */
public enum PropertiesPath {
			inputDir,
			outputDir, 
			progressDir,
			backupDir,
			ftpInputUrl,
			ftpInputDir,
			ftpInputUser,
			ftpInputPassword,
			ftpOutputUrl,
			ftpOutputDir,
			ftpOutputUser,
			ftpOutputPassword,
			tipologiaFile,
			securityEncriptionIV,
			securityEncriptionKey
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
