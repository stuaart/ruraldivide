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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.sql.SQLException;
import java.net.URL;
import java.net.MalformedURLException;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;

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
import org.springframework.web.servlet.ModelAndView;

import org.geoserver.rest.client.datatypes.*;

import org.postgis.*;

@Controller
public class Main
{

    private static final Logger log = 
		Logger.getLogger(Main.class.getName());


    
	@RequestMapping(value = "/main", method = RequestMethod.GET)
    public String mainPageGET() 
	{
		log.info("Main.mainPageGET()");
		return "main";
    }


	@RequestMapping(value = "/createlayer", method = RequestMethod.POST)
	public ModelAndView createLayerPOST(HttpServletRequest req)
	{
		log.info("createLayerPOST()");

		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getCredentialsProvider().setCredentials(
			new AuthScope(AuthScope.ANY),
			new UsernamePasswordCredentials("admin", "r4nd0m")
		);

		HttpGet httpget = 
			new HttpGet("http://localhost:8080/geoserver/rest/layers/Area_Mosaic_polyline.json");
        
		StringBuffer result = new StringBuffer();
		try 
		{
			ResponseHandler<String> responseHandler = 
				new BasicResponseHandler();
        	String responseBody = httpclient.execute(httpget, responseHandler);
			
			httpclient.getConnectionManager().shutdown();

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(
				DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, 
				false
			);
			log.info("JSON="+responseBody);
			try
			{
				Map<String,Object> entry = 
					mapper.readValue(responseBody, Map.class);

				log.info("layer="+entry.get("layer").toString());
				Layer layer = 
					mapper.readValue(entry.get("layer").toString(), 
									 Layer.class);
				log.info("Layer layer.toString="+layer.toString());

/*
				Layers layer = mapper.readValue(responseBody, Layers.class);
				log.info("list size = " + layers.getLayer().size());
				for (Iterator i = layers.getLayer().iterator(); i.hasNext(); )
				{
					Layer l = (Layer)i.next();
					result.append(l.getName() + "; " + l.getHref() + "... ");
				}
*/
				result.append(layer.getName() + "; " + layer.getHref() + 
								"... ");
			}
	      	catch (JsonParseException e) { 
        		log.error(e.toString(), e); 
        	}
        	catch (JsonMappingException e) {
        	    log.error(e.toString(), e); 
        	}

		}
		catch (IOException e) 
		{
			log.error(e.toString());
		}

        
		return new ModelAndView("message", 
								"text", 
								"Test layer created via REST interface \""+
								result.toString()+ "\"");

	}

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
    public void uploadGET()
	{
		log.info("Main.uploadGET()");
    }


    @RequestMapping(value = "/upload/single", method = RequestMethod.POST)
    public ModelAndView singleUploadPOST(HttpServletRequest req)
		throws ServletException
    {
		log.info("Main.singleUploadPOST()");
		      try
        {
        	// Look at all the submitted parts of the post data and process 
			// files as necessary
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iterator = upload.getItemIterator(req);
            while (iterator.hasNext())
            {
                FileItemStream item = iterator.next();
                InputStream is = item.openStream();

                // Form fields will be returned too, but Files are only 
				// important
                if (!item.isFormField())
                {
					// Single entry is never compressed, but put it through util
					// anyway
                    OutputStream os = new ByteArrayOutputStream();
                    try
                    {
                    	CompressionUtils.uncompress(is, os, 
													CompressionType.NONE);
                    }
                    catch (Exception ex)
                    {
                    	throw new ServletException(
							"Single entry upload failure", ex);
                    }

					parseAndPersistEntries(os.toString());
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
        
  		return new ModelAndView("message", 
								"text", 
								"Uploader processed single entry");
	}


    @RequestMapping(value = "/upload/bulk", method = RequestMethod.POST)
    public ModelAndView bulkUploadPOST(HttpServletRequest req)
    {
		log.info("Main.bulkUploadPOST()");
		int count = 0;
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
                    count = 
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
        
  		return new ModelAndView("message", 
								"text", 
								"Uploader processed " + count + " entries");

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

    
	@SuppressWarnings("unchecked")
    public static int parseAndPersistEntries(String contents) 
	{
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(
			DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
		PersistenceManager pm = PMF.get().getPersistenceManager();
        
		Transaction tx = pm.currentTransaction();
		
		int count = -1;
        try 
		{
			BufferedReader reader = 
				new BufferedReader(new StringReader(contents));

	        String strLine;
			log.info("parseAndPersistEntries(): Reading log file");
            
			count = 0;
            while ((strLine = reader.readLine()) != null) 
			{
				Map<String,Object> entry = mapper.readValue(strLine, Map.class);
				tx.begin();
				processEntry(entry, mapper, pm);
				++count;
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
		
		return count;
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
		/*
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
			*/
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

	@RequestMapping(value = "/cleandb", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)	
	public void cleanDatabaseGET()
	{
		log.info("cleanDatabaseGET()");
	}

	@RequestMapping(value = "/cleandb", method = RequestMethod.POST)
	public ModelAndView cleanDatabasePOST(HttpServletRequest req)
	{
		log.info("cleanDatabasePOST()");

		PersistenceManager pm = PMF.get().getPersistenceManager();
		Transaction tx = pm.currentTransaction();

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

		return new ModelAndView("message", 
								"text", 
								"Cleaned up database of persistent objects");
	}

}


