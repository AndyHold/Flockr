package controllers;

import actions.ActionState;
import actions.LoggedIn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.BadRequestException;
import exceptions.ForbiddenRequestException;
import exceptions.NotFoundException;
import models.*;
import play.libs.Json;
import play.mvc.*;
import repository.DestinationRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import play.libs.concurrent.HttpExecutionContext;


/**
 * Controller to manage endpoints related to destinations.
 */
public class DestinationController extends Controller {
    private final DestinationRepository destinationRepository;
    private HttpExecutionContext httpExecutionContext;

    @Inject
    public DestinationController(DestinationRepository destinationRepository, HttpExecutionContext httpExecutionContext) {
        this.destinationRepository = destinationRepository;
        this.httpExecutionContext = httpExecutionContext;
    }

    /**
     * A function that gets a list of all the destinations and returns it with a 200 ok code to the HTTP client
     *
     * @param request Http.Request the http request
     * @return a completion stage and a status code 200 if the request is successful, otherwise returns 500.
     */
    public CompletionStage<Result> getDestinations(Http.Request request) {
        return destinationRepository.getDestinations()
                .thenApplyAsync(destinations -> ok(Json.toJson(destinations)), httpExecutionContext.current());
    }

    /**
     * A function that retrieves a destination details based on the destination ID given
     *
     * @param destinationId the destination Id of the destination to retrieve
     * @param request       request Object
     * @return a completion stage and a Status code of 200 and destination details as a Json object if successful,
     * otherwise returns status code 404 if the destination can't be found in the db.
     */

    public CompletionStage<Result> getDestination(int destinationId, Http.Request request) {

        return destinationRepository.getDestinationById(destinationId)
                .thenApplyAsync((destination) -> {
                    if (!destination.isPresent()) {
                        ObjectNode message = Json.newObject();
                        message.put("message", "No destination exists with the specified ID");
                        return notFound(message);
                    }

                    JsonNode destAsJson = Json.toJson(destination);

                    return ok(destAsJson);

                }, httpExecutionContext.current());
    }

    /**
     * Get the destinations owned by a user.
     * If the user is viewing their own destinations, return all their destinations.
     * If the user is viewing someone elses destinations, return just the public destinations of the
     * specified user.
     *
     * @param userId  user to find destinations for.
     * @param request HTTP request.
     * @return 200 status code with the data for the user's destinations in body
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> getUserDestinations(int userId, Http.Request request) {
        User loggedInUser = request.attrs().get(ActionState.USER);
        return destinationRepository.getDestinations()
                .thenApplyAsync(destinations -> {
                    if (loggedInUser.getUserId() == userId) {
                        List<Destination> userDestinations = destinations.stream()
                                .filter(destination -> destination.getDestinationOwner() == userId).
                                        collect(Collectors.toList());
                        return ok(Json.toJson(userDestinations));
                    } else {
                        List<Destination> userDestinations = destinations.stream()
                                .filter(destination -> destination.getDestinationOwner() == userId
                                        && destination.getIsPublic()).
                                        collect(Collectors.toList());
                        return ok(Json.toJson(userDestinations));
                    }
                }, httpExecutionContext.current());
    }


    /**
     * Function to add destinations to the database
     *
     * @param request the HTTP post request.
     * @return a completion stage with a 200 status code and the new json object or a status code 400.
     */

    @With(LoggedIn.class)
    public CompletionStage<Result> addDestination(Http.Request request) {
        JsonNode jsonRequest = request.body().asJson();

        int user = request.attrs().get(ActionState.USER).getUserId();

        try {
            // check that the request has a body
            if (jsonRequest.isNull()) {
                throw new BadRequestException("No details received, please send a valid request.");
            }

            String destinationName;
            int destinationTypeId;
            int districtId;
            Double latitude;
            Double longitude;
            int countryId;

            try {
                destinationName = jsonRequest.get("destinationName").asText();
                destinationTypeId = jsonRequest.get("destinationTypeId").asInt();
                districtId = jsonRequest.get("districtId").asInt();
                latitude = jsonRequest.get("latitude").asDouble();
                longitude = jsonRequest.get("longitude").asDouble();
                countryId = jsonRequest.get("countryId").asInt();
            } catch (NullPointerException exception) {
                throw new BadRequestException("One or more fields are missing.");
            }

            if (destinationRepository.CheckDestinations(countryId,
                    destinationName, destinationTypeId, districtId)) {
                throw new BadRequestException("Duplicate destination already exists");
            }


            DestinationType destinationTypeAdd = DestinationType.find.byId(destinationTypeId);
            District districtAdd = District.find.byId(districtId);
            Country countryAdd = Country.find.byId(countryId);
            if (destinationTypeAdd == null || districtAdd == null || countryAdd == null) {
                throw new BadRequestException("One of the fields you have selected does not exist.");
            }

            Destination destination = new Destination(destinationName, destinationTypeAdd, districtAdd,
                    latitude, longitude, countryAdd, user, false);

            return destinationRepository.insert(destination)
                    .thenApplyAsync(insertedDestination -> created(Json.toJson(insertedDestination)), httpExecutionContext.current());
        } catch (BadRequestException e) {
            ObjectNode message = Json.newObject();
            message.put("message", e.getMessage());
            return supplyAsync(() -> badRequest(message));
        }

    }

