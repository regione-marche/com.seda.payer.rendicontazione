package com.seda.payer.acquisisciflussiscarti;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.log4j.PropertyConfigurator;

import com.seda.bap.components.core.BapException;
import com.seda.bap.components.core.spi.ClassPrinting;
import com.seda.bap.components.core.spi.PrintCodes;
import com.seda.commons.properties.PropertiesLoader;
import com.seda.payer.rendicontazione.bap.RendicontazioneBapContext;
import com.seda.payer.rendicontazione.bap.RendicontazioneBapResponse;
import com.seda.payer.commons.utility.LogUtility;

public class AcquisisciFlussiScartiCore  {
	//private Logger logger = Logger.getLogger("FILE"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
	private static String PRINT_SYSOUT = "SYSOUT";

	private RendicontazioneBapContext context;
	private ClassPrinting classPrinting;
	
	String lineSeparator = "============================================================================================";
	//inizio LP PG21X010
	public String messErr = "";
	//fine LP PG21X010

	public AcquisisciFlussiScartiCore() {
		super();
		welcome();
	}

	public RendicontazioneBapResponse run(String[] params, DataSource datasource, String schema, ClassPrinting classPrinting, String jobId) throws BapException { //LP PG22XX10_LP2 - Log inizio e fine operazioni
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScartiCore::run"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		RendicontazioneBapResponse response = new RendicontazioneBapResponse();
		response.setCode("00");
		response.setMessage("");
		try {
			LogUtility.writeLog("login timeout: " + datasource.getLoginTimeout());
			this.classPrinting = classPrinting;
			preProcess(params);
			String dbSchemaCodSocieta = context.getCodiceUtente();
			AcquisisciFlussiScarti proc = new AcquisisciFlussiScarti(dbSchemaCodSocieta, datasource, schema); //inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
			int readTimeOut = 0;
			int ret = proc.elaboraFlussiScarti(readTimeOut);
			if (ret == 0) {
				response.setCode("00");
				response.setMessage("OK");
				postProcess(classPrinting);
	 			printRow(PRINT_SYSOUT, "Elaborazione completata con successo ");
	 			printRow(PRINT_SYSOUT, lineSeparator);
	 			return response;
			} else {
				response.setCode("01");
				response.setMessage("KO - Errore di esecuzione processo");
			}
			//inizio LP PG21X010
			if(jobId.trim().length() == 0) {
				if(ret != 0 && proc.messErr.length() > 0) {
					String mess = response.getMessage();
					mess += "<BR>AcquisisciFlussiScarti::run - Lista errori:" + proc.messErr + "<BR>";
					response.setMessage(mess);
				}
			}
			//fine LP PG21X010
		} catch (Exception e) {
			e.printStackTrace();
			printRow(PRINT_SYSOUT, "Elaborazione completata con errori " + e);
 			printRow(PRINT_SYSOUT, lineSeparator);
 			response.setCode("30");	//TODO da verificare se mantenere 30 come per altri processi oppure impostare 12
 			response.setMessage("Operazione terminata con errori");
			//inizio LP PG21X010
			if(jobId.trim().length() == 0) {
				String mess = response.getMessage();
				mess += "<BR>AcquisisciFlussiScarti::run - Errore:<BR>" + e.getMessage() + "<BR>";
				response.setMessage(mess);
			}
			//fine LP PG21X010
		}
		LogUtility.writeLog("******************************************* fine AcquisisciFlussiScartiCore::run"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		return response;
	}	

	private void postProcess(ClassPrinting classPrinting) {
		printRow(PRINT_SYSOUT, " ");
	}

	private void welcome() {
		StringBuffer w = new StringBuffer("");    	
		w.append("" +"Acquisisci Flussi-Scarti "+ "\n");
		w.append(System.getProperties().get("java.specification.vendor") + " ");
		w.append(System.getProperties().get("java.version") + "\n");  
		w.append("(C) Copyright 2024 Maggioli Spa"  + "\n");
		w.append("\n");
		LogUtility.writeLog(w.toString());
		w=null;

		LogUtility.writeLog(lineSeparator);
		LogUtility.writeLog("Avvio " + "Acquisisci Flussi-Scarti " + "");
		LogUtility.writeLog(lineSeparator);
	}
	
	public void preProcess(String[] params) throws Exception {
		context = new RendicontazioneBapContext();
		context.loadSchedeBap(params);
		Properties config = null;

		String fileConf = context.getParameter("CONFIGPATH");
	
		try {
			config = PropertiesLoader.load(fileConf);
		} catch (FileNotFoundException e) {
			printRow(PRINT_SYSOUT, "File properties di configurazione " + fileConf + " non trovato");
			throw new Exception();
		} catch (IOException e) {
			printRow(PRINT_SYSOUT, "Errore file di configurazione " + fileConf + " " + e);
			throw new Exception();
		}	
		context.setConfig(config);

		PropertyConfigurator.configure(context.getLogger());
		//logger = Logger.getLogger("FILE"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		
		printRow(PRINT_SYSOUT, "Configurazione esterna caricata da " + fileConf);
	}

	public void printRow(String printer, String row) {
		LogUtility.writeLog(row);	
		if (classPrinting!=null)
			try {
				classPrinting.print(printer, row);
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	public void printRow(String printer, String row, PrintCodes printCodes) {
		LogUtility.writeLog(row);	
		if (classPrinting!=null)
			try {
				classPrinting.print(printer, row, printCodes);
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
}