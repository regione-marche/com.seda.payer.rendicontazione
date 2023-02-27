package com.seda.payer.acquisisciflussiscarti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.seda.commons.properties.tree.PropertiesTree;
import com.seda.data.dao.DAOHelper;
import com.seda.payer.acquisisciflussiscarti.config.PrintStrings;
import com.seda.payer.acquisisciflussiscarti.config.PropertiesPath;
import com.seda.payer.commons.utility.FileUtility;
import com.seda.payer.core.dao.PagamentiScartatiDao;
import com.seda.payer.facade.dto.ScartoDto;
import com.seda.payer.notifiche.webservice.dati.NotificaScartoRequestType;
import com.seda.payer.notifiche.webservice.dati.NotificaScartoResponseType;
import com.seda.payer.notifiche.webservice.source.NotificheSOAPBindingStub;
import com.seda.payer.pgec.webservice.commons.dati.ChiaveScarto;
import com.seda.payer.commons.utility.LogUtility;


public class AcquisisciFlussiScarti {
	
	private final String DBSCHEMACODSOCIETA = "dbSchemaCodSocieta";
	private PropertiesTree configuration;
	private String dbSchemaCodSocieta;
	private DataSource dataSource;	
	private String schema;
	//private Logger logger = Logger.getLogger("FILE");//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
	//inizio LP PG21X010
	public String messErr = "";
	//fine LP PG21X010

