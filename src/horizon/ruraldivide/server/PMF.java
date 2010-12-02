package horizon.ruraldivide.server;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import java.util.Properties;


/**
 * Singleton wrapper class with a static instance for the 
 * PersistenceManagerFactory class.
 */
public final class PMF 
{
    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory(
			generatePropertiesPostGIS("localhost:5432", "features_gis",
									  "postgres", "r4nd0m")
		);
	private static Properties propInstance;

    private PMF() {}

    public static PersistenceManagerFactory get() 
	{
        return pmfInstance;
    }

	public static Properties generatePropertiesPostGIS(String hostport, 
									String dbname, String user, String pass)
	{
		propInstance = new Properties();
		propInstance.setProperty("javax.jdo.PersistenceManagerFactoryClass",
		    "org.datanucleus.jdo.JDOPersistenceManagerFactory");
		propInstance.setProperty("javax.jdo.option.ConnectionDriverName",
			"org.postgresql.Driver");
		propInstance.setProperty("javax.jdo.option.ConnectionURL",
			"jdbc:postgresql://" + hostport + "/" + dbname);
		propInstance.setProperty("javax.jdo.option.ConnectionUserName", user);
		propInstance.setProperty("javax.jdo.option.ConnectionPassword", pass);
		propInstance.setProperty("javax.jdo.PersistenceManagerFactoryClass", 
			"org.datanucleus.jdo.JDOPersistenceManagerFactory");
		propInstance.setProperty("javax.jdo.option.Optimistic", "false");
		propInstance.setProperty("javax.jdo.option.IgnoreCache", "false");
		propInstance.setProperty("datanucleus.autoCreateTables", "true");
		propInstance.setProperty("datanucleus.validateTables", "true");
		propInstance.setProperty("datanucleus.autoCreateColumns", "true");
		propInstance.setProperty("datanucleus.autoCreateConstraints", "true");
		propInstance.setProperty("datanucleus.validateConstraints", "true");
		propInstance.setProperty("datanucleus.autoCreateSchema", "true");

		return propInstance;
	}
}
