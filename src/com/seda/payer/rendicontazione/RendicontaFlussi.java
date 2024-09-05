package com.seda.payer.rendicontazione;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;

import com.seda.payer.core.dao.FlussiRenDao;
import com.seda.payer.pro.rendicontazione.exception.ProRendicontazioneException;

import com.seda.commons.properties.tree.PropertiesTree;
import com.seda.data.dao.DAOHelper;
import com.seda.payer.acquisisciflussiscarti.config.PrintStrings;
import com.seda.payer.acquisisciflussiscarti.config.PropertiesPath;
import com.seda.payer.core.dao.PagDaRendDao;
import com.seda.payer.facade.dto.PagDaRendDto;
import com.seda.payer.facade.rendicontazione.ContabilitaSupport;
import com.seda.payer.facade.rendicontazione.RendResult;
import com.seda.payer.facade.rendicontazione.RendicontazioneSupport;
import com.seda.payer.pro.rendicontazione.ProRendicontaEnte;
import com.seda.payer.pro.rendicontazione.config.RendicontazionePropertiesPath;
import com.seda.payer.pro.rendicontazione.ws.EMailSender;
import com.seda.payer.rendicontazione.exception.RendicontazioneException;
import com.seda.payer.commons.utility.LogUtility;

public class RendicontaFlussi
{
	//private final String DBSCHEMACODSOCIETA = "dbSchemaCodSocieta";
	private PropertiesTree configuration;
	private DataSource dataSource;
	//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
	//private Logger logger = Logger.getLogger(RendicontaFlussi.class); //LP PG22XX10_LP2 - Log inizio e fine operazioni
	//private Logger logger = null; //LP PG22XX10_LP2 - Log inizio e fine operazioni
	//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
	private String schema;
	private String dbSchemaCodSocieta;
	private ProRendicontaEnte proRendicontaEnte;
	//inizio LP PG21X010
	public String messErr = "";
	//fine LP PG21X010
	
	public RendicontaFlussi(DataSource dataSource, String schema, String dbSchemaCodSocieta, String sDataSourceName) throws Exception  //LP PG22XX10_LP2 - Log inizio e fine operazioni 
	{
		super();
		this.dataSource = dataSource;
		this.schema = schema;
		this.dbSchemaCodSocieta = dbSchemaCodSocieta;
		//this.logger = logger; //LP PG22XX10_LP2 - Log inizio e fine operazioni
		String envRoot = PrintStrings.ROOT.format();
		String rootPath = System.getenv(envRoot);
		if (rootPath==null){
			throw new Exception("Variabile di sistema " + envRoot + " non definita");
		}
		
		LogUtility.writeLog("dbSchemaCodSocieta: " + dbSchemaCodSocieta);
		LogUtility.writeLog("rootPath: " + rootPath);
		try {
			configuration = new PropertiesTree(rootPath);
		} catch (Exception e) {
			throw new Exception("Errore durante la creazione del contesto elaborativo " + e.getMessage(),e);
		}
		String wsEMailSenderUrl = configuration.getProperty(PropertiesPath.wsEMailSenderUrl.format(PropertiesPath.DEFAULT_NODE.format()));
		LogUtility.writeLog("wsEMailSenderUrl: '" + wsEMailSenderUrl + "'");
		EMailSender emailSender = new EMailSender(wsEMailSenderUrl);
		//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
		//if (emailSender == null) {
		//	logger.warn("emailSender == null");
		//}
		//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
		String rendicontazionePath = configuration.getProperty(RendicontazionePropertiesPath.rendicontazioneFilePath.format(dbSchemaCodSocieta));
		LogUtility.writeLog("rendicontazionePath: " + rendicontazionePath);
		if(rendicontazionePath == null) {
			throw new Exception("rendicontazionePath non definita");
		}
		proRendicontaEnte = new ProRendicontaEnte(dataSource, schema, sDataSourceName, configuration, emailSender);
	}
	
	@SuppressWarnings("unused")
	private Connection getConnection(boolean autoCommit) throws SQLException
	{
		Connection conn = null;
		conn = dataSource.getConnection();
		conn.setAutoCommit(autoCommit);
		return conn;
	}

	private Connection getConnection() throws SQLException
	{
		Connection conn = null;
		conn = dataSource.getConnection();
		conn.setAutoCommit(true);
		return conn;
	}
	
