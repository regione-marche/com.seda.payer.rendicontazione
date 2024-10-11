/**
 * 
 */
package com.seda.payer.rendicontazione;


import java.text.SimpleDateFormat;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.seda.bap.components.core.spi.ClassPrinting;
import com.seda.commons.properties.PropertiesLoader;
import com.seda.data.dao.DAOHelper;
import com.seda.data.datasource.DataSourceFactoryImpl;
import com.seda.emailsender.webservices.dati.EMailSenderResponse;
import com.seda.payer.pro.rendicontazione.ws.EMailSender;
import com.seda.payer.rendicontazione.bap.RendicontazioneBapResponse;
import com.seda.payer.rendicontazione.util.CipherHelper;
import com.seda.payer.commons.utility.LogUtility;
import com.seda.payer.unisciflussi.components.UnisciFlussiCore;
import com.seda.payer.unisciflussi.config.UnisciFlussiResponse;

/** Avvio da riga comando della procedura.
 * Uso commons-cli-1.2 che è l'ultima versione funzionante su Java6 */
@SuppressWarnings("deprecation")
public class Main {
  String fileConfig;
  String codiceUtente;

  public static void main(String... argv) {
    Options options = new Options();
    options.addOption("f", "config", true, "File di configurazione .properties");
    options.addOption("u", "codiceUtente", true, "Codice Utente (cutecute)");
    options.addOption("h", "help", false, "Istruzioni esplicative");
    CommandLineParser commandLineParser = new BasicParser();
    HelpFormatter formatter = new HelpFormatter();

    boolean printHelp = false;
    Main main = new Main();
    try {
      CommandLine line = commandLineParser.parse(options, argv);
      if (line.hasOption("h"))
        printHelp = true;
      if (line.hasOption("f")) {
        main.fileConfig = line.getOptionValue("f");
      } else {
        printHelp = true;
      }
      if (line.hasOption("u")) {
        main.codiceUtente = line.getOptionValue("u");
      } else {
        printHelp = true;
      }
    } catch (ParseException ex) {
      ex.printStackTrace();
      printHelp = true;
    }

    if (printHelp) {
      formatter.printHelp("RendicontazioneFlussi -u \"XXX\" -f \"path_to_file.properties\"\n"
          + "Rendicontazione Flussi."
          + "Specificare il file di configurazione .properties e il codice utente,"
          + "come da documentazione.", options);
      System.exit(1);
    }
    main.run();
  }