    /**
     * Endpoint to update a destination's details
     *
     * @param request       Request body to get json body from
     * @param destinationId The destination ID to update
     * @return Returns status code 200 if successful, 404 if the destination isn't found, 500 for other errors.
     *
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> updateDestination(Http.Request request, int destinationId) {
        JsonNode response = Json.newObject();
        User user = request.attrs().get(ActionState.USER);
        return destinationRepository.getDestinationById(destinationId)
                .thenComposeAsync(optionalDest -> {
                    if (!optionalDest.isPresent()) {
                        throw new CompletionException(new NotFoundException());
                    }

                    // Checks that the user is either the admin or the owner of the photo to change permission groups
                    if (!user.isAdmin() && user.getUserId() != optionalDest.get().getDestinationOwner()) {
                        throw new CompletionException(new ForbiddenRequestException("You're not allowed to change the permission group."));
                    }

                    JsonNode jsonBody = request.body().asJson();

                    String destinationName = jsonBody.get("destinationName").asText();
                    int destinationTypeId = jsonBody.get("destinationTypeId").asInt();
                    int countryId = jsonBody.get("countryId").asInt();
                    int districtId = jsonBody.get("districtId").asInt();
                    double latitude = jsonBody.get("latitude").asDouble();
                    double longitude = jsonBody.get("longitude").asDouble();
                    boolean isPublic = jsonBody.get("isPublic").asBoolean();

                    Destination destination = optionalDest.get();

                    DestinationType destType = new DestinationType(null);
                    destType.setDestinationTypeId(destinationTypeId);

                    Country country = new Country(null);
                    country.setCountryId(countryId);

                    District district = new District(null, null);
                    district.setDistrictId(districtId);

                    destination.setDestinationName(destinationName);
                    destination.setDestinationType(destType);
                    destination.setDestinationCountry(country);
                    destination.setDestinationDistrict(district);
                    destination.setDestinationLat(latitude);
                    destination.setDestinationLon(longitude);
                    destination.setIsPublic(isPublic);

                    List<Integer> duplicatedDestinationIds = new ArrayList<>();
                    // Checks if destination is a duplicate destination
                    boolean exists = false;
                    List<Destination> destinations = Destination.find.query().findList();
                    for (Destination dest : destinations) {
                        if (dest.getDestinationId() != destinationId && destination.getIsPublic()) {
                            exists = destination.equals(dest);
                            if (exists && !dest.getIsPublic()) {
                                duplicatedDestinationIds.add(dest.getDestinationId());
                            }
                        }
                    }

                    for (int destId : duplicatedDestinationIds) {
                        Optional<Destination> optDest = Destination.find.query().
                                where().eq("destination_id", destId).findOneOrEmpty();
                        System.out.println(destId);

                        // TODO: Delete the private dest and then...
                    }
                    
                    return destinationRepository.update(destination);
                }, httpExecutionContext.current())
                .thenApplyAsync(destination -> (Result) ok(), httpExecutionContext.current())
                .exceptionally(error -> {
                    try {
                        throw error.getCause();
                    } catch (NotFoundException notFoundE) {
                        ((ObjectNode) response).put("message", "There is no destination with the given ID found");
                        return notFound(response);
                    } catch (ForbiddenRequestException forbiddenRequest) {
                        ((ObjectNode) response).put("message", "You are unathorised to update this destination");
                        return forbidden(response);
                    } catch (Throwable ee) {
                        ((ObjectNode) response).put("message", "Endpoint under development");
                        return internalServerError(response);
                    }
                });
    }

    /**
     * Endpoint to delete a destination given its id
     *
     * @param destinationId the id of the destination that we want to delete
     * @param request       the request sent by the routes file
     * @return Status code 200 if successful, 404 if not found, 500 otherwise.
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> deleteDestination(int destinationId, Http.Request request) {
        return destinationRepository.getDestinationById(destinationId)
                .thenComposeAsync(optionalDestination -> {
                    if (!optionalDestination.isPresent()) {
                        throw new CompletionException(new NotFoundException());
                    }
                    Destination destination = optionalDestination.get();
                    ObjectNode success = Json.newObject();
                    success.put("message", "Successfully deleted the given destination id");
                    return this.destinationRepository.deleteDestination(destination.getDestinationId());
                }, httpExecutionContext.current())
                .thenApplyAsync(destId -> (Result) ok(), httpExecutionContext.current())
                .exceptionally(e -> {
                    try {
                        throw e.getCause();
                    } catch (NotFoundException notFoundE) {
                        ObjectNode message = Json.newObject();
                        message.put("message", "The given destination id is not found");
                        return notFound(message);
                    } catch (Throwable serverError) {
                        return internalServerError();
                    }
                });
    }

    /**
     * Endpoint to get all countries.
     *
     * @return A completion stage and status code 200 with countries in JSON body if successful, 500 for errors.
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> getCountries() {
        return destinationRepository.getCountries()
                .thenApplyAsync(countries -> {
                    JsonNode countriesJson = Json.toJson(countries);
                    return ok(countriesJson);
                }, httpExecutionContext.current())
                .exceptionally(e -> internalServerError());
    }

    /**
     * Endpoint to get destination types.
     *
     * @return A completion stage and status code 200 with destination types in body if successful, 500 for errors.
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> getDestinationTypes() {
        return destinationRepository.getDestinationTypes()
                .thenApplyAsync(destinationTypes -> {
                    JsonNode destinationTypesJson = Json.toJson(destinationTypes);
                    return ok(destinationTypesJson);
                }, httpExecutionContext.current())
                .exceptionally(e -> internalServerError());
    }

    /**
     * Endpoint to get all districts for a country.
     *
     * @param countryId country to get districts for.
     * @return A completion stage and status code of 200 with districts in the json body if successful.
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> getDistricts(int countryId) {
        return destinationRepository.getDistricts(countryId)
                .thenApplyAsync(districts -> {
                    JsonNode districtsJson = Json.toJson(districts);
                    return ok(districtsJson);
                }, httpExecutionContext.current());
    }

}

