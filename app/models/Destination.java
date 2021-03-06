package models;

import io.ebean.annotation.Index;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.*;
import io.ebean.annotation.SoftDelete;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A destination that a traveller can choose to go to.
 */
@Entity
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {
                "destination_name",
                "destination_country_country_id",
                "destination_type_destination_type_id",
                "is_public",
                "destination_owner"
            }))
public class Destination extends Model {

    @Id
    private int destinationId;

    @Index
    private String destinationName;

    @ManyToOne
    private DestinationType destinationType;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL)
    private List<TripDestinationLeaf> tripDestinations;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL)
    private List<DestinationPhoto> destinationPhotos;

    @OneToMany(mappedBy = "treasureHuntDestination", cascade = CascadeType.ALL)
    private List<TreasureHunt> treasureHunts;

    @ManyToMany(mappedBy = "destinations", cascade = CascadeType.PERSIST)
    private List<TravellerType> travellerTypes;

    private String destinationDistrict;

    private Double destinationLat;
    private Double destinationLon;
    @ManyToOne
    private Country destinationCountry;

    @ManyToOne
    private Integer destinationOwner;

    private boolean isPublic;

    @JsonIgnore
    @SoftDelete
    @Column(name = "deleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean deleted;

    @JsonIgnore
    private Timestamp deletedExpiry;


    @Override
    public int hashCode() {
        return destinationId;
    }

    @Override
    public String toString() {
        return "Destination{" +
                "destinationId=" + destinationId +
                ", destinationName='" + destinationName + '\'' +
                ", destinationType=" + destinationType +
                ", tripDestinations=" + tripDestinations +
                ", destinationPhotos=" + destinationPhotos +
                ", destinationDistrict=" + destinationDistrict +
                ", destinationLat=" + destinationLat +
                ", destinationLon=" + destinationLon +
                ", destinationCountry=" + destinationCountry +
                ", destinationOwner=" + destinationOwner +
                ", travellerTypes=" + travellerTypes +
                ", isPublic=" + isPublic +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Destination)) {
        return false;
      }
      Destination destinationToCompare = (Destination) obj;
      boolean sameName =
          this.destinationName.equalsIgnoreCase(destinationToCompare.getDestinationName().toLowerCase());
      boolean sameType = this.destinationType.equals(destinationToCompare.getDestinationType());
      boolean sameCountry =
          this.destinationCountry.equals(destinationToCompare.getDestinationCountry());

      return (sameName && sameType && sameCountry);
    }

    /**
     * Constructor.
     *
     * @param destinationName name of the destination
     * @param destinationType type of destination
     * @param destinationDistrict district the destination is in
     * @param destinationLat latitiude of the destination
     * @param destinationLon longitude of the destination
     * @param destinationCountry country the destination is in
     * @param destinationOwner the owner of the destination
     * @param isPublic whether or not the destination is public
     */
    public Destination(
        String destinationName,
        DestinationType destinationType,
        String destinationDistrict,
        Double destinationLat,
        Double destinationLon,
        Country destinationCountry,
        Integer destinationOwner,
        List<TravellerType> travellerTypes,
        boolean isPublic) {
      this.destinationName = destinationName;
      this.destinationType = destinationType;
      this.destinationDistrict = destinationDistrict;
      this.destinationLat = destinationLat;
      this.destinationLon = destinationLon;
      this.destinationCountry = destinationCountry;
      this.destinationOwner = destinationOwner;
      this.travellerTypes = travellerTypes;
      this.isPublic = isPublic;
    }

    /**
     * Constructor for destination ID
     */
    public Destination(int destinationId) {
      this.destinationId = destinationId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedExpiry(Timestamp deletedExpiry) {
        this.deletedExpiry = deletedExpiry;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationDistrict() {
        return destinationDistrict;
    }

    public void setDestinationDistrict(String destinationDistrict) {
        this.destinationDistrict = destinationDistrict;
    }

    public Double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(Double destinationLat) {
        this.destinationLat = destinationLat;
    }

    public Double getDestinationLon() {
        return destinationLon;
    }

    public void setDestinationLon(Double destinationLon) {
        this.destinationLon = destinationLon;
    }

    public Country getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(Country destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public void setDestinationOwner(Integer destinationOwner) { this.destinationOwner = destinationOwner; }

    public Integer getDestinationOwner() { return this.destinationOwner; }

    public void setIsPublic (boolean isPublic) { this.isPublic = isPublic; }

    public boolean getIsPublic() { return this.isPublic; }


    public List<TravellerType> getTravellerTypes() {
        return travellerTypes;
    }

    public void setTravellerTypes(List<TravellerType> travellerTypes) {
        this.travellerTypes = travellerTypes;
    }

    public void setDestinationPhotos(List<DestinationPhoto> destinationPhotos) {
        this.destinationPhotos = destinationPhotos;
    }

    public List<DestinationPhoto> getDestinationPhotos() {
        return destinationPhotos;
    }

    /**
     * Get the the public photos linked to the destination
     *
     * @return a list of all the public photos in the destination
     */
    public List<DestinationPhoto> getPublicDestinationPhotos() {
      return destinationPhotos
          .parallelStream()
          .filter((destinationPhoto -> destinationPhoto.getPersonalPhoto().isPublic()))
          .collect(Collectors.toList());
    }

    /**
     * Get the private photos from a particular user linked to the destination
     *
     * @param userId the id of the user for which we want the private photos in the destination
     * @return the private photos for the given user that are linked with the destination
     */
    public List<DestinationPhoto> getPrivatePhotosForUserWithId(int userId) {
        return destinationPhotos.parallelStream()
            .filter((destinationPhoto -> destinationPhoto.getPersonalPhoto().getUser().getUserId() == userId))
            .filter(destinationPhoto -> !destinationPhoto.getPersonalPhoto().isPublic())
            .collect(Collectors.toList());
    }

    /**
     * This is required by EBean to make queries on the database.
     */
    public static final Finder<Integer, Destination> find = new Finder<>(Destination.class);
}
