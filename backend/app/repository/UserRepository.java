package repository;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.ExpressionList;
import models.Passport;
import models.Nationality;
import models.TravellerType;
import models.User;
import play.db.ebean.EbeanConfig;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Contains database calls for all things traveller related
 */
public class UserRepository {

    private final EbeanServer ebeanServer;
    private final DatabaseExecutionContext executionContext;

    /**
     * Dependency injection
     *
     * @param ebeanConfig      ebean config to use
     * @param executionContext Context to run completion stages on
     */
    @Inject
    public UserRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
        this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
        this.executionContext = executionContext;
    }

    /**
     * Updates a users details
     *
     * @param user The user to update
     * @return Nothing
     */
    public CompletionStage<User> updateUser(User user) {
        return supplyAsync(() -> {
            user.update();
            return user;
        }, executionContext);
    }

    /**
     * Gets a user/traveller by their ID
     *
     * @param userId The ID of the user to get
     * @return the user object
     */
    public CompletionStage<Optional<User>> getUserById(int userId) {
        return supplyAsync(() -> {
            Optional<User> user = User.find.query().
                    where().eq("user_id", userId).findOneOrEmpty();
            return user;
        }, executionContext);
    }

    /**
     * A function that gets the list of all the valid passports.
     *
     * @return the list of all the Passports
     */
    public CompletionStage<List<Passport>> getAllPassports() {
        return supplyAsync(() -> {
            List<Passport> passports = Passport.find.query().findList();
            return passports;
        }, executionContext);
    }

    /**
     * Gets a passport by it's ID
     *
     * @param passportId The passport to get
     * @return The list of passports
     */
    public CompletionStage<Optional<Passport>> getPassportById(int passportId) {
        return supplyAsync(() -> {
            Optional<Passport> passport = Passport.find.query().
                    where().eq("passport_id", passportId).findOneOrEmpty();
            return passport;
        }, executionContext);
    }

    /**
     * Gets a list of all nationalities
     *
     * @return List of nationalities
     */
    public CompletionStage<List<Nationality>> getAllNationalities() {
        return supplyAsync(() -> Nationality.find.query().findList(), executionContext);
    }

    /**
     * Gets a nationality by it's ID
     *
     * @param nationalityId The nationality to get
     * @return The list of nationalities
     */
    public CompletionStage<Optional<Nationality>> getNationalityById(int nationalityId) {
        return supplyAsync(() -> {
            Optional<Nationality> nationality = Nationality.find.query().
                    where().eq("nationality_id", nationalityId).findOneOrEmpty();
            return nationality;
        }, executionContext);
    }


    /**
     * Funtion that gets all of the valid traveller types in the database
     *
     * @return the list of traveller types
     */
    public CompletionStage<List<TravellerType>> getAllTravellerTypes() {
        return supplyAsync(() -> {
            List<TravellerType> types = TravellerType.find.query().findList();
            return types;
        }, executionContext);
    }


    /**
     * Gets a list of travellers
     */
    public CompletionStage<List<User>> getTravellers() {
        return supplyAsync(() -> {
            List<User> user = User.find.query()
                    .fetch("passports")              // contacts is a OneToMany path
                    .fetch("travellerTypes")
                    .fetch("nationalities")
                    .where()
                    .isNotNull("middle_name")
                    .isNotNull("gender")
                    .isNotNull("date_of_birth")
                    .isNotEmpty("nationalities")
                    .isNotEmpty("travellerTypes")
                    .findList();
            return user;
        }, executionContext);
    }


    /**
     * Function to search through the user database
     *
     * @param nationality     nationality id
     * @param gender          gender string
     * @param dateMin         min age Date
     * @param dateMax         max age Date
     * @param travellerTypeId traveller type Id
     * @return List of users or empty list
     */
    public CompletionStage<List<User>> searchUser(int nationality, String gender, Date dateMin, Date dateMax, int travellerTypeId) {


        return supplyAsync(() -> {
            boolean found;
            ExpressionList<User> query = User.find.query()
                    .fetch("travellerTypes").where();
            if (gender != null) {
                query = query.eq("gender", gender);
            }
            if (travellerTypeId != -1) {
                query = query.where().eq("traveller_type_id", travellerTypeId);
            }
            query = query.where().between("dateOfBirth", dateMax, dateMin)
                    .isNotNull("dateOfBirth")
                    .isNotNull("gender")
                    .isNotEmpty("travellerTypes");

            List<User> users = query.findList();

            if (nationality != -1) {
                List<User> filteredUsers = new ArrayList<User>();
                for (int i = 0; i < users.size(); i++) {
                    found = false;
                    List<Nationality> natsToCheck = users.get(i).getNationalities();
                    for (int j = 0; j < natsToCheck.size(); j++) {
                        if (natsToCheck.get(j).getNationalityId() == nationality) {
                            found = true;
                        }
                    }
                    if (found) {
                        filteredUsers.add(users.get(i));
                    }
                }
                return filteredUsers;
            } else return users;
        }, executionContext);
    }


}