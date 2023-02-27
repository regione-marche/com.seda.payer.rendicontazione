package com.seda.payer.rendicontazione;

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
import com.seda.payer.acquisisciflussiscarti.AcquisisciFlussiScartiCore;
import com.seda.payer.rendicontazione.bap.RendicontazioneBapContext;
import com.seda.payer.rendicontazione.bap.RendicontazioneBapResponse;
import com.seda.payer.commons.utility.LogUtility;

public class RendicontazioneFlussiCore
{
	//private Logger logger = Logger.getLogger(RendicontazioneFlussiCore.class); //LP PG22XX10_LP2 - Log inizio e fine operazioni
	private static String PRINT_SYSOUT = "SYSOUT";

	private RendicontazioneBapContext context;
	private ClassPrinting classPrinting;
	
	String lineSeparator = "============================================================================================";

	public RendicontazioneFlussiCore() {
		super();
		welcome();
	}

	public RendicontazioneBapResponse run(String[] params, DataSource datasource, String schema, ClassPrinting classPrinting, String jobId) throws BapException { //LP PG22XX10_LP2 - Log inizio e fine operazioni
		RendicontazioneBapResponse response = new RendicontazioneBapResponse();
		response.setCode("00");
		response.setMessage("");
		try {
			//inizio LP PG200070 - 20200723
			//LogUtility.writeLog("login timeout: " + datasource.getLoginTimeout());
			//fine LP PG200070 - 20200723
			this.classPrinting = classPrinting;
			preProcess(params);
			//inizio LP PG200070 - 20200723
			LogUtility.writeLog("login timeout: " + datasource.getLoginTimeout());
			//fine LP PG200070 - 20200723
			String dbSchemaCodSocieta = context.getCodiceUtente();
			String sDataSourceName = null;
			RendicontaFlussi proc = new RendicontaFlussi(datasource, schema, dbSchemaCodSocieta, sDataSourceName); //LP PG22XX10_LP2 - Log inizio e fine operazioni
			int ret = proc.start();
			if (ret == 0) {
				response.setCode("00");
				response.setMessage("OK");
				postProcess(classPrinting);
				//inizio LP PG21XX04
				if(jobId.trim().length() == 0) {
					printRow(PRINT_SYSOUT, "Elaborazione Operazione 'Rendicontazione Flussi' completata con successo ");
				} else
				//fine LP PG21XX04
					printRow(PRINT_SYSOUT, "Elaborazione completata con successo ");
	 			printRow(PRINT_SYSOUT, lineSeparator);
	 			return response;
			} else if (ret == 1) {
				response.setCode("01");
				response.setMessage("KO - Errore di esecuzione processo di contabilità");
			} else if (ret == 2) {
				response.setCode("02");
				response.setMessage("KO - Errore di esecuzione processo di rendicontazione");
			} else if (ret == 3) {
				response.setCode("03");
				response.setMessage("KO - Errore di esecuzione processi di rendicontazione e di contabilità");
			} else {
				response.setCode("05");
				response.setMessage("KO - Errore di esecuzione generico");
			}
			//inizio LP PG21X010
			if(jobId.trim().length() == 0) {
				if(ret != 0 && proc.messErr.length() > 0) {
					String mess = response.getMessage();
					mess += "<BR>RendicontaFlussi::run - Lista errori:" + proc.messErr + "<BR>";
					response.setMessage(mess);
				}
			}
			//fine LP PG21X010
			printRow(PRINT_SYSOUT, "Elaborazione completata con errori");
 			printRow(PRINT_SYSOUT, lineSeparator);
		} catch (Exception e) {
			e.printStackTrace();
			printRow(PRINT_SYSOUT, "Elaborazione completata con errori " + e);
 			printRow(PRINT_SYSOUT, lineSeparator);
 			response.setCode("30");	//TODO da verificare se mantenere 30 come per altri processi oppure impostare 12
 			response.setMessage("Operazione terminata con errori ");
			//inizio LP PG21X010
			if(jobId.trim().length() == 0) {
				String mess = response.getMessage();
				mess += "<BR>RendicontaFlussi::run - Errore:<BR>" + e.getMessage() + "<BR>";
				response.setMessage(mess);
			}
			//fine LP PG21X010
		}
		return response;
	}	

	private void postProcess(ClassPrinting classPrinting) {
		printRow(PRINT_SYSOUT, " ");
	}

	private void welcome() {
		StringBuffer w = new StringBuffer("");    	
		w.append("" +"Rendicontazione Flussi "+ "\n");
		w.append(System.getProperties().get("java.specification.vendor") + " ");
		w.append(System.getProperties().get("java.version") + "\n");  
		w.append("(C) Copyright 2022 Maggioli spa"  + "\n");
		w.append("\n");
		LogUtility.writeLog(w.toString());
		w=null;

		//inizio LP PG200070 - 20200723
		//LogUtility.writeLog(lineSeparator);
		//LogUtility.writeLog("Avvio " + "Rendicontazione Flussi " + "");
		//LogUtility.writeLog(lineSeparator);
		//fine LP PG200070 - 20200723
	}
	
	public void preProcess(String[] params) throws Exception {
		context =  new RendicontazioneBapContext();
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
		//logger = Logger.getLogger(RendicontazioneFlussiCore.class);  //LP PG22XX10_LP2 - Log inizio e fine operazioni
		
		//inizio LP PG200070 - 20200723
		printRow(PRINT_SYSOUT, lineSeparator);
		printRow(PRINT_SYSOUT, "Avvio Rendicontazione Flussi");
		printRow(PRINT_SYSOUT, lineSeparator);
		//fine LP PG200070 - 20200723
		printRow(PRINT_SYSOUT, "Configurazione esterna caricata da " + fileConf);
	}

	public void printRow(String printer, String row) {
		LogUtility.writeLog(row);	
		//inizio LP PG200070 - 20200723
		LogUtility.writeLog(row);
		//fine LP PG200070 - 20200723
		if (classPrinting!=null)
			try {
				classPrinting.print(printer, row);
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	public void printRow(String printer, String row, PrintCodes printCodes) {
		LogUtility.writeLog(row);	
		//inizio LP PG200070 - 20200723
		LogUtility.writeLog(row);
		//fine LP PG200070 - 20200723
		if (classPrinting!=null)
			try {
				classPrinting.print(printer, row, printCodes);
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

}