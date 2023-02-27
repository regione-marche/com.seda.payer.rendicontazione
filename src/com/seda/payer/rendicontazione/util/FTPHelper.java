package com.seda.payer.rendicontazione.util;

import java.io.*;
import java.net.*;
import org.apache.commons.net.ftp.*;

import com.seda.payer.commons.utility.LogUtility;
import com.seda.payer.unisciflussi.config.UnisciFlussiContext;

public class FTPHelper {
	
	public boolean Debug = false;
	
	public FTPHelper() {
	}

	public FTPHelper(boolean debug) {
		this.Debug = debug;
	}
	
	public FTPClient getFTPClientFromUrl(String ftpUrl, String ftpUser, String ftpPassword) {
		
		FTPClient ftpClient = new FTPClient();

		if (Debug) LogUtility.writeLog("FTPClient> ftpUrl='" + ftpUrl + "', ftpUser='" + ftpUser );
		
		//inizio LP PG21XX07
		int timeoutMillis = 300000; //5 min
		//fine LP PG21XX07
		try { 
			//inizio LP PG21XX07
			ftpClient.setDataTimeout(timeoutMillis);
			ftpClient.setConnectTimeout(timeoutMillis);
			ftpClient.setDefaultTimeout(timeoutMillis);
			//fine LP PG21XX07
			//controlliamo se viene indicata una porta e tentiamo una connessione 
			//se il protocollo non è specificato
			if(ftpUrl.contains(":")){
				ftpClient.connect(ftpUrl.split(":")[0],Integer.valueOf(ftpUrl.split(":")[1]));
			}else{
				ftpClient.connect(ftpUrl);
			}
			//inizio LP PG21XX07 - 20210707
			LogUtility.writeLog("FTPClient #1 dopo connect: " + ftpClient.getReplyString());
			ftpClient.enterLocalPassiveMode();
			LogUtility.writeLog("FTPClient #1 eseguito enterLocalPassiveMode: " + ftpClient.getReplyString());
			//fine LP PG21XX07 - 20210707
			ftpClient.login(ftpUser, ftpPassword);
			if (Debug) LogUtility.writeLog(ftpClient.getReplyString().replace("\r\n", ""));
			//inizio LP PG21XX07
			LogUtility.writeLog("FTPClient #1 connection reply string: " + ftpClient.getReplyString());
			ftpClient.setSoTimeout(timeoutMillis);
			//fine LP PG21XX07
		} catch (Exception e) {
			//se la connessione non è riuscita si tenta la connessione con il protocollo
			if (Debug) LogUtility.writeLog("FTPClient #1", e);

			URL url;
			
			try {
				url = new URL(ftpUrl);

				String host = url.getHost();
				int port = url.getPort();
				String path = url.getPath();
				
				//inizio LP PG21XX07
				ftpClient.setDataTimeout(timeoutMillis);
				ftpClient.setConnectTimeout(timeoutMillis);
				ftpClient.setDefaultTimeout(timeoutMillis);
				//fine LP PG21XX07
				ftpClient.connect(host, port == -1 ? 21 : port);
				//inizio LP PG21XX07 - 20210707
				LogUtility.writeLog("FTPClient #2 dopo connect: " + ftpClient.getReplyString());
				ftpClient.enterLocalPassiveMode();
				LogUtility.writeLog("FTPClient #2 eseguito enterLocalPassiveMode: " + ftpClient.getReplyString());
				//fine LP PG21XX07 - 20210707
				ftpClient.login(ftpUser, ftpPassword);
				if (Debug) LogUtility.writeLog(ftpClient.getReplyString().replace("\r\n", ""));
				//inizio LP PG21XX07
				LogUtility.writeLog("FTPClient #2 connection reply string: " + ftpClient.getReplyString());
				ftpClient.setSoTimeout(timeoutMillis);
				//fine LP PG21XX07
				
		    	ftpClient.changeWorkingDirectory("/" +  ftpUser + path);
		    	if (Debug) LogUtility.writeLog(ftpClient.getReplyString().replace("\r\n", ""));
			      
			} catch (Exception ex) {
				LogUtility.writeLog("errore FTP" + ex);
				if (Debug) LogUtility.writeLog("FTPClient #2", ex);
			}
		}

		return ftpClient;
	}
	
