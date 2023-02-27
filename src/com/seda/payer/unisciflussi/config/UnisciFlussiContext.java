package com.seda.payer.unisciflussi.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import com.seda.bap.components.core.spi.ClassPrinting;
import com.seda.commons.properties.tree.PropertiesTree; 
import com.seda.payer.rendicontazione.util.CipherHelper;

public class UnisciFlussiContext implements Serializable {
	private static final long serialVersionUID = 1L;

	private Properties config;
	
	protected HashMap<String, List<String>> parameters = new HashMap<String, List<String>>();
	
	public UnisciFlussiContext() {
	}
	
	public UnisciFlussiContext(PropertiesTree propertiesTree,
			DataSource dataSource, String schema,
			ClassPrinting printers, String idJob, Properties config) {
		super();
		this.config = config;
	}
	
	public Properties getProperties() {
		return config;
	}
	
	public void setConfig(Properties config) {
		this.config = config;
	}

	public String getInputDir(String cuteCute) {
		return config.getProperty(PropertiesPath.inputDir.format(cuteCute));
	}
	
	public String getOutputDir(String cuteCute) {
		return config.getProperty(PropertiesPath.outputDir.format(cuteCute));
	}

	public String getProgressDir(String cuteCute) {
		return config.getProperty(PropertiesPath.progressDir.format(cuteCute));
	}
	
	public String getBackupDir(String cuteCute) {
		return config.getProperty(PropertiesPath.backupDir.format(cuteCute));
	}

	public String getFtpInputUrl(String cuteCute) {
		return config.getProperty(PropertiesPath.ftpInputUrl.format(cuteCute));
	}
	
	public String getFtpOutputUrl(String cuteCute) {
		return config.getProperty(PropertiesPath.ftpOutputUrl.format(cuteCute));
	}
	
	public String getFtpInputDir(String cuteCute) {
		return config.getProperty(PropertiesPath.ftpInputDir.format(cuteCute));
	}
	
	public String getFtpOutputDir(String cuteCute) {
		return config.getProperty(PropertiesPath.ftpOutputDir.format(cuteCute));
	}

	public String getFtpInputUser(String cuteCute) {
		return config.getProperty(PropertiesPath.ftpInputUser.format(cuteCute));
	}
	
	public String getFtpOutputUser(String cuteCute) {
		return config.getProperty(PropertiesPath.ftpOutputUser.format(cuteCute));
	}
	
	public String getFtpInputPassword(String cuteCute) {
		String password = config.getProperty(PropertiesPath.ftpInputPassword.format(cuteCute));
		CipherHelper cipher = new CipherHelper(getEncryptionIV(), getEncryptionKEY());
		String decrypterPassword = cipher.decryptData(password);
		return decrypterPassword;
	}
	
	public String getFtpOutputPassword(String cuteCute) {
		String password = config.getProperty(PropertiesPath.ftpOutputPassword.format(cuteCute));
		CipherHelper cipher = new CipherHelper(getEncryptionIV(), getEncryptionKEY());
		String decrypterPassword = cipher.decryptData(password);
		return decrypterPassword;
	}
	
	public String getEncryptionIV() {
		return config.getProperty(PropertiesPath.securityEncriptionIV.format()).trim();
	}	
	
	public String getEncryptionKEY() {
		return config.getProperty(PropertiesPath.securityEncriptionKey.format()).trim();
	}
	
	public String getTipologiaFile(String cuteCute) {
		return config.getProperty(PropertiesPath.tipologiaFile.format(cuteCute));
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
	public void loadSchedeBap(String[] params) {
		for (int i=0;i < params.length; i++ ) {
			String[] p = params[i].split("\\s+");
			if (p[0].equals("END") || p[0].equals("CONFIGPATH") || p[0].equals("CUTECUTE")) {
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
}