  void run() {
	//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
	//LogUtility.writeLog("inizio");
	LogUtility.writeLog("******************************************* inizio Rendicontazione.Main::run"); 
	//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
    //Logger logger = Logger.getLogger("FILE");
    //LogUtility.writeLog("inizio logger fatto");
	LogUtility.writeLog("Avviato da riga comando");
	LogUtility.writeLog("fileConfig: " + fileConfig);
	LogUtility.writeLog("codiceUtente: " + codiceUtente);
	//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
    System.setProperty("db2.jcc.charsetDecoderEncoder", "3");
    //inizio LP PG21XX06 - Invio email per alert esecuzione KO  
    String urlWSEmailSender = null;
    String emailForAdmin = null;
    //fine LP PG21XX06 - Invio email per alert esecuzione KO  

    try {
      // configurazione connessione DB per dtasource, usa lo stesso file di configurazione BAP e 
      // condivide i parametri di crittografia password 
      Properties datasourceConfig = null;

      // costruzione del datasource, normalmente fornito da BAP
      DataSource datasource = null;
      String schema = null;
      try {
    	LogUtility.writeLog("fileConfig = " + fileConfig);
        datasourceConfig = PropertiesLoader.load(fileConfig);

        String dbDriver = datasourceConfig.getProperty("dbDriver." + this.codiceUtente);
        LogUtility.writeLog("dbDriver = " + dbDriver);
        dbDriver = dbDriver.replaceAll("\\s+","");
        String dbUrl = datasourceConfig.getProperty("dbUrl." + this.codiceUtente);
        LogUtility.writeLog("dbUrl = " + dbUrl);
        String dbUser = datasourceConfig.getProperty("dbUser." + this.codiceUtente);
        LogUtility.writeLog("dbUser = " + dbUser);
        String dbSchema = datasourceConfig.getProperty("dbSchema." + this.codiceUtente);
        LogUtility.writeLog("dbSchema = " + dbSchema);
        String dbPassword = datasourceConfig.getProperty("dbPassword." + this.codiceUtente);
        //inizio LP PG21XX04
        LogUtility.writeLog("dbPassword = " + dbPassword);
        //fine LP PG21XX04
        
        String encryptionIV = datasourceConfig.getProperty("security.encryption.iv");
        String encryptionKey = datasourceConfig.getProperty("security.encryption.key");
        CipherHelper cipher = new CipherHelper(encryptionIV, encryptionKey);
        dbPassword = cipher.decryptData(dbPassword);

        //inizio LP PG21XX04
        //System.out.println("dbPassword = " + dbPassword);
        //fine LP PG21XX04
        Properties dsProperties = new Properties();
        dsProperties.put(DAOHelper.JDBC_DRIVER, dbDriver);
        dsProperties.put(DAOHelper.JDBC_URL, dbUrl);
        dsProperties.put(DAOHelper.JDBC_USER, dbUser);
        dsProperties.put(DAOHelper.JDBC_PASSWORD, dbPassword);
        dsProperties.put("autocommit", "false");

        DataSourceFactoryImpl dataSourceFactory = new DataSourceFactoryImpl();
        dataSourceFactory.setProperties(dsProperties);
        datasource = dataSourceFactory.getDataSource();
        datasource.getConnection().close(); // test connessione per verificare i parametri
        LogUtility.writeLog("Ottenuto datasource DB: " + dbUrl);
        
        schema = dbUser;
        if(dbSchema != null) {
            schema = dbSchema;
        }
        //inizio LP PG21XX06 - Invio email per alert esecuzione KO  
        urlWSEmailSender = datasourceConfig.getProperty("url.emailsender");
        if(urlWSEmailSender != null) {
        	urlWSEmailSender = urlWSEmailSender.trim();
        }
        if(urlWSEmailSender == null || urlWSEmailSender.length() == 0) {
        	LogUtility.writeLog("******************************************* fine Rendicontazione.Main::run Non e' stato valorizzato l'attributo urlWSEmailSender"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
        	throw new Exception("Non è stato valorizzato l'attributo urlWSEmailSender");  
        }
        LogUtility.writeLog("urlWSEmailSender = " + urlWSEmailSender);
        emailForAdmin = datasourceConfig.getProperty("emailForAdmin." + this.codiceUtente);
        if(emailForAdmin != null) {
        	emailForAdmin = emailForAdmin.trim();
        }
        if(emailForAdmin == null || emailForAdmin.length() == 0) {
        	LogUtility.writeLog("******************************************* fine Rendicontazione.Main::run Non e' stato valorizzato l'attributo emailForAdmin"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
        	throw new Exception("Non è stato valorizzato l'attributo emailForAdmin."+ this.codiceUtente);  
        }
        LogUtility.writeLog("emailForAdmin = " + emailForAdmin);
        //fine LP PG21XX06 - Invio email per alert esecuzione KO  
      } catch (Exception e) {
    	  //inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
    	  //LogUtility.writeLog("errore = " + e.getMessage());
      	  LogUtility.writeLog("******************************************* fine Rendicontazione.Main::run errore: " + e.getMessage()); 
    	  //fine LP PG22XX10_LP2 - Log inizio e fine operazioni
    	  e.printStackTrace();
    	  throw new Exception("Errore config datasource DB, File properties: " + fileConfig, e);
      }

      ClassPrinting classPrinting = null;
      String jobId = "";
      String[] params = new String[] {"CONFIGPATH      " + fileConfig, "CUTECUTE      " + codiceUtente};

      RendicontazioneFlussiCore core = new RendicontazioneFlussiCore();
      LogUtility.writeLog("******************************************* inizio Rendicontazione.Main::RendicontazioneFlussiCore"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
      RendicontazioneBapResponse res = core.run(params, datasource, schema, classPrinting, jobId); //LP PG22XX10_LP2 - Log inizio e fine operazioni
      if (!res.getCode().equals("00")) {
      	LogUtility.writeLog("******************************************* fine Rendicontazione.Main::RendicontazioneFlussiCore Comando Rendicontazione Flussi eseguito con esito negativo:"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
        //inizio LP PG21XX04
        //throw new Exception("Comando eseguito con esito negativo: " + res.getMessage());
        throw new Exception("Comando Rendicontazione Flussi eseguito con esito negativo: " + res.getMessage());
        //fine LP PG21XX04
      }
      LogUtility.writeLog("******************************************* fine Rendicontazione.Main::RendicontazioneFlussiCore Comando Rendicontazione Flussi eseguito con esito positivo: " + res.getMessage().replace("<BR>", "\r\n")); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
      //inizio LP PG21XX04
      //logger.info("Comando eseguito con esito positivo: " + res.getMessage());
      //inizio LP PG21X010
      //logger.info("Comando Rendicontazione Flussi eseguito con esito positivo: " + res.getMessage());
      LogUtility.writeLog("Comando Rendicontazione Flussi eseguito con esito positivo: " + res.getMessage().replace("<BR>", "\r\n"));
      //fine LP PG21X010
      //16062021 GG- Escludo unisci flussi da RM che non lo utilizza e non ha le relative configurazioni
      if (!codiceUtente.equalsIgnoreCase("000RM")) {
    	  LogUtility.writeLog("******************************************* inizio Rendicontazione.Main::UnisciFlussiCore"); 
    	  LogUtility.writeLog("Inizio Unisci Flussi");
	      UnisciFlussiCore unisciFlussiCore= new UnisciFlussiCore();
	      UnisciFlussiResponse resUF = null; 
	      resUF = unisciFlussiCore.run(params, classPrinting, jobId);
	      if (!resUF.getCode().equals("00")) {
	    	  LogUtility.writeLog("******************************************* fine Rendicontazione.Main::UnisciFlussiCore Comando Unisci Flussi eseguito con esito negativo: " + resUF.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
	          //inizio LP PG21XX06
	          //throw new Exception("Comando Unisci Flussi eseguito con esito negativo: " + res.getMessage());
	          throw new Exception("Comando Unisci Flussi eseguito con esito negativo: " + resUF.getMessage());
	          //fine LP PG21XX06 - Invio email per alert esecuzione KO
	      }
    	  LogUtility.writeLog("******************************************* fine Rendicontazione.Main::UnisciFlussiCore Comando Unisci Flussi eseguito con esito positivo: " + resUF.getMessage().replace("<BR>", "\r\n")); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
	      //inizio LP PG21XX06
	      //logger.info("Comando Unisci Flussi eseguito con esito positivo: " + res.getMessage());
	      //inizio LP PG21X010
	      //logger.info("Comando Unisci Flussi eseguito con esito positivo: " + resUF.getMessage());
    	  LogUtility.writeLog("Comando Unisci Flussi eseguito con esito positivo: " + resUF.getMessage().replace("<BR>", "\r\n"));
	      //fine LP PG21X010
	      //fine LP PG21XX06
    	  LogUtility.writeLog("Fine Unisci Flussi");
    	  LogUtility.writeLog("******************************************* fine Rendicontazione.Main::UnisciFlussiCore"); 
      }
      //fine LP PG21XX04
    } catch (Exception ex) {
      //inizio LP PG21X010
      //logger.error(ex);
      LogUtility.writeLog(ex.getMessage().replace("<BR>", "\r\n"));
      //fine LP PG21X010
      //inizio LP PG21XX06 - Invio email per alert esecuzione KO
      if(urlWSEmailSender != null && emailForAdmin != null) {
    	  inviaEmailAlert(urlWSEmailSender, emailForAdmin, ex.getMessage());
      }
      //fine LP PG21XX06 - Invio email per alert esecuzione KO
  	  LogUtility.writeLog("******************************************* fine Rendicontazione.Main::run errore: " + ex.getMessage().replace("<BR>", "\r\n")); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
      System.exit(-1);
    }
	LogUtility.writeLog("******************************************* fine Rendicontazione.Main::run"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
  }
  //inizio LP PG21XX06 - Invio email per alert esecuzione KO  
  private void inviaEmailAlert(String urlWSEmailSender, String emailForAdmin, String mess)
  {
	String eMailDataTOList = emailForAdmin;
	String eMailDataCCList = "";
	String eMailDataCCNList = "";
	String eMailDataSubject = "";
	String eMailDataAttacchedFileList = "";
	String eMailDataText = "";
	SimpleDateFormat formatDateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");

	EMailSender emailSender = new EMailSender(urlWSEmailSender);

	//inizio LP PG21X010  
	//eMailDataSubject = " -  Rendicontazione " + formatDate.format(new java.sql.Date(System.currentTimeMillis())) +  " - KO";	
	//eMailDataText = " - Rendicontazione <BR>  Data elaborazione: " + formatDateTime.format(new java.sql.Date(System.currentTimeMillis()));
	eMailDataSubject = " - Rendicontazione " + codiceUtente + " " + formatDate.format(new java.sql.Date(System.currentTimeMillis())) +  " - KO";	
	eMailDataText = " - Rendicontazione <b>" + codiceUtente + "</b><BR>  Data elaborazione: " + formatDateTime.format(new java.sql.Date(System.currentTimeMillis()));
	//fine LP PG21X010  
	String chi = "Pagamenti On-Line";
	if(codiceUtente.equals("000LP") || codiceUtente.equals("000RM")) {
		chi = "MPAY";
	}
	eMailDataSubject = chi + eMailDataSubject; 	
	eMailDataText = chi + eMailDataText;
	String testoMail = "<BR>  Esito processo:<BR>  ";
	testoMail += mess.replace("KO - ", "");
	eMailDataText += testoMail.replace("è", "e'").replace("à", "a'");
	LogUtility.writeLog("Oggetto:");
	LogUtility.writeLog(eMailDataSubject);
	LogUtility.writeLog("Testo:");
	LogUtility.writeLog(eMailDataText);
	
	EMailSenderResponse emailSenderResponse = null;
	LogUtility.writeLog("sendMail - pre emailSender.sendEMail");
	try {
		emailSenderResponse = emailSender.sendEMail(eMailDataTOList, eMailDataCCList, eMailDataCCNList, eMailDataSubject, eMailDataText, eMailDataAttacchedFileList);
	} catch (Exception e) {
		e.printStackTrace();
		LogUtility.writeLog("sendMail - emailSender.sendEMail exception " + e.getMessage() + " " + e.getCause());
	}
	LogUtility.writeLog("sendMail - post emailSender.sendEMail");
	if(emailSenderResponse != null) {
		LogUtility.writeLog("sendMail - esito: " + emailSenderResponse.getValue());
	}
	return;
  }
  //fine LP PG21XX06 - Invio email per alert esecuzione KO  
}