	public boolean uploadFile(String cuteCute, UnisciFlussiContext ctx, String localFilePath, String remoteFilePath) {
		
		boolean success = false;
		//inizio LP PG21XX07
		if (Debug) LogUtility.writeLog("uploadFile inizio #1");
		//fine LP PG21XX07
		
		String ftpOutputUrl = ctx.getFtpOutputUrl(cuteCute);
		if (ftpOutputUrl.length() > 0) {
			//inizio LP PG21XX07
		    FTPClient ftpClient = null;
			//fine LP PG21XX07
			String ftpOutputUser = ctx.getFtpOutputUser(cuteCute);
			String ftpOutputPassword = ctx.getFtpOutputPassword(cuteCute);
			try {	 
				//inizio LP PG21XX07
			    //FTPClient ftpClient = getFTPClientFromUrl(ftpOutputUrl, ftpOutputUser, ftpOutputPassword);
			    ftpClient = getFTPClientFromUrl(ftpOutputUrl, ftpOutputUser, ftpOutputPassword);
				//fine LP PG21XX07
				success = uploadFile(ftpClient, localFilePath, remoteFilePath);
			    if (Debug) LogUtility.writeLog(ftpClient.getReplyString().replace("\r\n", ""));
		    	
		    	ftpClient.logout();
		    	ftpClient.disconnect();
		 
		    } catch (Exception e) {
		    	LogUtility.writeLog("FTPHelper.uploadFile " + localFilePath +  " " + remoteFilePath, e);
		    }
			//inizio LP PG21XX07
			finally {
		    	if(ftpClient != null && ftpClient.isConnected()) {
		    	    try {
		    	    	ftpClient.disconnect();
		    	    } catch(IOException ioe) {
				    	LogUtility.writeLog("FTPHelper.uploadFile #1 " + localFilePath +  " " + remoteFilePath + " Errore: Problemi disconnessione FTP");
		    	    }
		    	}
			}
			//fine LP PG21XX07
		}
		//inizio LP PG21XX07
		if (Debug) LogUtility.writeLog("uploadFile fine #1 esito: " + (success ? "OK" : "KO"));
		//fine LP PG21XX07
		return success;
	}

	private boolean uploadFile(FTPClient ftpClient, String localFilePath, String remoteFilePath) throws IOException {
		
		File uploadFile = new File(localFilePath);
		InputStream inputStream = new BufferedInputStream(new FileInputStream(uploadFile));
		try {
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			boolean success = ftpClient.storeFile(remoteFilePath, inputStream);
			if (Debug) LogUtility.writeLog(ftpClient.getReplyString().replace("\r\n", ""));
			return success;
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
	public boolean uploadFile(UnisciFlussiContext ctx, String cuteCute, String localFilePath, String remoteFilePath) {
		
		boolean success = false;
		//inizio LP PG21XX07
		if (Debug) LogUtility.writeLog("uploadFile inizio #2");
		//fine LP PG21XX07
		
		String ftpOutputUrl = ctx.getFtpOutputUrl(cuteCute);
		if (ftpOutputUrl.length() > 0) {
			//inizio LP PG21XX07
		    FTPClient ftpClient = null;
			//fine LP PG21XX07
			String ftpOutputUser = ctx.getFtpOutputUser(cuteCute);
			String ftpOutputPassword = ctx.getFtpOutputPassword(cuteCute);
			try {	 
				//inizio LP PG21XX07
			    //FTPClient ftpClient = getFTPClientFromUrl(ftpOutputUrl, ftpOutputUser, ftpOutputPassword);
			    ftpClient = getFTPClientFromUrl(ftpOutputUrl, ftpOutputUser, ftpOutputPassword);
				//fine LP PG21XX07
				success = uploadFile(ftpClient, localFilePath, remoteFilePath);
			    if (Debug) LogUtility.writeLog(ftpClient.getReplyString().replace("\r\n", ""));
		    	
		    	ftpClient.logout();
		    	ftpClient.disconnect();
		 
		    } catch (Exception e) {
		    	LogUtility.writeLog("FTPHelper.uploadFile " + localFilePath +  " " + remoteFilePath, e);
		    }
			//inizio LP PG21XX07
			finally {
		    	if(ftpClient != null && ftpClient.isConnected()) {
		    	    try {
		    	    	ftpClient.disconnect();
		    	    } catch(IOException ioe) {
				    	LogUtility.writeLog("FTPHelper.uploadFile #2  " + localFilePath +  " " + remoteFilePath + " Errore: Problemi disconnessione FTP");
		    	    }
		    	}
			}
			//fine LP PG21XX07
		}
		//inizio LP PG21XX07
		if (Debug) LogUtility.writeLog("uploadFile fine #2 esito: " + (success ? "OK" : "KO"));
		//fine LP PG21XX07
		return success;
	}
}