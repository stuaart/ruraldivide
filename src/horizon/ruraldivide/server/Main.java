package horizon.ruraldivide.server;

import horizon.aether.model.*;
import horizon.aether.utilities.CompressionUtils;
import horizon.aether.utilities.CompressionUtils.CompressionType;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.sql.SQLException;

import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.*;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.postgis.*;

@Controller
public class Main
{

    private static final Logger log = 
		Logger.getLogger(Main.class.getName());

	private static final CompressionType compressionType = CompressionType.NONE;


  /**
     * GET requests return empty page.
     */
    @RequestMapping(value = "/server/*", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
    public void getResponse() 
	{
    }

    /**
     * POST requests are used to receive archived log files. The file is 
	 * received, uncompressed and parsed to retrieve the logging data that are 
	 * eventually persisted on the database.
     * @param req
     * @return an empty response
     */
    @RequestMapping(value = "/server/*", method = RequestMethod.POST)
    public String postResponse(HttpServletRequest req)
    {
        try
        {
        	// Look at all the submitted parts of the post data and process 
			// files as necessary
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iterator = upload.getItemIterator(req);
            while (iterator.hasNext())
            {
                FileItemStream item = iterator.next();
                InputStream stream = item.openStream();

                // Form fields will be returned too, but Files are only 
				// important
                if (!item.isFormField())
                {
                	// The uncompressed file can also be uploaded by naming the
					// file upload field 'uncompressed' - otherwise assume it's
					// compressed... 
                	CompressionType compressionType = CompressionType.DEFAULT;
                	if (item.getFieldName().contentEquals("uncompressedfile"))
                	{
                		compressionType = CompressionType.NONE;
                	}
                	else if (item.getFieldName().contentEquals("gzippedfile"))
                	{
                		compressionType = CompressionType.GZIP;
                	}
                	

                	// Uncompress using the appropriate compression type...
                    OutputStream uncompressedStream = 
						new ByteArrayOutputStream();
                    try
                    {
                    	if (!CompressionUtils.uncompress(stream, 
								uncompressedStream, compressionType))
	                    {
	                    	throw new ServletException(
								"Failed to uncompress file (default)");
	                    }
                    }
                    catch (Exception ex)
                    {
                    	throw new ServletException(
							"Failed to uncompress file...", ex);
                    }
                    // parse and persist file entries
                    parseAndPersistEntries(uncompressedStream.toString());
                }
            }
        }
        catch (FileUploadException ex) 
		{
            log.warn(ex.toString());
        }
        catch (IOException ex) 
		{
            log.warn(ex.toString());
        }
        catch (ServletException ex) 
		{
            log.warn(ex.toString());
        }
        
        return "empty";
    }


/*	@SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
	{
    	/*if(args.length==0)
    	{
    		throw new Exception("No input file specified!");
    	}
    	String filenameArg = args[0];
    	if(args.length > 1)
    	{
    		String compressionTypeArg = args[1];
	    	if(compressionTypeArg.contentEquals("none"))
	    	{
	    		compressionType = CompressionType.NONE;
	    	}
	    	else if (compressionTypeArg.contentEquals("gz"))
	    	{
	    		compressionType = CompressionType.GZIP;
	    	}
	    	else if (compressionTypeArg.contentEquals("dfl"))
	    	{
	    		compressionType = CompressionType.DEFAULT;
	    	}
    	}
    	String sourceFileName = filenameArg;*/
/*
		String sourceFileName = "sensorservice.log";
		if (args.length > 0)
	    	sourceFileName = args[0];

    	//Set up the log4j log to log to console
		PropertyConfigurator.configure("log4j.properties");
		log.setLevel(Level.INFO);
		

	}*/

   /**
     * Parses a string and creates the entry objects using Jackson's full data 
	 * binding. The entries are then persisted on the database. 
     * @param contents
     * @return
     */
    @SuppressWarnings("unchecked")
    public static void parseAndPersistEntries(String contents) 
	{
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(
			DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
		PersistenceManager pm = PMF.get().getPersistenceManager();
        
		Transaction tx = pm.currentTransaction();

        try 
		{
			BufferedInputStream stream = new BufferedInputStream(
				new FileInputStream(contents));
	    	OutputStream uncompressedStream = new ByteArrayOutputStream();
            CompressionUtils.uncompress(stream, uncompressedStream, 
										compressionType);

			BufferedReader reader = new BufferedReader(
				new StringReader(uncompressedStream.toString()));

	        String strLine;
			log.info("Read file");
            
            while ((strLine = reader.readLine()) != null) 
			{
				Map<String,Object> entry = mapper.readValue(strLine, Map.class);
				tx.begin();
				processEntry(entry, mapper, pm);
				tx.commit();
			}
			
        }
        catch (JsonParseException e) { 
            log.error(e.toString(), e); 
        }
        catch (JsonMappingException e) {
            log.error(e.toString(), e); 
        }
        catch (IOException e) {
            log.error(e.toString(), e); 
        }
		catch (SQLException e)
		{
			log.error(e.toString(), e);
		}
		catch (Exception e)
		{
			log.error(e.toString(), e);
		}
		finally
        {
            if (tx.isActive())
                tx.rollback();
            pm.close();
        }
		
	}


	private static void processEntry(Map<String, Object> entry,
									 ObjectMapper mapper,
									 PersistenceManager pm)
		throws SQLException, IOException, JsonParseException, 
			   JsonMappingException
	{

		// timestamp
		long timestamp = 
			Long.parseLong(entry.get("timestamp").toString());

		// identifier
		String identifier = entry.get("identifier").toString();

		// location
		Location location = null;
		if (entry.get("location") != null) 
		{
			location = 
				mapper.readValue(entry.get("location").toString(), 
								 Location.class);
			location.initGISPoint(); // Manual init
		}

		// blob data
		String dataBlob = entry.get("dataBlob").toString();
		if (identifier.equals(WifiEntry.IDENTIFIER)) {
			ArrayList<Wifi> networks = 
				mapper.readValue(dataBlob, 
								 TypeFactory.collectionType(
									ArrayList.class, Wifi.class)
								);
			pm.makePersistent(
				new WifiEntry(timestamp, location, networks)
			);
		}
		else if (identifier.equals(CellLocationEntry.IDENTIFIER)) {
			CellLocationBlob blob = 
				(CellLocationBlob)mapper.readValue(dataBlob, 
												   CellLocationBlob.class);
			pm.makePersistent(new CellLocationEntry(timestamp, location, blob));
		}
		else if (identifier.equals(DataConnectionStateEntry.IDENTIFIER)) {
			DataConnectionStateBlob blob = 
				(DataConnectionStateBlob)mapper.readValue(
					dataBlob, DataConnectionStateBlob.class);
			pm.makePersistent(new DataConnectionStateEntry(timestamp, location,
														   blob));
		}
		else if (identifier.equals(ServiceStateEntry.IDENTIFIER)) {
			ServiceStateBlob blob = 
				(ServiceStateBlob)mapper.readValue(dataBlob, 
												   ServiceStateBlob.class);
			pm.makePersistent(new ServiceStateEntry(timestamp, location, blob));
		}
		else if (identifier.equals(SignalStrengthEntry.IDENTIFIER)) {
			SignalStrengthBlob blob = 
				(SignalStrengthBlob)mapper.readValue(dataBlob, 
													 SignalStrengthBlob.class);
			pm.makePersistent(new SignalStrengthEntry(timestamp, location, 
													  blob));
		}
		else if (identifier.equals(
					SignalStrengthOnLocationChangeEntry.IDENTIFIER)
				) 
		{
			SignalStrengthOnLocationChangeBlob blob = 
				(SignalStrengthOnLocationChangeBlob)mapper.readValue(dataBlob, 
					SignalStrengthOnLocationChangeBlob.class);
			// Use the location recorded by the OnLocationChange event for this
			// instead of the geotagger app log.
			Location loc = new Location(blob.getAccuracy(), blob.getAltitude(),
										blob.getBearing(), blob.getLatitude(), 
										blob.getLongitude(), blob.getProvider(),
										blob.getSpeed(), blob.getExtras()); 
			pm.makePersistent(new SignalStrengthOnLocationChangeEntry(timestamp,
																loc, blob));
			// Also create a SignalStrength entry as above with the normal 
			// location data not sure whether this is useful but seems to be...
			SignalStrengthBlob blob2 = new SignalStrengthBlob();
			blob2.setStrength(blob.getSignalStrength());
			pm.makePersistent(new SignalStrengthEntry(timestamp, location, 
													  blob2));
		}
		else if (identifier.equals(TelephonyStateEntry.IDENTIFIER)) {
			TelephonyStateBlob blob = 
				(TelephonyStateBlob)mapper.readValue(dataBlob, 
													 TelephonyStateBlob.class);
			pm.makePersistent(new TelephonyStateEntry(timestamp, location, 
													  blob));
		}
				
	}

	private static void cleanDatabase(PersistenceManager pm, Transaction tx)
	{

		tx.begin();
		pm.newQuery(WifiEntry.class).deletePersistentAll();
		tx.commit();
		tx.begin();
		pm.newQuery(CellLocationEntry.class).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(DataConnectionStateEntry.class).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(ServiceStateEntry.class).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(SignalStrengthEntry.class).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(SignalStrengthOnLocationChangeEntry.class
						).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(SignalStrengthEntry.class).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(TelephonyStateEntry.class).deletePersistentAll();
		tx.commit();
		log.info(".");
		tx.begin();
		pm.newQuery(Location.class).deletePersistentAll();
		tx.commit();
		log.info("Cleaned up db old entries");

	}

}


