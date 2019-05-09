package controllers;

import actions.LoggedIn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.NotFoundException;
import models.Country;
import models.Destination;
import models.DestinationType;
import models.District;
import play.libs.Json;
import play.mvc.*;
import repository.DestinationRepository;

import javax.inject.Inject;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import play.libs.concurrent.HttpExecutionContext;


/**
 * Controller to manage endpoints related to destinations.
 */
public class DestinationController  extends Controller{
    private final DestinationRepository destinationRepository;
    private HttpExecutionContext httpExecutionContext;

    @Inject
    public DestinationController(DestinationRepository destinationRepository, HttpExecutionContext httpExecutionContext) {
        this.destinationRepository = destinationRepository;
        this.httpExecutionContext = httpExecutionContext;
    }

    /**
     * A function that gets a list of all the destinations and returns it with a 200 ok code to the HTTP client
     * @param request Http.Request the http request
     * @return a completion stage and a status code 200 if the request is successful, otherwise returns 500.
     */
    public CompletionStage<Result> getDestinations(Http.Request request) {
        return destinationRepository.getDestinations()
                .thenApplyAsync(destinations -> ok(Json.toJson(destinations)), httpExecutionContext.current());
    }

    /**
     * A function that retrieves a destination details based on the destination ID given
     * @param destinationId the destination Id of the destination to retrieve
     * @param request request Object
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
     * Function to add destinations to the database
     * @param request the HTTP post request.
     * @return a completion stage with a 200 status code and the new json object or a status code 400.
     */

    public CompletionStage<Result> addDestination(Http.Request request) {
        JsonNode jsonRequest = request.body().asJson();

        // check that the request has a body
        if (jsonRequest == null) {
            return supplyAsync(() -> {
                ObjectNode message = Json.newObject();
                message.put("message", "Please provide a valid request body according to the API spec");
                return badRequest(message);
            });
        }

        try {
            String destinationName = jsonRequest.get("destinationName").asText();
            int destinationType = jsonRequest.get("destinationTypeId").asInt();
            int district = jsonRequest.get("districtId").asInt();
            Double latitude = jsonRequest.get("latitude").asDouble();
            Double longitude = jsonRequest.get("longitude").asDouble();
            int country = jsonRequest.get("countryId").asInt();

            DestinationType destinationTypeAdd = new DestinationType(null);
            destinationTypeAdd.setDestinationTypeId(destinationType);
            District districtAdd = new District(null, null);
            districtAdd.setDistrictId(district);
            Country countryAdd = new Country(null);
            countryAdd.setCountryId(country);
            Destination destination = new Destination(destinationName,destinationTypeAdd,districtAdd,
                    latitude,longitude,countryAdd);

            return destinationRepository.insert(destination)
                    .thenApplyAsync(insertedDestination -> created(Json.toJson(insertedDestination)), httpExecutionContext.current());
        } catch (Exception e) {
            ObjectNode message = Json.newObject();
            message.put("message", "Please provide a valid Destination with complete data");
            return supplyAsync(() -> badRequest(message));
        }

    }

    /**
     * Endpoint to update a destination's details
     * @param request Request body to get json body from
     * @param destinationId The destination ID to update
     * @return Returns status code 200 if successful, 404 if the destination isn't found, 500 for other errors.
     *
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> updateDestination(Http.Request request, int destinationId) {
       return destinationRepository.getDestinationById(destinationId)
       .thenComposeAsync(optionalDest -> {
        if (!optionalDest.isPresent()) {
            throw new CompletionException(new NotFoundException());
        }
        JsonNode jsonBody = request.body().asJson();

        String destinationName =  jsonBody.get("destinationName").asText();
        int destinationTypeId = jsonBody.get("destinationTypeId").asInt();
        int countryId = jsonBody.get("countryId").asInt();
        int districtId = jsonBody.get("districtId").asInt();
        double latitude = jsonBody.get("latitude").asDouble();
        double longitude = jsonBody.get("longitude").asDouble();

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

        return destinationRepository.update(destination);
        }, httpExecutionContext.current())
       .thenApplyAsync(destination -> (Result) ok(), httpExecutionContext.current())
       .exceptionally(error -> {
            try {
                throw error.getCause();
            } catch (NotFoundException notFoundE) {
                return notFound();
            } catch (Throwable ee) {
                return internalServerError();
            }
       });
    }

    /**
     * Endpoint to delete a destination given its id
     * @param destinationId the id of the destination that we want to delete
     * @param request the request sent by the routes file
     * @return Status code 200 if successful, 404 if not found, 500 otherwise.
     */
    @With(LoggedIn.class)
    public CompletionStage<Result> deleteDestination(int destinationId, Http.Request request) {
        return destinationRepository.getDestinationById(destinationId)
                .thenComposeAsync(optionalDestination -> {
                    if(!optionalDestination.isPresent()) {
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