	//public int start (int readTimeOut)
	public int start ()
	{
		int exitCode = 0;
		int exitCode1 = 0;
		try 
		{	
			
			/***RENDICONTAZIONE***/
			boolean bResult = generaFlussiRend();
			
			if (bResult) {
				LogUtility.writeLog("Esito Rendicontazione: WS method generaFlussiRend eseguito con successo");
				LogUtility.writeLog("Esito Rendicontazione: 00");
				exitCode = 0;
			} else {
				LogUtility.writeLog("Esito Rendicontazione: WS method generaFlussiRend eseguito con errore");
				LogUtility.writeLog("Esito Rendicontazione: 01");
				exitCode = 1;
			}
			//fine LP PG200070
			
			
			//inizio LP PG200070
			/*
			*CONTABILITA' SEDA*
			GeneraFlussoContabilitaRequest req = new GeneraFlussoContabilitaRequest();
			
			GeneraFlussoContabilitaResponse res = commonsPort.generaFlussoContabilita(req);
			
			if (res != null && res.getResponse() != null)
			{
				LogUtility.writeLog("Esito Contabilità: " + res.getResponse().getRetMessage());
				LogUtility.writeLog("Esito Contabilità: " + res.getResponse().getRetCode());
			
				exitCode1 = Integer.parseInt(res.getResponse().getRetCode().getValue());
			}
			else
			{
				exitCode1 = -2;
			}
			*/
			/***CONTABILITA' SEDA***/
			bResult = generaFlussoContabilita_Seda();

			if (bResult) {
				LogUtility.writeLog("Esito Rendicontazione: WS method generaFlussoContabilita_Seda eseguito con successo");
				LogUtility.writeLog("Esito Rendicontazione: 00");
				exitCode1 = 0;
			} else {
				LogUtility.writeLog("Esito Rendicontazione: generaFlussoContabilita_Seda eseguito con errore");
				LogUtility.writeLog("Esito Rendicontazione: 01");
				exitCode1 = 1;
			}
			//fine LP PG200070
			
			if (exitCode == 0 && exitCode1 == 0)
				return 0;
			else if (exitCode == 0 && exitCode1 != 0)
				return 1;
			else if (exitCode != 0 && exitCode1 == 0)
				return 2;
			else if (exitCode != 0 && exitCode1 != 0)
				return 3;
			
			return exitCode;
		} catch (Exception e) {
			
			e.printStackTrace();
			return -2;
		} 
	}
	
