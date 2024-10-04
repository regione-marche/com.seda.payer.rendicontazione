package com.seda.payer.unisciflussi.components;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import com.seda.bap.components.core.BapException;
import com.seda.bap.components.core.spi.ClassPrinting;
import com.seda.bap.components.core.spi.PrintCodes;
import com.seda.commons.properties.PropertiesLoader;
import com.seda.payer.unisciflussi.config.UnisciFlussiContext;
import com.seda.payer.unisciflussi.config.UnisciFlussiResponse;
import com.seda.payer.rendicontazione.util.FTPHelper;
import com.seda.payer.rendicontazione.util.FileFilter;
import com.seda.payer.commons.utility.LogUtility;
import com.seda.payer.rendicontazione.util.ZipUtil;


public class UnisciFlussiCore  {
	private static String myPrintingKey_SYSOUT = "SYSOUT";

	Calendar cal = Calendar.getInstance();
	private UnisciFlussiContext unisciFlussiContext;
	private ClassPrinting classPrinting;
	String jobId;
	//private Logger logger;
	private File inputDirectory ;
	private File outputDirectory ;
	private File progressDirectory;
	private String tipologiaFile;
	private String ftpInputUrl;
	private String ftpInputDir;
	private String ftpInputUser;
	private String ftpInputPassword;
	private String ftpOutputUrl;
	private String ftpOutputDir;
	private String ftpOutputUser;
	private String ftpOutputPassword;
	private String fileConf;
	private String cuteCute;
	private File newZip;
	HashMap<String, ArrayList<File>> filesDaElaborare = new HashMap<String, ArrayList<File>>();

	public UnisciFlussiCore() {
		super();
		welcome();
	}

	public UnisciFlussiResponse run(String[] params, ClassPrinting classPrinting, String jobId) throws BapException {
		UnisciFlussiResponse unisciFlussiResponse = new UnisciFlussiResponse();
		unisciFlussiResponse.setCode("00");
		//inizio LP PG21XX06
		//unisciFlussiResponse.setMessage("");
		unisciFlussiResponse.setMessage("OK");
		//fine LP PG21XX06
		try {
			this.jobId = jobId;
			this.classPrinting = classPrinting;
			//this.logger = null;

			preProcess(params);
			processUnisciFlussi();
			postProcess();

		} catch (Exception e) {
			e.printStackTrace();
			LogUtility.writeLog("Operazione terminata con errori " + e);
			LogUtility.writeLog("=======================================================");
			try {
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Operazione terminata con errori " + e);
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "=======================================================");
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			unisciFlussiResponse.setCode("12");
			//inizio LP PG21X010
			if(jobId.trim().length() == 0) {
				unisciFlussiResponse.setMessage("<BR>UnisciFlussi::run Errore:<BR>    " + e.getMessage());
			} else
			//fine LP PG21X010
			unisciFlussiResponse.setMessage("Operazione terminata con errori");
		}

		return unisciFlussiResponse;
	}

	private void postProcess() {
		//stampaRiepilogo();	//TODO da implementare se serve
	}

