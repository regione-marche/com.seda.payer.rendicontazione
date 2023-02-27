package com.seda.payer.rendicontazione.bap;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.seda.bap.components.core.spi.ClassPrinting;
import com.seda.commons.properties.tree.PropertiesTree; 
import com.seda.payer.rendicontazione.util.CipherHelper;

public class RendicontazioneBapContext implements Serializable {

	private static final long serialVersionUID = 1;

	private Properties config;
	
	private Logger logger;
	protected HashMap<String, List<String>> parameters = new HashMap<String, List<String>>();
	
	public RendicontazioneBapContext() {
	}
	 
	public RendicontazioneBapContext(PropertiesTree propertiesTree,
			DataSource dataSource, String schema, Logger logger,
			ClassPrinting printers, String idJob, Properties config) {
		super();
		this.config = config;
		this.logger = logger;
	}	
	
	/**
	 * Ritorna il logger
	 * @return the logger
	 */
	public String getLogger() {
		return config.getProperty("log4j.path");
	}
	/**
	 * Setta il logger
	 * @param logger the logger to set
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	 
	public Properties getProperties() {
		return config;
	}
	public void setConfig(Properties config) {
		this.config = config;
	}
	
	public String getEncryptionIV() {
		return config.getProperty("security.encryption.iv");
	}

	public String getEncryptionKEY() {
		return config.getProperty("security.encryption.key");
	}

	public String encryptPassword(String clearPass)
	{
		CipherHelper cipher = new CipherHelper(getEncryptionIV(), getEncryptionKEY());
		String cryptedPassword = cipher.cryptData(clearPass);
		return cryptedPassword;
	}
	
	public int addParameter(String name, String value) {
		if(!this.parameters.containsKey(name)) {
			this.parameters.put(name, new LinkedList<String>());
		}
		this.parameters.get(name).add(value); //Aggiunge un valore alla lista delle ripetizioni
		return this.parameters.get(name).size();
	}
	
	public String getParameter(String name) {
		if(parameters.containsKey(name))
			return (String)parameters.get(name).get(0);
		else
			return "";
	}
	
	/** Parametri di lancio della procedura, passati dallo schedulatore */
	public void loadSchedeBap(String[] params) {
		for (int i=0;i < params.length; i++ ) {
			String[] p = params[i].split("\\s+");
			//if (p[0].equals("END") || p[0].equals("CONFIGPATH") || p[0].equals("CUTECUTE")|| p[0].equals("ENTE")) {
			if (p[0].equals("END")) {
				if (p[1].trim().equals("")) {
					addParameter(p[0].trim(), "");
				} else {
					addParameter(p[0].trim(), p[1].trim());
				}
			} else {
				addParameter(p[0].trim(), p[1].trim());//Nome parametro - valore(Aggiunge Lista di valori per schede con ripetizione)	
			}
		}
	}

	public String formatDate(Date date, String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(date);
	}

	/** fornito come parametro BAP */
	public String getCodiceUtente() {
		return getParameter("CUTECUTE");
	}
}
