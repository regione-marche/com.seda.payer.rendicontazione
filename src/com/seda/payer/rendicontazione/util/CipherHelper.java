package com.seda.payer.rendicontazione.util;

import com.seda.commons.security.TripleDESChryptoService;

public class CipherHelper {

	private String securityIV = "";
	private String securityKey = "";
	
	public CipherHelper(String securityIV, String securityKey) {
		
		this.securityIV = securityIV;
		this.securityKey = securityKey;
	}
	  
	public String decryptData(String dataCrypted) 
	{
		try
		{
			TripleDESChryptoService desChrypto = new TripleDESChryptoService();
			desChrypto.setIv(securityIV);
			desChrypto.setKeyValue(securityKey);
			return desChrypto.decryptBASE64(dataCrypted);	
			
		} catch (Exception ex) { 
			return ""; 
		}
	}
	
	public String cryptData(String dataToCrypt)
	{
		try
		{
			TripleDESChryptoService desChrypto = new TripleDESChryptoService();
			desChrypto.setIv(securityIV);
			desChrypto.setKeyValue(securityKey);
			return desChrypto.encryptBASE64(dataToCrypt);
			
		} catch (Exception ex) { 
			return ""; 
		}
	}
}