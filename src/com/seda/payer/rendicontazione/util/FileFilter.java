package com.seda.payer.rendicontazione.util;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FileFilter implements FTPFileFilter {
	
	String tipologia;
	
	public FileFilter(String tipologia) {
		super();
		this.tipologia = tipologia;
	}

	public boolean accept(FTPFile ftpFile) {
		return ftpFile.getName().toUpperCase().startsWith(tipologia);
	}
	
}
