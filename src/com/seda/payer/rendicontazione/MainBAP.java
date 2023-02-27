package com.seda.payer.rendicontazione;

import com.seda.bap.components.core.BapException;
import com.seda.bap.components.core.spi.ClassRunnableHandler;
import com.seda.payer.rendicontazione.bap.RendicontazioneBapResponse;

public class MainBAP extends ClassRunnableHandler {

	@Override
	public void run(String[] args) throws BapException {
		
		//Stampa parametri di input - inizio
		for (int i = 0; i < args.length; i++) {
			System.out.println( "argomento[" + i +"] " + args[i] );   
		} 
		//In caso di esecuzione da BAP:
		String[] parameters = getParameters();
		for (int i = 0; i < parameters.length; i++) {
			System.out.println( "param[" + i +"] [" + parameters[i] + "]" );   
		}
		//Stampa parametri di input - fine
		
		RendicontazioneFlussiCore core = new RendicontazioneFlussiCore();
		RendicontazioneBapResponse res = core.run(getParameters(), getDataSource(), getSchema(), printer(), getJobId());
		
		setCode(res != null ? res.getCode() : "");
		setMessage(res != null ? res.getMessage() : "");
	}
}