	public AcquisisciFlussiScarti(String dbSchemaCodSocieta, DataSource dataSource,  String schema) throws Exception //inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
	{
		super();
		this.dbSchemaCodSocieta = dbSchemaCodSocieta;
		this.dataSource = dataSource;
		this.schema = schema;
		//this.logger = logger;//inizio LP PG22XX10_LP2 - Log inizio e fine operazioni
		
		String envRoot = PrintStrings.ROOT.format();
		String rootPath = System.getenv(envRoot);
		if (rootPath==null){
			throw new Exception("Variabile di sistema " + envRoot + " non definita");
		}
		
		try {
			configuration = new PropertiesTree(rootPath);
		} catch (Exception e) {
			throw new Exception("Errore durante la creazione del contesto elaborativo " + e.getMessage(),e);
		}
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
	
	/**
	 * Richiama il metodo <code><b>elaboraFlussiScarti</b></code> del servizio com.seda.payer.pgec.webservice
	 * <br>
	 * @return
	 * @throws Exception
	 */
	public int elaboraFlussiScarti(int readTimeOut) throws Exception
	{
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::elaboraFlussiScarti(" + readTimeOut + ")"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		int exitCode=0;
		try {
			/*
						
			String commonsWsUrl = configuration.getProperty(PropertiesPath.wsCommonsUrl.format(PropertiesPath.baseCatalogName.format()));
					
			CommonsServiceLocator serviceLocator = new CommonsServiceLocator();
			CommonsSOAPBindingStub commons = (CommonsSOAPBindingStub)serviceLocator.getCommonsPort(new URL(commonsWsUrl));
			if (readTimeOut > 0)
				commons.setTimeout(readTimeOut);
			
			commons.clearHeaders();
			commons.setHeader("", DBSCHEMACODSOCIETA,dbSchemaCodSocieta);	
			
			ElaboraFlussiScartiResponse response = commons.elaboraFlussiScarti();
			
			
			if ( response != null)
			{
				System.err.println(response.getResponse().getRetMessage());
				System.err.println(response.getResponse().getRetCode());
			
				exitCode = Integer.parseInt(response.getResponse().getRetCode().getValue());
			}
			else
			{
				exitCode = -2;
			}
			*/
			boolean bResult = elaboraFlussiScarti(); 
			
			if (bResult) {
				LogUtility.writeLog("Esito ElaboraFlussiScarti eseguita con successo");
				LogUtility.writeLog("Esito ElaboraFlussiScarti: 00");
				exitCode = 0;
			} else {
				LogUtility.writeLog("Esito ElaboraFlussiScarti eseguita con errore");
				LogUtility.writeLog("Esito ElaboraFlussiScarti: 01");
				exitCode = 1;
			}
			//fine LP PG200070
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::elaboraFlussiScarti(" + readTimeOut + ")"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			return exitCode;
			
		}  catch (Exception e) {
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::elaboraFlussiScarti(" + readTimeOut + ") errore: " + e.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			throw e;
		}
	}
	
	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	//verifica se si puo' cancelalre l'originale
	/**
	 * Verrà richiamato per l'elaborazione dei flussi con i pagamanenti scartati
	 * dai sistemi dell'ente. <br>
	 * <br>
	 * Per ogni flusso fisico presente in un determinato path configurabile da
	 * file .properties richiama nell'ordine i seguenti metodi:
	 * <ul>
	 * <li><code><b>importaFlussoScarti</b></code></li>
	 * <li><code><b>assegnaFlussoScarti</b></code></li>
	 * <li><code><b>notificaScarto</b></code></li>
	 * <li><code><b>aggiornaFlussoScarti</b></code></li>
	 * </ul>
	 * Il flusso, terminata l'importazione nel DB, verrà spostato (storicizzato)
	 * in un altro path configurabile sempre da file .properties. <br>
	 * Avrà come parametri di output un codice esito operazione
	 * <code>retCode</code> e un messaggio di ritorno relativo al codice esito
	 * <code>retMessage</code>. <br>
	 * 
	 * @see com.seda.payer.pgec.webservice.commons.source.CommonsInterface#elaboraFlussiScarti()
	 */
	public boolean elaboraFlussiScarti() throws RemoteException {
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::elaboraFlussiScarti"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		boolean elaborated = false;
		String pathIniziale = configuration.getProperty(PropertiesPath.REJECT_PATH.format(PropertiesPath.DEFAULT_NODE.format()));
		String pathElab = configuration.getProperty(PropertiesPath.REJECT_IN_PROGRESS_PATH.format(PropertiesPath.DEFAULT_NODE.format()));
		String processedPath = configuration.getProperty(PropertiesPath.REJECT_PROCESSED_PATH.format(PropertiesPath.DEFAULT_NODE.format()));
		String rejectedPath = configuration.getProperty(PropertiesPath.REJECT_REJECTED_PATH.format(PropertiesPath.DEFAULT_NODE.format()));
		try {
			/*
			CommonsFacadeRemoteHome serviceRemoteHomeFacade = (CommonsFacadeRemoteHome) ServiceLocator.getInstance().getRemoteHome(env.getProperty(Context.PROVIDER_URL), env.getProperty(Context.INITIAL_CONTEXT_FACTORY), null, null, CommonsFacadeRemoteHome.JNDI_NAME, CommonsFacadeRemoteHome.class);
			CommonsFacade facade = serviceRemoteHomeFacade.create();
			*/
			// we find zip file flow
			TreeSet treeSet = FileUtility.find(pathIniziale, Pattern.compile("^(.*?)\\.(zip)$"), FileUtility.ORDER_ASC);
			if (treeSet != null && !treeSet.isEmpty()) {
				// Phase I - we import & store flow
				LogUtility.writeLog("Phase I - start import & backup flow file");
				Iterator iter = treeSet.iterator();
				while (iter.hasNext()) {
					File f = (File) iter.next();
					String flowFileName = fileInProgress(pathIniziale, pathElab, f);
					LogUtility.writeLog("flow phisical file name - " + flowFileName);
					List<ChiaveScarto> response = importaFlussoScarti(flowFileName, f.getName().substring(0, f.getName().indexOf(".")));
					LogUtility.writeLog("list flow - " + response);
					if (response != null && response.size() > 0)
						backupFile(pathIniziale, processedPath, pathElab, f.getName().substring(0, f.getName().indexOf(".")));
					else
						backupFile(pathIniziale, rejectedPath, pathElab, f.getName().substring(0, f.getName().indexOf(".")));
				}
			} else
				LogUtility.writeLog("Phase I - import & backup flow file not found");
			// Phase II - we assign, notify & mark flow processed
			LogUtility.writeLog("Phase II - start assign, notify & mark flow");
			boolean responseAssign = assegnaFlussoScarti();
			//LogUtility.writeLog("assegnaFlussiScarti failed - "+ responseAssign.getRetMessage());
			if (!responseAssign)
				LogUtility.writeLog("assegnaFlussiScarti failed");
			else
				elaborated = true;
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::elaboraFlussiScarti: " + (elaborated ? "true" : "false")); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		} catch (Exception e) {
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::elaboraFlussiScarti errore: " + e.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			LogUtility.writeLog("elaboraFlussiScarti failed, generic error due to: ", e);
			//inizio LP PG21X010
			messErr += "<BR>elaboraFlussiScarti - Errore:<BR>    " + e.getMessage();
			//fine LP PG21X010
		}
		return elaborated;
	}
	
	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	/**
	 * Esegue unzip di un file presente in una determinata directory, lo
	 * rinomina in .ele, e resituisce il nome del file da processare. <br>
	 * 
	 * @param pathInziale
	 * @param pathElab
	 * @param f
	 */
	private String fileInProgress(String pathIniziale, String pathElab, File f)
			throws Exception {
		LogUtility.writeLog("unzip file " + pathIniziale + f.getName() + " to dir " + pathElab);
		com.seda.payer.commons.utility.GZiper.unzip(pathIniziale + f.getName(), pathElab);
		LogUtility.writeLog("unzip file " + pathIniziale + f.getName() + " to dir " + pathElab
				+ " succeful");
		String nomeFile = f.getName().substring(0, f.getName().indexOf("."));
		LogUtility.writeLog("phisical file name " + nomeFile);
		LogUtility.writeLog("rename zip file " + (pathIniziale + f.getName())
				+ " to ele file " + pathIniziale + nomeFile + ".ele");
		boolean rename = new File(pathIniziale + f.getName()).renameTo(new File(pathIniziale
				+ nomeFile + ".ele"));
		LogUtility.writeLog("rename zip file ... to ele file ... result - " + rename);
		return pathElab + nomeFile + ".txt";
	}
	
	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	/**
	 * Rinomina il file da .ele, cioè file in elaborazione, a .zip e lo sposta
	 * in una directory specifica <br>
	 * 
	 * @param pathInziale
	 * @param pathStrorico
	 * @param pathTemporale
	 * @param nomeFile
	 */
	private void backupFile(String pathInziale, String targetPath,
			String pathTemporale, String nomeFile) {
		File f = new File(pathInziale + nomeFile + ".zip");
		LogUtility.writeLog("backup file name - " + f.getAbsolutePath());
		boolean rename = new File(pathInziale + nomeFile + ".ele").renameTo(f);
		LogUtility.writeLog("renamed backup file name " + f.getAbsolutePath() + " to "
				+ pathInziale + nomeFile + ".ele - " + rename);
		boolean move = f.renameTo(new File(targetPath + nomeFile + ".zip"));
		LogUtility.writeLog("move file " + f.getAbsolutePath() + " to " + targetPath
				+ nomeFile + ".zip - " + move);
		boolean delete = new File(pathTemporale + nomeFile + ".txt").delete();
		LogUtility.writeLog("delete file " + pathTemporale + nomeFile + ".txt - " + delete);
	}

	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	/**
	 * Avrà il compito di importare un determinato flusso di scarti nella
	 * tabella PYSCATB. <br>
	 * <br>
	 * Al termine dell'import dovrà essere storicizzato il file. <br>
	 * Avrà come parametro di input il parametro <code>pathFileName</code>
	 * contenente il path completo comprensivo del nome del file di scarto, il
	 * parametro <code>nomeFile</code> ed il parametro <code>facade</code>. <br>
	 * Avrà come parametri di output una lista di valori ChiaveScarto
	 * <code>List<ChiaveScarto></code> contente tutti gli id dei record inseriti
	 * nella tabella PYSCATB. <br>
	 * 
	 * @param fileName
	 *            java.lang.String
	 * @throws Exception
	 */
	public List<ChiaveScarto> importaFlussoScarti(String pathFileName, String nomeFile) throws Exception {
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::importaFlussoScarti"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		List<ChiaveScarto> output = new ArrayList<ChiaveScarto>();
		LogUtility.writeLog("start importaFlussoScarti");
		try {
			int chiaveScarto = 0;
			FileInputStream fis = new FileInputStream(pathFileName);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			String linea = br.readLine();
			while (linea != null) {
				String tipologiaFlusso = nomeFile.substring(2, 5);
				if (tipologiaFlusso.equals("ICI") || tipologiaFlusso.equals("ISC")) {
					int tipoRecord = Integer.parseInt(linea.substring(25, 26));
					// we store in temp table record type
					// "Record riscossione contabile"
					if (tipoRecord == 3) {
						chiaveScarto = savePagamentiScartati(-1, nomeFile.substring(2, 5), nomeFile, linea, " ", 'N');
						LogUtility.writeLog("saved chiaveScarto - " + chiaveScarto
								+ ", flow type ICI or ISC (type " + tipoRecord
								+ ")");
						output.add(new ChiaveScarto(chiaveScarto, tipoRecord));
					} else if (tipoRecord == 4 || tipoRecord == 5) {
						// we save record type
						// "Record anagrafica persone fisiche" or
						// "Record anagrafica persone giuridiche"
						savePagamentiScartati(chiaveScarto, null, null, null, linea, 'N');
						LogUtility.writeLog("saved chiaveScarto - " + chiaveScarto
								+ ", flow type ICI or ISC (type " + tipoRecord
								+ ")");
					}
				} else {
					LogUtility.writeLog("saved chiaveScarto - " + chiaveScarto
							+ ", flow type base");
					chiaveScarto = savePagamentiScartati(-1, nomeFile.substring(2, 5), nomeFile, linea, " ", 'N');
					output.add(new ChiaveScarto(chiaveScarto, 3));
				}
				linea = br.readLine();
			}
			br.close();
			isr.close();
			fis.close();
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::importaFlussoScarti"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		} catch (Exception e) {
			e.printStackTrace();
			LogUtility.writeLog("importaFlussiScarti failed, generic error due to: ", e);
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::importaFlussoScarti errore: " + e.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		}
		return output;
	}

	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	/**
	 * Avrà il compito di associare ad ogni pagamento presente nel flusso di
	 * scarti e precedentemente importato nella tabella PYSCATB con il relativo
	 * pagamento presente nelle tabelle PYTDTTB, PYTICTB e PYTFRTB. <br>
	 * <br>
	 * L'assegnazione viene fatta partendo dalla tabella PYSCATB per tutti i
	 * record che hanno il campo SCA_FSCAELAB='N' (pagamento non elaborato), il
	 * relativo valore del campo SCA_PSCAPKEY dovrà essere utilizzato per
	 * aggiornare i campi TDT_PSCAPKEY, TIC_PSCAPKEY e TFR_PSCAPKEY delle
	 * tabelle di dettaglio transazione in base alla tipologia del flusso:
	 * <ul>
	 * <li>ICI</li>
	 * <li>ISC</li>
	 * <li>IVP</li>
	 * <li>IVS</li>
	 * <li>IVM</li>
	 * <li>IVB</li>
	 * <li>IVC</li>
	 * <li>IVF</li>
	 * </ul>
	 * <br>
	 * Avrà come parametro di input il parametro
	 * <code>facade<code>, il parametro <code>tipologiaFlusso<code>,
	 * ed il parametro <code>chiaviScarto<code>.
	 * Avrà come parametri di output un codice esito operazione <code>retCode</code>
	 * e un messaggio di ritorno relativo al codice esito
	 * <code>retMessage</code>. <br>
	 * 
	 * @throws Exception
	 */
	private boolean assegnaFlussoScarti() throws Exception {
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::assegnaFlussoScarti"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		try {
			
			// we assign
			LogUtility.writeLog("Phase II - start assign flow");
			List<ScartoDto> out = assignPagamentiScartati();
			LogUtility.writeLog("Phase II - list assigned - " + out);
			// we notify & mark
			LogUtility.writeLog("Phase II - start notify & mark");
			boolean response = notificationProcessing(out);
			//warn("notificationProcessing failed - "+ response.getRetMessage());
			if (!response) 
				LogUtility.writeLog("notificationProcessing failed");
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::assegnaFlussoScarti: true"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			LogUtility.writeLog("assegnaFlussiScarti failed, generic error due to: ", e);
			//inizio LP PG21X010
			messErr += "<BR>assegnaFlussoScarti - Errore:<BR>    " + e.getMessage();
			//fine LP PG21X010
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::assegnaFlussoScarti: false"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			return false;
		}
	}

	// com.seda.payer.facade\ejbModule\com\seda\payer\facade\bean\CommonsFacadeBean.java
	public List<ScartoDto> assignPagamentiScartati() throws Exception {
		Connection connection = null;
		try { 
			connection = getConnection();
			PagamentiScartatiDao dao = new PagamentiScartatiDao(connection, schema);
			List<String[]> scarti = dao.doAssignPagamentiScartati();
			if (scarti != null) {
				List<ScartoDto> output = new ArrayList<ScartoDto>();
				for (int i = 0; i < scarti.size(); i++) {
					output.add(new ScartoDto(scarti.get(i)));
				}
				return output;
			}
			return null;
		} catch (Exception ex) {
			LogUtility.writeLog("assignPagamentiScartati failed, generic error due to: ", ex);
			throw new Exception(ex);
		} finally {
			//connection.close();
			DAOHelper.closeIgnoringException(connection);
		}	
	}
	
	// com.seda.payer.facade\ejbModule\com\seda\payer\facade\bean\CommonsFacadeBean.java
	public int savePagamentiScartati(int progKey, String tipoFlusso, String nomeFile, String recordScartato, String tipologiaRecord, char stato) throws Exception
	{
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::savePagamentiScartati"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		Connection connection = null;
		try 
		{ 
			PagamentiScartatiDao dao = null;
			connection = getConnection();
			dao = new PagamentiScartatiDao(connection, schema);
			if(progKey <= 0) { 
				return dao.doInsertPagamentiScartati(tipoFlusso, nomeFile, recordScartato, tipologiaRecord, stato);
			} else {
				return dao.doUpdatePagamentiScartati(tipoFlusso, nomeFile, recordScartato, tipologiaRecord, stato, progKey);
			}	
		} catch (Exception ex) {
			LogUtility.writeLog("savePagamentiScartati failed, generic error due to: ", ex);
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::savePagamentiScartati errore: " + ex.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			throw new Exception(ex);
		}
		finally {
			//connection.close();
			DAOHelper.closeIgnoringException(connection);
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::savePagamentiScartati"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		}	
	}
	
	// com.seda.payer.facade\ejbModule\com\seda\payer\facade\bean\CommonsFacadeBean.java
	public int removePagamentiScartati(String nomeFile, char stato, String dbSchemaCodSocieta) throws Exception
	{
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::removePagamentiScartati"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		Connection connection = null;
		try { 
			connection = getConnection();
			PagamentiScartatiDao dao = new PagamentiScartatiDao(connection, schema);
			return dao.doDeletePagamentiScartati(nomeFile, stato);
		} catch (Exception ex) {
			LogUtility.writeLog("removePagamentiScartati failed, generic error due to: ", ex);
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::removePagamentiScartati errore: " + ex.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			throw new Exception(ex);
		} finally {
			DAOHelper.closeIgnoringException(connection);
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::removePagamentiScartati"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		}	
	}
	
	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	/**
	 *Richiama i metodi notificaScarto e aggornaFlussoScarti. <br>
	 * 
	 * @param Facade
	 * @param List
	 *            <ScartoDto> out
	 */
	private boolean notificationProcessing(List<ScartoDto> out) {
		LogUtility.writeLog("******************************************* inizio AcquisisciFlussiScarti::notificationProcessing"); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
		try {
			boolean x = false;
			//inizio LP PG200070
			//if (out != null) {
			if (out != null && out.size() > 0) {
				String wsNotificheUrl = configuration.getProperty(PropertiesPath.NOTIFICHE_ENDPOINTURL.format(PropertiesPath.DEFAULT_NODE.format()));
				LogUtility.writeLog("wsNotificheUrl: '" + wsNotificheUrl + "'");
				NotificheSOAPBindingStub binding = new NotificheSOAPBindingStub(new URL(wsNotificheUrl), null);
				setCodSocietaHeaderNotifiche(binding, dbSchemaCodSocieta);
			//fine LP PG200070
				for (ScartoDto scarto : out) {
					try {
						// we notify
						//inizio LP PG200070
						//NotificheSOAPBindingStub binding = new NotificheSOAPBindingStub(new URL(configuration.getProperty(PropertiesPath.NOTIFICHE_ENDPOINTURL.format(PropertiesPath.DEFAULT_NODE.format()))), null);
						//setCodSocietaHeaderNotifiche(binding, dbSchemaCodSocieta);
						//fine LP PG200070
						NotificaScartoRequestType notificaAutorizzazioneBancaRequest = new NotificaScartoRequestType(scarto.getChiaveTransazione(), "E", "T", scarto.getDescrizioneTipologiaServizio(), scarto.getDescrizioneEnte(), scarto.getDescrizioneUfficio());
						NotificaScartoResponseType response = binding.notificaScarto(notificaAutorizzazioneBancaRequest);
						if (response.getResponse().getRetcode().getValue().equals(com.seda.payer.notifiche.webservice.dati.ResponseRetcode._value2)) {
							LogUtility.writeLog("elaboraNotifica chiaveTransazione "
									+ scarto.getChiaveTransazione()
									+ " e chiaveScarto "
									+ scarto.getChiaveScarto()
									+ " failed, notify response ["
									+ response.getResponse().getRetcode().getValue()
									+ "] "
									+ response.getResponse().getRetmessage());
							// we mark to state "E"
							aggiornaFlussoScarti(Integer.parseInt(scarto.getChiaveScarto()), 'E');
							continue;
						}
						// we mark to state "Y"
						aggiornaFlussoScarti(Integer.parseInt(scarto.getChiaveScarto()), 'Y');
						x = true;
					} catch (Exception e) {
						LogUtility.writeLog("elaboraNotifica chiaveTransazione "
								+ scarto.getChiaveTransazione()
								+ " e chiaveScarto " + scarto.getChiaveScarto()
								+ " failed, ", e);
						// we mark to state "E"
						aggiornaFlussoScarti(Integer.parseInt(scarto.getChiaveScarto()), 'E');
					}
				}
			}
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::notificationProcessing: " + (x ? "true" : "false")); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			return x;
		} catch (Exception e) {
			e.printStackTrace();
			LogUtility.writeLog("elaboraNotifica failed, generic error due to: ", e);
			LogUtility.writeLog("******************************************* fine AcquisisciFlussiScarti::notificationProcessing errore: " + e.getMessage()); //LP PG22XX10_LP2 - Log inizio e fine operazioni 
			return false;
		}
	}
	
	//da com.seda.payer.pgec.webservice\src\com\seda\payer\pgec\webservice\commons\source\Commons.java
	/**
	 * Aggiorna il campo SCA_FSCAELAB nella tabella degli scarti PYSCATB con il
	 * valore 'Y' (pagamento elaborato) in base alle chiavi scarto contenute nel
	 * parametro in input <code>scarti</code>. <br>
	 * 
	 * @param Facade
	 * @param chiaveScarto
	 * @throws Exception
	 */
	private boolean aggiornaFlussoScarti(int chiaveScarto, char esito) throws Exception {
		try {
			savePagamentiScartati(chiaveScarto, null, null, null, null, esito);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			LogUtility.writeLog("aggiornaFlussiScarti failed, generic error due to: ", e);
			return false;
		}
	}
	
	public void setCodSocietaHeaderNotifiche(NotificheSOAPBindingStub stub, String dbSchemaCodSocieta) {
		stub.clearHeaders();
		stub.setHeader("",DBSCHEMACODSOCIETA, dbSchemaCodSocieta);		
	}
	
}