package com.seda.payer.rendicontazione.util;

import java.io.*; 
import java.util.*;
import java.util.zip.*;
  

public class ZipUtil {  
	static final int BUFFER = 2048; 
	  
	  
	    
  @SuppressWarnings("unchecked")
  public static ArrayList<File> unzipFile (File origFile, File destDir) throws ZipException, IOException {
	  		 ArrayList<File> result =  new ArrayList<File>();
	         BufferedOutputStream dest = null;
	         BufferedInputStream is = null;
	         ZipEntry entry;
	         
	         if (!destDir.exists() && !destDir.mkdir()) {
				throw new ZipException("Impossibile creare la directory di destinazione [" + destDir +"] per l'operazione di unzip ");
			 }
	         
	         ZipFile zipfile = new ZipFile(   origFile );
	          
	         Enumeration e = zipfile.entries(); 
	         
	         while(e.hasMoreElements()) {
	        	 
	            entry = (ZipEntry) e.nextElement();
	             
	            is = new BufferedInputStream   ( zipfile.getInputStream(entry) );
	            
	            
	            
	            int count;
	            byte data[] = new byte[BUFFER]; 
	            
	            String fileName;
	            
	            fileName = entry.getName().substring(entry.getName().lastIndexOf("\\") +1);
	            
	            FileOutputStream fos = new   FileOutputStream( destDir +"/" + fileName );
	            
	            dest = new    BufferedOutputStream(fos, BUFFER);
	            
	            while ((count = is.read(data, 0, BUFFER)) != -1) {
	               dest.write(data, 0, count);
	            }
	            
	            dest.flush();
	            dest.close();
	            is.close();
	            
	            result.add(new File( destDir +"/" + fileName  ) );
	         }
	         

	         zipfile.close();
	         
	         return result;
	   }

	  public static void zipFile(File file, File destFile) throws IOException {  
		  ArrayList<File>  list = new ArrayList<File>();
		
		  list.add(file);
		  
		  zipFile(list, destFile);
			  
	  }
  
	   public static void zipFile(List<File> fileList, File destFile) throws IOException {  
	         BufferedInputStream origin = null;
	         
	         FileOutputStream dest = new  FileOutputStream( destFile );
	         
	         ZipOutputStream out = new ZipOutputStream(new  BufferedOutputStream(dest));

	         out.setMethod(ZipOutputStream.DEFLATED);
	         
	         byte data[] = new byte[BUFFER];
	          
	           
	         
	         for (File file : fileList) { 
		            
		            FileInputStream fi = new    FileInputStream( file );
		            origin = new   BufferedInputStream(fi, BUFFER);
		            ZipEntry entry = new ZipEntry( file.getName() );
		            out.putNextEntry(entry);
		            int count;
		            
		            while((count = origin.read(data, 0, 
		              BUFFER)) != -1) {
		               out.write(data, 0, count);
		            }
		            
		            origin.close();
			 }

	         out.close(); 
	   }
	   
	    
  
 
}
