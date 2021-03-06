package horizon.aether.model;

import java.util.ArrayList;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.PersistenceModifier;
import javax.jdo.annotations.Extension;

import org.postgis.Point;

/**
 * Class that represents a Wi-Fi entry. A Wi-Fi entry 
 * consists of a collection of networks.
 */
@PersistenceCapable
public class WifiEntry {

    public static final String IDENTIFIER = "AP_SITINGS";
    
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.UUIDHEX)
    private String key;

    @Persistent
    private long timestamp;
    
    @Persistent(defaultFetchGroup = "true")
    private Location location;

	// Support for PostGIS geometries
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	@Extension(vendorName="datanucleus", key="spatial-srid", value="4269")
	private Point point;

    @Persistent
    ArrayList<Wifi> networks;
    
    /**
     * Gets the key.
     * @return
     */
    public String getKey() { return this.key; }
    
    /**
     * Gets the timestamp.
     * @return
     */
    public long getTimestamp() { return this.timestamp; }
    
    /**
     * Gets the location.
     * @return
     */
    public Location getLocation() { return this.location; }

    public ArrayList<Wifi> getNetworks() { return this.networks; }
    
    public void setNetworks(ArrayList<Wifi> networks) { this.networks = networks; }
    
    /**
     * Constructor
     * @param timestamp
     * @param location
     * @param networks
     */
    public WifiEntry(long timestamp, Location location, 
					 ArrayList<Wifi> networks) 
		throws java.sql.SQLException
	{
        this.timestamp = timestamp;
        this.location = location;
        this.networks = networks;
		this.point = new Point("SRID=4269;POINT(" + location.getLongitude() + 
							   " " + location.getLatitude() + "");
    }
    
    /**
     * Constructor.
     * @param timestamp
     * @param location
     * @param blob
     */
    public WifiEntry(long timestamp, Location location, WifiBlob blob) 
		throws java.sql.SQLException
	{
        this.timestamp = timestamp;
        this.location = location;
        this.networks = blob.getNetworks();
		this.point = new Point("SRID=4269;POINT(" + location.getLongitude() + 
							   " " + location.getLatitude() + "");
    }
    /**
     * Default constructor.
     */
    public WifiEntry() {
        this.networks = new ArrayList<Wifi>();
    }

}