	private void welcome() {
		StringBuffer w = new StringBuffer("");    	
		w.append("" +" Unisci Flussi "+ "\n");
		w.append(System.getProperties().get("java.specification.vendor") + " ");
		w.append(System.getProperties().get("java.version") + "\n");  
		w.append("(C) Copyright 2024 Maggioli spa"  + "\n");
		w.append("\n");
		LogUtility.writeLog(w.toString());
		w=null;

		LogUtility.writeLog("=======================================================");
		LogUtility.writeLog("Avvio " + " Unisci Flussi " + "");
		LogUtility.writeLog("=======================================================");
		
		

	}
	public void preProcess(String[] params) throws Exception {
		unisciFlussiContext =  new UnisciFlussiContext();
		unisciFlussiContext.loadSchedeBap(params);
		Properties config = null;

		fileConf = unisciFlussiContext.getParameter("CONFIGPATH");
		cuteCute = unisciFlussiContext.getParameter("CUTECUTE");

		try {
			config = PropertiesLoader.load(fileConf);
		} catch (FileNotFoundException e) {
			LogUtility.writeLog("File di configurazione " + fileConf + " non trovato" );
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "File di configurazione " + fileConf + " non trovato");
			throw new Exception();
		} catch (IOException e) {
			LogUtility.writeLog("Errore file di configurazione " + fileConf + e );
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Errore file di configurazione " + fileConf + e);
			throw new Exception();
		}	
		unisciFlussiContext.setConfig(config);
		if (unisciFlussiContext.getInputDir(cuteCute)==null) {
			LogUtility.writeLog("Cartella Input per Unisci Flussi non configurata"); 
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Cartella Input per Unisci Flussi non configurata"); 
			throw new Exception();
		} else {
			File input = new File(unisciFlussiContext.getInputDir(cuteCute));
			if(!input.exists()) {
				LogUtility.writeLog("Cartella Input per Unisci Flussi non presente");
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Cartella Input per Unisci Flussi non presente");
				throw new Exception();
			}
		}

		if (unisciFlussiContext.getOutputDir(cuteCute)==null) {
			LogUtility.writeLog("Cartella Output per Unisci Flussi non configurata");
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Cartella Output per Unisci Flussi non configurata");
			throw new Exception();
		} else {
			File output = new File(unisciFlussiContext.getOutputDir(cuteCute));
			if(!output.exists()) {
				LogUtility.writeLog("Cartella Output per Unisci Flussi non presente");
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Cartella Output per Unisci Flussi non presente");
				throw new Exception();
			}
		}
		
		if (unisciFlussiContext.getProgressDir(cuteCute)==null) {
			LogUtility.writeLog("Cartella Progress per Unisci Flussi non configurata"); 
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Cartella Progress per Unisci Flussi non configurata");
			throw new Exception();
		} else {
			File progress = new File(unisciFlussiContext.getProgressDir(cuteCute));
			if(!progress.exists()) {
				LogUtility.writeLog("Cartella Progress per Unisci Flussi non presente");
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Cartella Progress per Unisci Flussi non presente");
				throw new Exception();
			}
		}

		inputDirectory = new File( unisciFlussiContext.getInputDir(cuteCute) )  ;
		outputDirectory  =new File( unisciFlussiContext.getOutputDir(cuteCute));
		progressDirectory = new File( unisciFlussiContext.getProgressDir(cuteCute));
				
		ftpInputUrl = unisciFlussiContext.getFtpInputUrl(cuteCute);
		ftpInputDir = unisciFlussiContext.getFtpInputDir(cuteCute);
		ftpInputUser = unisciFlussiContext.getFtpInputUser(cuteCute);
		ftpInputPassword = unisciFlussiContext.getFtpInputPassword(cuteCute);
		ftpOutputUrl = unisciFlussiContext.getFtpOutputUrl(cuteCute);
		ftpOutputDir = unisciFlussiContext.getFtpOutputDir(cuteCute);
		ftpOutputUser = unisciFlussiContext.getFtpOutputUser(cuteCute);
		ftpOutputPassword = unisciFlussiContext.getFtpOutputPassword(cuteCute);
		