	//inizio LP PG200070
	private boolean generaFlussiRend() throws Exception 
	{
        LogUtility.writeLog("******************************************* inizio RendicontaFlussi.Main::generaFlussiRend"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		boolean bVal = true;
		RendicontazioneSupport spedInstance = new RendicontazioneSupport(dataSource, schema, dbSchemaCodSocieta, configuration); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		try
		{
			//LogUtility.writeLog("Inizio rendicontazione flussi"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
			LogUtility.writeLog("generaFlussiRend - dbSchemaCodSocieta: " + dbSchemaCodSocieta);
			LogUtility.writeLog("generaFlussiRend - Recupero pagamenti da rendicontare");
			PagDaRendDto pagDaRendDto = recuperaPgDaRend(dbSchemaCodSocieta);

			if (pagDaRendDto != null)
			{
				LogUtility.writeLog("generaFlussiRend - Generazione flussi rendicontazione");
				RendResult resultBean = spedInstance.generaFlussoRend(pagDaRendDto, dbSchemaCodSocieta);
				//Vector <String> vRetVal = new  Vector<String>() ;
				bVal &= resultBean.isBResult();	//propagate error if it is
				//inizio LP PG21XX07 - 20210709
				if(!bVal ) {
					String mErr = "generaFlussiRend - generaFlussoRend esito negativo (bVal == false)";
					//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
					//logger.error(mErr);
			        LogUtility.writeLog(mErr);
					//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
					messErr += "<BR>" + mErr;
				}
				//fine LP PG21XX07 - 20210709

				//vRetVal.addAll(spedInstance.generaFlussoRend(pagDaRendDto));
				LogUtility.writeLog("generaFlussiRend - vRetVal.addAll =" + resultBean.getSResult().size());
				for (int i = 0; i < resultBean.getSResult().size(); i++)
				{
					try {
					//inizio LP PG21X010
					//if (proRendicontaEnte.inviaFlussoRend(resultBean.getSResult().get(i),"A", dbSchemaCodSocieta).contains("OK"))
					String statoInvio = proRendicontaEnte.inviaFlussoRend(resultBean.getSResult().get(i),"A", dbSchemaCodSocieta);

					//inizio LP PG21XX07 - 20210707
					boolean erroreInviaOrAggiornaFlagSped = (statoInvio.indexOf("Flusso non inviato per ") != -1 || statoInvio.indexOf(" e non tracciato chiaveRendicontazione ") != -1);
					//fine LP PG21XX07 - 20210707
					if (statoInvio.contains("OK"))
					//fine LP PG21X010
					{
						LogUtility.writeLog("generaFlussiRend - invio corretto del flusso con chiave = " +  resultBean.getSResult().get(i));
					}
					//inizio LP PG21X010
					else if(statoInvio.indexOf("CONFIGURAZIONE_KO") != -1)
					{
						//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
						//logger.error("generaFlussiRend - Errore per assenza della configurazione nell'invio del flusso con chiave = " +  resultBean.getSResult().get(i));
						LogUtility.writeLog("generaFlussiRend - Errore per assenza della configurazione nell'invio del flusso con chiave = " +  resultBean.getSResult().get(i));
						//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
					}
					//fine LP PG21X010
					//inizio LP PG21X010 - 20210628
					//inizio LP PG21XX07 - 20210707
					//else if(statoInvio.indexOf("Ente non abilitato a invio tramite FTP") != -1
					//		|| statoInvio.indexOf("Ente non abilitato a invio tramite EMAIL") != -1
					//		|| statoInvio.indexOf("Ente non abilitato a invio tramite WS") != -1)
					else if(!erroreInviaOrAggiornaFlagSped &&
							(statoInvio.indexOf("Ente non abilitato a invio tramite FTP") != -1
					         || statoInvio.indexOf("Ente non abilitato a invio tramite EMAIL") != -1
					         || statoInvio.indexOf("Ente non abilitato a invio tramite WS") != -1))
					//fine LP PG21XX07 - 20210707
					{
						if(statoInvio.indexOf("Ente non abilitato a invio tramite FTP") != -1) {
							//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
							//logger.error("generaFlussiRend - Errore ente non abilitato a invio tramite FTP del flusso con chiave = " +  resultBean.getSResult().get(i));
					        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore: Errore ente non abilitato a invio tramite FTP del flusso con chiave = " +  resultBean.getSResult().get(i));
							//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
						}
						if(statoInvio.indexOf("Ente non abilitato a invio tramite EMAIL") != -1) {
							//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
							//logger.error("generaFlussiRend - Errore ente non abilitato a invio tramite FTP del flusso con chiave = " +  resultBean.getSResult().get(i));
							//logger.error("generaFlussiRend - Errore ente non abilitato a invio tramite EMAIL del flusso con chiave = " +  resultBean.getSResult().get(i));
					        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore: Errore ente non abilitato a invio tramite EMAIL del flusso con chiave = " +  resultBean.getSResult().get(i));
							//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
						}
						if(statoInvio.indexOf("Ente non abilitato a invio tramite WS") != -1) {
							//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
							//logger.error("generaFlussiRend - Errore ente non abilitato a invio tramite FTP del flusso con chiave = " +  resultBean.getSResult().get(i));
							//logger.error("generaFlussiRend - Errore ente non abilitato a invio tramite WS del flusso con chiave = " +  resultBean.getSResult().get(i));
					        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore: Errore ente non abilitato a invio tramite WS del flusso con chiave = " +  resultBean.getSResult().get(i));
							//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
						}
					}
					//fine LP PG21X010 - 20210628
					 else
					 {
						//logger.error("generaFlussiRend - Errore nell'invio del flusso con chiave = " +  resultBean.getSResult().get(i));
				        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore:  Errore nell'invio del flusso con chiave = " +  resultBean.getSResult().get(i)); //LP PG22XX10_LP2 - Log inizio e fine operazioni
						//inizio LP PG21X010
						messErr += "<BR>generaFlussiRend - Errore nell'invio del flusso con chiave = " +  resultBean.getSResult().get(i);
						//fine LP PG21X010
						bVal = false;
					 }
					
				}catch(Exception ex)
				    {
						//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
						//logger.error("Errore nella rendicontazione flussi: ",ex);
				        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore: " + ex.getMessage());
						//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
						//inizio LP PG21X010
						messErr += "<BR>generaFlussiRend - Errore nella rendicontazione flussi:<BR>    " + ex.getMessage();
						//fine LP PG21X010
						bVal = false;
					}
					
				}
				//Mettere qui nuovo aggiornamento transazioni
				LogUtility.writeLog("aggiornaStatoRendComplex - inizio");
				if(!aggiornaStatoRendComplex()){
					LogUtility.writeLog("inviaFlussoRend - Errore in aggiornaStatoRend");
				}else{
					LogUtility.writeLog("inviaFlussoRend - aggiornaStatoRend ok");
				}
			}
			else
			{
				//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
				//logger.error("Nessuna transazione da rendicontare o impossibile recuperare lista transazioni");
		        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore: Nessuna transazione da rendicontare o impossibile recuperare lista transazioni");
				//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
				//inizio LP PG21X010
				messErr += "<BR>generaFlussiRend - Nessuna transazione da rendicontare o impossibile recuperare lista transazioni";
				//fine LP PG21X010
				bVal = false;
			}
			//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
			//logger.debug("Fine rendicontazione flussi");
	        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend Fine rendicontazione flussi");
			//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
		}
		catch(Exception ex)
		{
			//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
			//logger.error("Errore nella rendicontazione flussi: ",ex);
	        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend errore: " + ex.getMessage());
			//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
			//inizio LP PG21X010
			messErr += "<BR>generaFlussiRend - Errore nella rendicontazione flussi:<BR>    " + ex.getMessage();
			//fine LP PG21X010
			bVal = false;
		}	
        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussiRend esito: " + (bVal ? "true" : "false")); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		return bVal;
	}

	
	private boolean generaFlussoContabilita_Seda() throws Exception 
	{
        LogUtility.writeLog("******************************************* inizio RendicontaFlussi.Main::generaFlussoContabilita_Seda"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		String codiceSocietaSeda = getCodiceSocietaSeda();
		ContabilitaSupport contabilitaSupport = new ContabilitaSupport(dataSource, schema, dbSchemaCodSocieta, codiceSocietaSeda, configuration); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		//inizio LP PG21X010
		//return contabilitaSupport.generaFlussoContabilita();
		boolean statoOp = contabilitaSupport.generaFlussoContabilita();
		if(!statoOp && contabilitaSupport.messErr.length() > 0) {
			messErr += contabilitaSupport.messErr;
			LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussoContabilita_Seda errore: " + contabilitaSupport.messErr); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		}
		else
			LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::generaFlussoContabilita_Seda Esito: OK"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		return statoOp;
		//fine LP PG21X010
	}

	//private PagDaRendDto recuperaPgDaRend(String dbSchemaCodSocieta) throws FacadeException
	private PagDaRendDto recuperaPgDaRend(String dbSchemaCodSocieta) throws RendicontazioneException
	{
        LogUtility.writeLog("******************************************* inizio RendicontaFlussi.Main::recuperaPgDaRend"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		PagDaRendDto pagDaRendDto = null;
		String dataEsecuzione = null;
		/*Calendar cal_dataEsecuzione = Calendar.getInstance();
		String yyyy = String.valueOf(cal_dataEsecuzione.get(Calendar.YEAR));
		String mm = String.valueOf(cal_dataEsecuzione.get(Calendar.MONTH));
		String dd = String.valueOf(cal_dataEsecuzione.get(Calendar.DAY_OF_MONTH));
		mm = ("0" + mm).substring(mm.length()-1);
		dd = ("0" + dd).substring(dd.length()-1);*/
		DateFormat formatter ; 
		Date date ;
		formatter = new SimpleDateFormat("yyyy-MM-dd");
		//date =  Calendar.getInstance().getTime(); 
		date = new Date();
		dataEsecuzione = formatter.format(date);
		//dataEsecuzione = yyyy + "-" + mm + "-" + dd;

		//recupero le tipologie servizio del CUP per le quali creare un flusso di rendicontazione a parte
		String sTipologieServizioCUP = "";
		//if (propertiesTree != null)
		if (configuration != null)
		{
			try
			{
				//sTipologieServizioCUP = propertiesTree.getProperty(PropertiesPath.cupRendicontazioneTipologieServizio.format());
				sTipologieServizioCUP = configuration.getProperty(RendicontazionePropertiesPath.cupRendicontazioneTipologieServizio.format());
			} catch (Exception e) {}
		}
		//inizio LP PG200060
		//String flagRendQuad = BaseFacadeHandler.propertiesTree.getProperty(PropertiesPath.flagRendQuad.format(dbSchemaCodSocieta));
		String flagRendQuad = null;
		if(!(dbSchemaCodSocieta.equals("000LP") || dbSchemaCodSocieta.equals("000RM"))) {
			//flagRendQuad = BaseFacadeHandler.propertiesTree.getProperty(PropertiesPath.flagRendQuad.format(dbSchemaCodSocieta));
			flagRendQuad = configuration.getProperty(RendicontazionePropertiesPath.flagRendQuad.format(dbSchemaCodSocieta));
			if (flagRendQuad == null) flagRendQuad = "N";
		}
		//fine LP PG200060
		
		Connection connection = null;
		try
		{
			//connection = getConnection(dbSchemaCodSocieta);;
			//PagDaRendDao pagDaRendDao = new PagDaRendDao(connection, getSchema(dbSchemaCodSocieta));
			connection = getConnection();
			PagDaRendDao pagDaRendDao = new PagDaRendDao(connection, schema);
			pagDaRendDto = new PagDaRendDto(dataEsecuzione,
					pagDaRendDao.ListaPagDaRend_IV(dataEsecuzione, sTipologieServizioCUP, flagRendQuad),
					pagDaRendDao.ListaPagDaRend_IV_CUP(dataEsecuzione, sTipologieServizioCUP, flagRendQuad),
					pagDaRendDao.ListaPagDaRend_ICI(dataEsecuzione, "N"), //ICI non seda
					pagDaRendDao.ListaPagDaRend_ICI(dataEsecuzione, "Y"), //ICI seda
					pagDaRendDao.ListaPagDaRend_Freccia(dataEsecuzione)) ;
			LogUtility.writeLog("SP eseguita correttamente");
		}
		catch (Exception ex)
		{
			//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
			//logger.error("recuperaPgDaRend, errore dovuto a: ",ex);
	        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::recuperaPgDaRend errore: " + ex.getMessage());
			//fine LP PG22XX10_LP2 - Log inizio e fine operazioni
	        
			throw new RendicontazioneException(ex);
		}
		finally {
			//closeConnection(connection);
			DAOHelper.closeIgnoringException(connection);
		}
        LogUtility.writeLog("******************************************* fine RendicontaFlussi.Main::recuperaPgDaRend"); //LP PG22XX10_LP2 - Log inizio e fine operazioni
		return pagDaRendDto;

	}

	/*
	public String inviaFlussoRend(String chiaveRendicontazione,String tipoInvio, String dbSchemaCodSocieta)
	{
		try {
			InviaFlussoRendRequestType inviaFlussoRequestType = new InviaFlussoRendRequestType();
			inviaFlussoRequestType.setChiaveRendicontazione(chiaveRendicontazione);
			inviaFlussoRequestType.setTipoInvio(tipoInvio);

			RendicontaEnteServicePort lsService = new RendicontaEnteServicePort();
			RendicontaEnteSOAPBinding binding = (RendicontaEnteSOAPBinding)lsService.getRendicontaEntePort(new URL(getWsInviaFlussoRendUrl()));

			setCodSocietaHeaderRendicontaEnte(binding, dbSchemaCodSocieta);

			InviaFlussoRendResponseType response = binding.inviaFlussoRend(inviaFlussoRequestType);

			return(response.getResponse().getRetmessage());


		} catch (RemoteException e) {
			logger.error("inviaFlussoRend, errore dovuto a: ",e);
		} catch (MalformedURLException e) {
			logger.error("inviaFlussoRend, errore dovuto a: ",e);
		} catch (ServiceException e) {
			logger.error("inviaFlussoRend, errore dovuto a: ",e);
		}
		//inizio LP PG1900XX_035
		//return null;
		return "KO";
		//fine LP PG1900XX_035

	}
	public String getWsInviaFlussoRendUrl() {
	  String catalogName = PropertiesPath.baseCatalogName.format();
	  return  configuration.getProperty(PropertiesPath.wsInviaFlussoRendUrl.format(catalogName));
	}


	public void setCodSocietaHeaderRendicontaEnte(RendicontaEnteSOAPBinding stub, String dbSchemaCodSocieta) {
		stub.clearHeaders();
		stub.setHeader("",DBSCHEMACODSOCIETA, dbSchemaCodSocieta);		
	}
	*/

	private String getCodiceSocietaSeda()
	{
		if (configuration != null)
		{
			try
			{
				return configuration.getProperty(RendicontazionePropertiesPath.contabilitaFtpUteGes.format(dbSchemaCodSocieta));
			} catch (Exception e) {}
		}
		return "";
	}
	//fine LP PG200070

	private boolean aggiornaStatoRendComplex() throws ProRendicontazioneException
	{
		Connection connection = null;
		try{
			connection = getConnection();

			FlussiRenDao frDao = new FlussiRenDao(connection, schema);

			int numrighe = frDao.updateEsitoRendicontazioneComplex();
			LogUtility.writeLog("aggiornaStatoRendComplex - righe aggiornate: " + numrighe);

			if( numrighe==-1){
				throw new Exception("aggiornaStatoRendComplex - Si è verificato un errrore durante l'aggiornamento dello stato della rendicontazione");
			}else{
				LogUtility.writeLog("aggiornaStatoRendComplex - Aggiornati " + numrighe + " record");
				return true;
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return false;
	}

}
