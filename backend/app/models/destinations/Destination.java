package models.destinations;

import javax.persistence.Entity;
import io.ebean.*;

/**
 * A destination that a traveller can choose to go to.
 */
@Entity
public class Destination extends models.BaseModel {

    /**
     * Constructor.
     * @param name name of the destination.
     * @param destinationType type of the destination.
     * @param district district the destination is in.
     * @param latitude latitude of the destination.
     * @param longitude longitude of the destination.
     * @param country country the destination is in.
     */
    public Destination(String name, DestinationType destinationType, String district, Double latitude, Double longitude, String country) {
        this.name = name;
        this.destinationType = destinationType;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
    }

    private String name;
    private DestinationType destinationType;
    private String district;
    private Double latitude;
    private Double longitude;
    private String country;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * This is required by EBean to make queries on the database
     */
    public static final Finder<Long, Destination> find = new Finder<>(Destination.class);
}