		if (ftpInputUrl==null || ftpInputUser==null || ftpInputPassword==null || ftpInputDir == null) {
			LogUtility.writeLog("Parametri FTP di input non configurati correttamente");
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT,"Parametri FTP di input non configurati correttamente"); 
			throw new Exception();
		}
		
		if (ftpOutputUrl==null || ftpOutputUser==null || ftpOutputPassword==null || ftpOutputDir == null) {
			LogUtility.writeLog("Parametri FTP di output non configurati correttamente");
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT,"Parametri FTP di output non configurati correttamente"); 
			throw new Exception();
		}
		
		if (unisciFlussiContext.getTipologiaFile(cuteCute)==null || unisciFlussiContext.getTipologiaFile(cuteCute).trim().equals("")) {
			LogUtility.writeLog("Tipologia File di interesse non configurata"); 
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Tipologia File di interesse non configurata");
			throw new Exception();
		}
		tipologiaFile = unisciFlussiContext.getTipologiaFile(cuteCute);
		
		LogUtility.writeLog("Configurazione esterna caricata da " + fileConf);
		if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Configurazione esterna caricata da " + fileConf);

	}

	private File[] getInputFiles(String inputDirFile) throws Exception {
		File[] inputFiles; 
		File inputDirectory = new File(inputDirFile);
		inputFiles= inputDirectory.listFiles();
		return inputFiles;
	}


	public boolean cancellaFile (File fileorigine){
		boolean ritorno = false;
		try{
			if(fileorigine.exists()){
				if(!fileorigine.delete()){
					LogUtility.writeLog("Il file " + fileorigine + " non può essere eliminato");
					if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Il file " + fileorigine + " non può essere eliminato");
				}else {
					ritorno = true; 
				}						
			}
		}catch(Exception e){
			LogUtility.writeLog("errore durante l'operazione di cancellazione del file " + fileorigine + e);
			try {
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "errore durante l'operazione di cancellazione del file " + fileorigine + e);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		return ritorno;
	}	

	private boolean spostaInOutputDir(File f) throws Exception{
		boolean spostato = false;
		File fileDest = new File(  outputDirectory.getAbsoluteFile() + File.separator +  f.getName() );
		spostato = spostaFile(f, fileDest);
		return spostato;		
	} 
	
	private boolean spostaInProgressDir(File f) throws Exception{
		boolean spostato = false;
		File fileDest = new File(  progressDirectory.getAbsoluteFile() + File.separator +  f.getName() );
		newZip = new File(  progressDirectory.getAbsoluteFile() + File.separator +  f.getName() );
		spostato = spostaFile(f, fileDest);
		return spostato;		
	}
	
	private void copiaInProgressDir(File fileToCopy) throws Exception{
		newZip = new File(  progressDirectory.getAbsoluteFile() + File.separator +  fileToCopy.getName() );
		copyFile(fileToCopy, newZip);
	}
	
	public boolean spostaFile (File pathOrigine, File pathDestinazione){
		boolean ret = false;
		try{
			if(pathDestinazione.exists()){
				if(!pathDestinazione.delete()){
					LogUtility.writeLog("Il file " + pathDestinazione + " non può essere eliminato");
					if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Il file " + pathDestinazione + " non può essere eliminato");
				}
			}
			if(pathOrigine.exists()){
				pathOrigine.renameTo(pathDestinazione);
				ret = true; 
			}

		}catch(Exception e){
			LogUtility.writeLog("errore durante l'operazione di spostamento (input di file) del file " + pathOrigine +" in " + pathDestinazione + e);
			try {
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "errore durante l'operazione di spostamento (input di file) del file " + pathOrigine +" in " + pathDestinazione + e);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		return ret;
	}

	public  void processUnisciFlussi() throws Exception {
		LogUtility.writeLog("=======================================================");
		LogUtility.writeLog("process " + " Unisci Flussi"+ "");
		LogUtility.writeLog("=======================================================");
		if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "=======================================================");
		if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "process " + " Unisci Flussi"+ "");
		if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "=======================================================");
		
		//Estrazione tipologie di file da trattare
		String[] tipologieFile = tipologiaFile.split(",");
		//Elaborazione files per tipologia da trattare
		for (String tipologiaFile : tipologieFile) {
			LogUtility.writeLog("Elaborazione files per tipologia " + tipologiaFile);
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Elaborazione files per tipologia " + tipologiaFile);
			//Sposto i files ZIP della tipologia in elaborazione dalla directory FTP di input alla directory locale di input del processo
			LogUtility.writeLog("Spostamento files di tipologia " + tipologiaFile + " da input FTP a locale");
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Spostamento files di tipologia " + tipologiaFile + " da input FTP a locale");
			spostaFilesFromFTP(inputDirectory.getPath(), tipologiaFile);
			//A questo punto tutti i files della tipologia di interesse sono stati spostati dal FTP di input alla directory locale di input
			//Elaboro i files presenti nella directory locale di input per tipologia e per data
			File [] filesZipInput = getInputFiles(unisciFlussiContext.getInputDir(cuteCute));
			File dirFileProgress = new File(unisciFlussiContext.getProgressDir(cuteCute));
			if (filesZipInput.length>0) {
				filesDaElaborare.clear();
				String dataPrev = "";
				ArrayList<File> filesDaElaborarePerData = null;
				//Predispongo HashMap per gestione file raggruppati per data
				for (File fileZipInput : filesZipInput) {
					if ((fileZipInput.getName().toUpperCase().startsWith(tipologiaFile)) &&
							(fileZipInput.getName().toUpperCase().endsWith(".ZIP"))) {
							String dataCurr = fileZipInput.getName().substring(10,16);
							if (!dataCurr.equals(dataPrev)) {
								if (filesDaElaborarePerData!=null) {
									filesDaElaborare.put(dataPrev, filesDaElaborarePerData);
								}
								dataPrev = dataCurr;
								filesDaElaborarePerData = null;
								filesDaElaborarePerData = new ArrayList<File>();
							}
							filesDaElaborarePerData.add(fileZipInput);
					}
				}
				if (filesDaElaborarePerData!=null) {
					filesDaElaborare.put(dataPrev, filesDaElaborarePerData);
				}
				
				//Elaboro le varie voci della hashmap raggruppate per data
				for (String key: filesDaElaborare.keySet()) {
					//Pulizia directories di progress e output da esiti di precedenti elaborazioni
					if (progressDirectory.isDirectory()) {
						for (File f : progressDirectory.listFiles()) {
			                f.delete();
			            }
					}
					if (outputDirectory.isDirectory()) {
						for (File f : outputDirectory.listFiles()) {
			                f.delete();
			            }
					}
					//Il nome del file nuovo file zip derivante dal raggruppamento dovrà essere quello del primo file zip per la data in esame
					String newFileZipName = filesDaElaborare.get(key).get(0).getName();
					for (File fileZipInputPerData : filesDaElaborare.get(key)) {
						LogUtility.writeLog("Elaborazione file " + fileZipInputPerData.getName());
						if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Elaborazione file " + fileZipInputPerData.getName());
						//inizio LP PG21XX07
						//inserito controllo (ridondante) per verificare esistenza del file:  fileZipInputPerData
						String mess = "Il file da elaborare '" + fileZipInputPerData.getName() + "'";
						if(!fileZipInputPerData.exists()) {
							mess += " NON PRESENTE in: '" + fileZipInputPerData.getParent() + "'";
							LogUtility.writeLog(mess);
							throw new Exception("mess"); 
						} else {
							mess += " presente in: '" + fileZipInputPerData.getParent() + "'";
							LogUtility.writeLog(mess);
						}
						//fine LP PG21XX07
						//Scompatto il file di input nella directory di progress
						copiaInProgressDir(fileZipInputPerData);	//utilizzato copia anzichè sposta per mantenere i files nella directory di input fino ad avvenuta elaborazione
						//inizio LP PG21XX07
						LogUtility.writeLog("dopo copiaInProgressDir file: '" + newZip.getAbsolutePath() + "'");
						//fine LP PG21XX07
						ArrayList<File> fileunzip; 
						fileunzip = ZipUtil.unzipFile(newZip, dirFileProgress);
						if (fileunzip!= null){ 
							cancellaFile(newZip);
						}						
//						if (spostaInProgressDir(fileZipInputPerData)) {
//							ArrayList<File> fileunzip; 
//							fileunzip = ZipUtil.unzipFile(newZip, dirFileProgress);
//							if (fileunzip!= null){ 
//								cancellaFile(newZip);
//							}
//						}
					}
					//Elaboro i files scompattati nella directory di progress
					File[] filesZipTxT = getInputFiles(unisciFlussiContext.getProgressDir(cuteCute));
					//Accorpo tutto il contenuto dei files
					if (filesZipTxT.length>0) {
						BufferedReader reader = null;
						//Creo file txt di output
						File fileOutput = new File(unisciFlussiContext.getOutputDir(cuteCute) + File.separator + filesZipTxT[0].getName());
						BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( fileOutput , true) )  );
						for (File fileZipTxT : filesZipTxT) {
							String line;
							reader = new BufferedReader( new InputStreamReader( new FileInputStream(fileZipTxT)  ) );
							while (  (line = reader.readLine() ) != null ) {
								writer.write(line.concat("\r\n"));
							}
							reader.close();
						}
						writer.close();
						//Creo file zip di output
						File fileZipOutput = new File (unisciFlussiContext.getOutputDir(cuteCute) + File.separator + newFileZipName);
						LogUtility.writeLog("Creazione file " + fileZipOutput.getName());
						if (classPrinting!=null) classPrinting.print("Creazione file " + fileZipOutput.getName());
						ZipUtil.zipFile(fileOutput, fileZipOutput);
						//Cancello file txt di output
						fileOutput.delete();
						//Cancello files txt in progress
						for (File fileZipTxT : filesZipTxT) {
							fileZipTxT.delete();
						}
						//invio file di output tramite FTP
						LogUtility.writeLog("Spostamento file " + fileZipOutput.getName() + " da output locale a FTP");
						if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Spostamento file " + fileZipOutput.getName() + " da output locale a FTP");
						spostaFileToFTP(fileZipOutput);	//TODO da valutare se spostare un file per volta o tutti insieme da output a ftp
						//Pulisco directory di input dai files trattati per tipologia e data
						for (File fileZipInputPerData : filesDaElaborare.get(key)) {
							fileZipInputPerData.delete();	
						}
						
					}
				}
			} else {
				LogUtility.writeLog("Non ci sono files per la tipologia " + tipologiaFile + " nella cartella di Input: " + unisciFlussiContext.getInputDir(cuteCute));
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT,"Non ci sono files per la tipologia " + tipologiaFile + " nella cartella di Input: " + unisciFlussiContext.getInputDir(cuteCute));
			}
		}

	}
	public static String getExtFile(File myfile){

		String fileName = myfile.getName();
		String extension = "";

		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i+1);
		}
		return extension.toLowerCase();
	}
	
	private void stampaRiepilogo() {
		String riga = " ";
		try {
			riga = "==============================================================================================================";
			LogUtility.writeLog(riga);
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, riga,PrintCodes.AFTER_LINE);
			
			riga = "Riepilogo Elaborazione: ";
			LogUtility.writeLog(riga);
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, riga,PrintCodes.AFTER_LINE);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	//Questo metodo sposta tutti i files nella cartella inputDirFile presupponendo che nella cartella FTP 
	//ci siano solo files e non cartelle.
	//Non verifica che nella cartella di destinazione ci siano files con lo stesso nome.
	//Qualora non riesca a spostare un files viene sollevata un'eccezione ed il trasferimento si blocca.
	private void spostaFilesFromFTP(String inputDirFile, String tipologia) throws Exception{
		if (inputDirFile==null) {
			throw new Exception("Directory di input non configurata, trasferimento da FTP non effettuato");
		}
		String directoryPath=unisciFlussiContext.getFtpInputDir(cuteCute);
		boolean success=false; 
		
		// se non è configurata una cartella FTP usciamo con un eccezione	
		if(directoryPath==null){
			throw new Exception("Directory FTP di input non configurata");
		}	
		String ftpInputUrl = unisciFlussiContext.getFtpInputUrl(cuteCute);
		//se non è configurato un FTP url usciamo con un' eccezione
		if(ftpInputUrl==null){
			throw new Exception("Url FTP di input non configurato");
		}	
		String ftpInputUser = unisciFlussiContext.getFtpInputUser(cuteCute);
		//se non è configurato un FTP user usciamo con un' eccezione
		if(ftpInputUser==null){
			throw new Exception("User FTP di input non configurato");
		}
		String ftpInputPassword = unisciFlussiContext.getFtpInputPassword(cuteCute);
		//se non è configurato un FTP password usciamo con un' eccezione
		if(ftpInputPassword==null){
			throw new Exception("Password FTP di input non configurata");
		}
		FTPHelper ftpHelper = new FTPHelper();
		//inizio LP PG21XX07
		FTPClient ftpClient = null;
		try {
			ftpClient = ftpHelper.getFTPClientFromUrl(ftpInputUrl, ftpInputUser, ftpInputPassword);
		//fine LP PG21XX07
		//inizio LP PG21XX07 - 20210707
		LogUtility.writeLog("spostaFilesFromFTP prima di listFiles( '" + directoryPath + "' , ... '" + tipologia + "' ... )");
		//fine LP PG21XX07 - 20210707
		FTPFile[] files = ftpClient.listFiles(directoryPath,new FileFilter(tipologia));
		//inizio LP PG21XX07 - 20210707
		LogUtility.writeLog("spostaFilesFromFTP dopo listFiles: " + ftpClient.getReplyString());
		//fine LP PG21XX07 - 20210707
		if(files==null||files.length<1){
			LogUtility.writeLog("Non ci sono files per la tipologia " + tipologia + " nella cartella FTP di Input: "+directoryPath+ " o si sono verificati problemi di caricamento");
			if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT,"Non ci sono files per la tipologia " + tipologia + " nella cartella FTP di Input: "+directoryPath+ " o si sono verificati problemi di caricamento");
		} else {
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			//inizio LP PG21XX07 - 20210707
			LogUtility.writeLog("spostaFilesFromFTP prima di changeWorkingDirectory( '" + directoryPath + "' )");
			//fine LP PG21XX07 - 20210707
			ftpClient.changeWorkingDirectory(directoryPath);
			//inizio LP PG21XX07 - 20210707
			LogUtility.writeLog("spostaFilesFromFTP dopo changeWorkingDirectory: " + ftpClient.getReplyString());
			//fine LP PG21XX07 - 20210707
			OutputStream outputStream = null;
			for (int k=0; k<files.length; k++) {
				FTPFile ftpFile = files[k];
				//nome del file FTP corrente
				String fileName=ftpFile.getName();
				File downloadFile = new File(inputDirFile,fileName);
				//creazione outputStream da scrivere
				try {
					outputStream=new BufferedOutputStream(new FileOutputStream(downloadFile));
					//inizio LP PG21XX07 - 20210707
					LogUtility.writeLog("spostaFilesFromFTP prima di retrieveFile( '" + fileName + "' , '" +  downloadFile.getAbsolutePath() + "' )");
					//fine LP PG21XX07 - 20210707
					success = ftpClient.retrieveFile(fileName, outputStream);
					//inizio LP PG21XX07 - 20210707
					LogUtility.writeLog("spostaFilesFromFTP dopo retrieveFile: " + ftpClient.getReplyString());
					//fine LP PG21XX07 - 20210707
					//è il codice di risposta del server 
					int x=ftpClient.getReplyCode();
					String[] messages=ftpClient.getReplyStrings();
					if(!success){
						LogUtility.writeLog("Si sono verificati problemi nel trasferimento del file FTP: "+fileName+" nella cartella : "+inputDirFile+" . Server replyCode "+x);
						if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT,"Si sono verificati problemi nel trasferimento del file FTP: "+fileName+" nella cartella : "+inputDirFile+" . Server replyCode "+x);
						for (int i = 0; messages!=null && i < messages.length; i++) {
							LogUtility.writeLog(messages[i]);
							if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT,messages[i]);
						}
						throw new Exception("Si sono verificati problemi nel trasferimento del file FTP: "+fileName+" nella cartella : "+inputDirFile+" . Server replyCode "+x);
					}else{					
						ftpClient.deleteFile(fileName); //sposto e cancello il file dalla directory ftp di input
					}
				} catch (IOException ex) {
					throw ex;
				} finally {
					if (outputStream != null) {
						outputStream.close();
					}
				}
			}
			ftpClient.logout();
			ftpClient.disconnect();	
		}
		//inizio LP PG21XX07
		} catch (Exception e) {
			LogUtility.writeLog("spostaFilesFromFTP Errore: " + e.getMessage());
			throw e;
		}
		finally {
	    	if(ftpClient != null && ftpClient.isConnected()) {
	    	    try {
	    	    	ftpClient.disconnect();
	    	    } catch(IOException ioe) {
					LogUtility.writeLog("spostaFilesFromFTP Errore: Problemi disconnessione FTP");
	    	    }
	    	}
		}
		//fine LP PG21XX07
		
	}
	
	public void inviaFlussoFTP (UnisciFlussiContext ctx,File localFile) throws SQLException{
		// send ftp server
		FTPHelper ftp = new FTPHelper();
		//ftp.Debug = true;		
		if (localFile != null && localFile.exists()) {
			//			boolean successCSV = ftp.uploadCSVFile(ctx, localFile.getAbsolutePath(), "out/" + localFile.getName());
			boolean successCSV = ftp.uploadFile(ctx, cuteCute, localFile.getAbsolutePath(), localFile.getName());
			if (!successCSV) {
				if (classPrinting!=null) classPrinting.print(myPrintingKey_SYSOUT, "Impossibile inviare il file " + localFile.getName());
				LogUtility.writeLog("Impossibile inviare il file " + localFile.getName());
			} else {
				localFile.deleteOnExit();				
			}
		}
	}
	
	public void backupFile(File localFile) throws IOException{
		//backup del file
		if(unisciFlussiContext.getBackupDir(cuteCute)!=null && !unisciFlussiContext.getBackupDir(cuteCute).trim().equals("")){
			File backupDir = new File(unisciFlussiContext.getBackupDir(cuteCute));
			backupDir.mkdirs();
			File backupFile = new File(unisciFlussiContext.getBackupDir(cuteCute).trim() + File.separator + localFile.getName());
			backupFile.createNewFile();
			copyFile(localFile,backupFile);
		}
	}
	
	public static void copyFile(File src, File dst) throws FileNotFoundException, IOException {
	    FileInputStream in = new FileInputStream(src);
	    FileOutputStream out = new FileOutputStream(dst);
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
	
	public void spostaFileToFTP(File localFile) throws IOException{
		//send ftp server
		FTPHelper ftp = new FTPHelper();
		boolean successInvioFtp = true;
		//backup del file di output-inizio
		backupFile(localFile);
		//backup del file di output-fine
		
		if (localFile != null && localFile.exists()) {
			successInvioFtp = ftp.uploadFile(cuteCute, unisciFlussiContext, localFile.getAbsolutePath(), ftpOutputDir + "/" + localFile.getName());
			if (!successInvioFtp) {
				LogUtility.writeLog("Impossibile inviare il file " + localFile.getName());
			} else {
				LogUtility.writeLog("Flusso creato  : " + localFile.getName()); 
				localFile.deleteOnExit();				
			}
		}
		
	}
}

