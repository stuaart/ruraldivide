package horizon.aether.model;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.PersistenceModifier;
import javax.jdo.annotations.Extension;

import org.postgis.Point;

//import com.google.appengine.api.datastore.Key;

/**
 * Class that represents a Signal Strength Entry.
 */
@PersistenceCapable
public class SignalStrengthEntry {
    
    public static final String IDENTIFIER = "SIGNAL_STRENGTH";
    
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.UUIDHEX)
    private String key;

    @Persistent
    private long timestamp;
    
    @Persistent(defaultFetchGroup = "true")
    private Location location;

    @Persistent
    private int strength;
	
	// Support for PostGIS geometries
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	@Extension(vendorName="datanucleus", key="spatial-srid", value="4269")
	private Point point;

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

    /**
     * Gets the strength.
     * @return The strength.
     */
    public int getStrength() { return this.strength; }

    /**
     * Sets the strength.
     * @param strength
     */
    public void setStrength(int strength) { this.strength = strength; }
    
    /**
     * Constructor.
     * @param timestamp
     * @param location
     * @param strength
     */
    public SignalStrengthEntry(long timestamp, Location location, int strength)
		throws java.sql.SQLException
	{
        this.timestamp = timestamp;
        this.location = location;        
        this.strength = strength;
		this.point = new Point("SRID=4269;POINT(" + location.getLongitude() + 
							   " " + location.getLatitude() + "");
    }
    
    /**
     * Constructor.
     * @param timestamp
     * @param location
     * @param blob
     */
    public SignalStrengthEntry(long timestamp, Location location, 
							   SignalStrengthBlob blob) 
		throws java.sql.SQLException
	{
        this.timestamp = timestamp;
        this.location = location;
        this.strength = blob.getStrength();
		this.point = new Point("SRID=4269;POINT(" + location.getLongitude() + 
							   " " + location.getLatitude() + "");
    }
    
    /**
     * Default constructor.
     */
    public SignalStrengthEntry() { }
}
