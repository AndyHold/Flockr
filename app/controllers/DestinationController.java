package controllers;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import actions.ActionState;
import actions.Admin;
import actions.LoggedIn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.BadRequestException;
import exceptions.ConflictingRequestException;
import exceptions.ForbiddenRequestException;
import exceptions.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.Country;
import models.Destination;
import models.DestinationPhoto;
import models.DestinationProposal;
import models.DestinationType;
import models.PersonalPhoto;
import models.TravellerType;
import models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.*;
import repository.DestinationRepository;
import repository.PhotoRepository;
import util.DestinationUtil;
import util.ExceptionUtil;
import util.Security;

/**
 * Controller to manage endpoints related to destinations.
 */
public class DestinationController extends Controller {
  private static final String MESSAGE_KEY = "message";
  private static final String TRAVELLER_TYPE_IDS_KEY = "travellerTypeIds";
  private final DestinationRepository destinationRepository;
  private final PhotoRepository photoRepository;
  private HttpExecutionContext httpExecutionContext;
  private final DestinationUtil destinationUtil;
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final ExceptionUtil exceptionUtil;



  @Inject
  public DestinationController(
      DestinationRepository destinationRepository,
      HttpExecutionContext httpExecutionContext,
      PhotoRepository photoRepository,
      DestinationUtil destinationUtil,
      ExceptionUtil exceptionUtil) {
    this.photoRepository = photoRepository;
    this.destinationRepository = destinationRepository;
    this.httpExecutionContext = httpExecutionContext;
    this.destinationUtil = destinationUtil;
    this.exceptionUtil = exceptionUtil;
  }

    /**
     * A function that queries the database to check if a given destination is being used in any trips
     *
     * @param destinationId the destination ID of the destination to check
     * @param request the play request object
     * @return true or false depending on whether the destination is used or not
     */
  @With(LoggedIn.class)
  public CompletionStage<Result> isDestinationUsed(int destinationId, Http.Request request) {
      return destinationRepository.isDestinationUsed(destinationId).thenApplyAsync(used ->
              ok(Json.toJson(used)));
  }

  /**
   * A function that gets a list of all the destinations and returns it with a 200 ok code to the
   * HTTP client
   *
   * @param request Http.Request the http request
   * @return a completion stage with status code - 200 - Got destinations successfully - 400 - Bad
   *     Request, e.g. was missing offset query parameter - 500 - Internal server error
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> getDestinations(Http.Request request) {
    String searchCriterion = request.getQueryString("search"); // optional
    String offsetString = request.getQueryString("offset");
    int offset;
    if (offsetString == null) {
      offsetString = "0";
    }

    try {
      offset = Integer.parseInt(offsetString);
    } catch (NumberFormatException e) {
      return supplyAsync(Results::badRequest, httpExecutionContext.current());
    }

    CompletionStage<List<Destination>> destinations;

    if (searchCriterion == null) {
      destinations = destinationRepository.getDestinations(offset);
    } else {
      destinations = destinationRepository.getDestinations(searchCriterion, offset);
    }

    return destinations.thenApplyAsync(
        allDestinations -> {
          List<Destination> publicDestinations =
              allDestinations.stream()
                  .filter(Destination::getIsPublic)
                  .collect(Collectors.toList());
          return ok(Json.toJson(publicDestinations));
        },
        httpExecutionContext.current());
  }

  /**
   * A function that retrieves a destination details based on the destination ID given
   *
   * @param destinationId the destination Id of the destination to retrieve
   * @param request request Object
   * @return a completion stage and a Status code of 200 and destination details as a Json object if
   *     successful, otherwise returns status code 404 if the destination can't be found in the db.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> getDestination(int destinationId, Http.Request request) {
    User user = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getDestinationById(destinationId)
        .thenApplyAsync(
            optionalDestination -> {
              if (!optionalDestination.isPresent()) {
                ObjectNode message = Json.newObject();
                message.put(MESSAGE_KEY, "No destination exists with the specified ID");
                return notFound(message);
              }

              Destination destination = optionalDestination.get();

              try {
                if (!user.isAdmin()
                    && destination.getDestinationOwner() != null
                    && destination.getDestinationOwner() != user.getUserId()
                    && !destination.getIsPublic()) {
                  ObjectNode message = Json.newObject();
                  message.put(MESSAGE_KEY, "You are not authorised to get this destination");
                  return forbidden(message);
                }
              } catch (NullPointerException e) {
                log.error(e.getMessage());
              }

              JsonNode destAsJson = Json.toJson(destination);

              return ok(destAsJson);
            },
            httpExecutionContext.current());
  }

  /**
   * Get the destinations owned by a user.
   * If the user is viewing their own destinations or is an admin, return all
   * their destinations.
   * If the user is viewing someone else's destinations, return just the public
   * destinations of the specified user.
   *
   * @param userId user to find destinations for.
   * @param request HTTP request.
   * @return 200 status code with the data for the user's destinations in body
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> getUserDestinations(int userId, Http.Request request) {
    User loggedInUser = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getUserDestinations(userId)
        .thenApplyAsync(
            destinations -> {
              log.info(String.format("is the logged in user an admin: %s", loggedInUser.isAdmin()));
              if (loggedInUser.getUserId() == userId || loggedInUser.isAdmin()) {
                return ok(Json.toJson(destinations));
              } else {
                List<Destination> userDestinations =
                    destinations.stream()
                        .filter(Destination::getIsPublic)
                        .collect(Collectors.toList());
                return ok(Json.toJson(userDestinations));
              }
            },
            httpExecutionContext.current());
  }

  /**
   * Function to add destinations to the database
   *
   * @param userId the userId of the user.
   * @param request the HTTP post request.
   * @return a completion stage with a 200 status code and the new json object or a status code 400.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> addDestination(int userId, Http.Request request) {
    JsonNode jsonRequest = request.body().asJson();
    User user = request.attrs().get(ActionState.USER);

    if (!user.isAdmin() && user.getUserId() != userId) {
      return supplyAsync(() -> forbidden("You don't have permission to create the destination"));
    }

    try {
      // check that the request has a body
      if (jsonRequest.isNull()) {
        throw new BadRequestException("No details received, please send a valid request.");
      }

      String destinationName;
      int destinationTypeId;
      String districtName;
      double latitude;
      double longitude;
      int countryId;
      JsonNode travellerTypeIds;

      try {
        destinationName = jsonRequest.get("destinationName").asText();
        destinationTypeId = jsonRequest.get("destinationTypeId").asInt();
        districtName = jsonRequest.get("districtName").asText();
        latitude = jsonRequest.get("latitude").asDouble();
        longitude = jsonRequest.get("longitude").asDouble();
        countryId = jsonRequest.get("countryId").asInt();
        travellerTypeIds = jsonRequest.get(TRAVELLER_TYPE_IDS_KEY);

      } catch (NullPointerException exception) {
        throw new BadRequestException("One or more fields are missing.");
      }

      DestinationType destinationTypeAdd = DestinationType.find.byId(destinationTypeId);
      Country countryAdd = Country.find.byId(countryId);

      if (destinationTypeAdd == null || countryAdd == null) {
        throw new BadRequestException("One of the fields you have selected does not exist.");
      }

      List<TravellerType> allTravellerTypes = TravellerType.find.all();
      List<TravellerType> travellerTypes =
          destinationUtil.transformTravellerTypes(travellerTypeIds, allTravellerTypes);

      Destination destinationToAdd =
          new Destination(
              destinationName,
              destinationTypeAdd,
              districtName,
              latitude,
              longitude,
              countryAdd,
              userId,
              travellerTypes,
              false);

      return destinationRepository
          .getDuplicateDestinations(destinationToAdd, userId)
          .thenComposeAsync(
              destinations -> {
                for (Destination dest : destinations) {
                  boolean ownsDestination =
                      dest.getDestinationOwner() != null && dest.getDestinationOwner() == userId;
                  if (dest.getIsPublic() || ownsDestination) {
                    throw new CompletionException(
                        new ConflictingRequestException("Destination already exists."));
                  }
                }
                return destinationRepository.insert(destinationToAdd);
              },
              httpExecutionContext.current())
          .thenApplyAsync(insertedDestination -> created(Json.toJson(insertedDestination)))
          .exceptionally(exceptionUtil::getResultFromError);

    } catch (BadRequestException e) {
      ObjectNode message = Json.newObject();
      message.put(MESSAGE_KEY, "Please provide a valid Destination with complete data");
      return supplyAsync(() -> badRequest(message));
    }
  }

  /**
   * Endpoint to update a destination's details
   *
   * @param request Request body to get json body from
   * @param destinationId The destination ID to update
   * @return Returns status code 200 if successful, 404 if the destination isn't found, 500 for
   *     other errors.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> updateDestination(Http.Request request, int destinationId) {
    User user = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getDestinationById(destinationId)
        .thenComposeAsync(
            optionalDest -> {
              if (!optionalDest.isPresent()) {
                throw new CompletionException(new NotFoundException());
              }

              // Checks that the user is either the admin or the owner of the photo to change
              // permission groups
              if (!user.isAdmin()
                  && (optionalDest.get().getDestinationOwner() == null
                      || user.getUserId() != optionalDest.get().getDestinationOwner())) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "You are unauthorised to update " + "this destination"));
              }

              JsonNode jsonBody = request.body().asJson();

              String destinationName = jsonBody.get("destinationName").asText();
              int destinationTypeId = jsonBody.get("destinationTypeId").asInt();
              int countryId = jsonBody.get("countryId").asInt();
              String districtName = jsonBody.get("districtName").asText();
              double latitude = jsonBody.get("latitude").asDouble();
              double longitude = jsonBody.get("longitude").asDouble();
              boolean isPublic = jsonBody.get("isPublic").asBoolean();
              JsonNode travellerTypeIds = jsonBody.get(TRAVELLER_TYPE_IDS_KEY);
              List<TravellerType> allTravellerTypes = TravellerType.find.all();
              List<TravellerType> travellerTypes =
                  destinationUtil.transformTravellerTypes(travellerTypeIds, allTravellerTypes);

              Destination destination = optionalDest.get();

              DestinationType destType = new DestinationType(null);
              destType.setDestinationTypeId(destinationTypeId);

              Country country = new Country(null, null, true);
              country.setCountryId(countryId);

              destination.setDestinationName(destinationName);
              destination.setDestinationType(destType);
              destination.setDestinationCountry(country);
              destination.setDestinationDistrict(districtName);
              destination.setDestinationLat(latitude);
              destination.setDestinationLon(longitude);
              destination.setIsPublic(isPublic);
              destination.setTravellerTypes(travellerTypes);

              List<Integer> duplicatedDestinationIds = new ArrayList<>();
              // Checks if destination is a duplicate destination
              return destinationRepository
                  .getDuplicateDestinations(destination, user.getUserId())
                  .thenComposeAsync(
                      destinations -> {
                        boolean exists;
                        boolean duplicate = false;
                        for (Destination dest : destinations) {
                          if (dest.getDestinationId() != destinationId
                              && destination.getIsPublic()) {
                            exists = destination.equals(dest);
                            // Checks if the destination found is private
                            if (exists && !dest.getIsPublic()) {
                              duplicatedDestinationIds.add(dest.getDestinationId());
                            } // Checks if the destination is a duplicate (both public)
                            else if (exists
                                && (dest.getIsPublic() && optionalDest.get().getIsPublic())) {
                              duplicate = true;
                            }
                          }
                        }

                        if (duplicate) {
                          throw new CompletionException(
                              new BadRequestException(
                                  "There is already a Destination with the following information"));
                        }

                        for (int destId : duplicatedDestinationIds) {
                          List<DestinationPhoto> photoDest =
                              DestinationPhoto.find
                                  .query()
                                  .where()
                                  .eq("destination_destination_id", destId)
                                  .findList();
                          destinationRepository.deleteDestination(destId);

                          for (DestinationPhoto photo : photoDest) {
                            photo.setDestination(destination);
                            destinationRepository.insertDestinationPhoto(photo);
                          }
                        }
                        return destinationRepository.update(destination);
                      });
            },
            httpExecutionContext.current())
        .thenApplyAsync(destination -> ok(Json.toJson(destination)), httpExecutionContext.current())
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Endpoint to delete a destination given its id
   *
   * @param destinationId the id of the destination that we want to delete
   * @param request the request sent by the routes file
   * @return Status code 200 if successful, 404 if not found, 500 otherwise.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> deleteDestination(int destinationId, Http.Request request) {
    User user = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getDestinationById(destinationId)
        .thenComposeAsync(
            optionalDestination -> {
              if (!optionalDestination.isPresent()) {
                throw new CompletionException(new NotFoundException());
              }
              Destination destination = optionalDestination.get();
              if (destination.getDestinationOwner() == null && !user.isAdmin()) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "You are not permitted to delete this destination"));
              }
              if (destination.getDestinationOwner() != null
                  && !user.isAdmin()
                  && user.getUserId() != destination.getDestinationOwner()) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "You are not permitted to delete this destination"));
              }
              ObjectNode success = Json.newObject();
              success.put(MESSAGE_KEY, "Successfully deleted the given destination id");
              return this.destinationRepository.deleteDestination(destination.getDestinationId());
            },
            httpExecutionContext.current())
        .thenApplyAsync(destId -> (Result) ok(), httpExecutionContext.current())
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Endpoint controller method for undoing a destination deletion. Return status codes are as
   * follows: - 200 - OK - successful undo. - 400 - Bad Request - The destination has not been
   * deleted. - 401 - Unauthorised - the user is not authorised. - 403 - Forbidden - The destination
   * does not have permission to undo this deletion. - 404 - Not Found - The destination cannot be
   * found.
   *
   * @param destinationId the id of the destination.
   * @param request the http request.
   * @return the completion stage containing the result.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> undoDeleteDestination(int destinationId, Http.Request request) {
    User userFromMiddleware = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getDestinationByIdIncludingSoftDelete(destinationId)
        .thenComposeAsync(
            optionalDestination -> {
              if (!optionalDestination.isPresent()) {
                throw new CompletionException(
                    new NotFoundException("The destination you are undoing does not exist."));
              }
              Destination destination = optionalDestination.get();
              if (!userFromMiddleware.isAdmin()
                  && userFromMiddleware.getUserId() != destination.getDestinationOwner()) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "You do not have permission to undo this deletion."));
              }
              if (!destination.isDeleted()) {
                throw new CompletionException(
                    new BadRequestException("This destination has not been deleted."));
              }
              return destinationRepository.undoDeletion(destination);
            })
        .thenApplyAsync(destination -> ok(Json.toJson(destination)))
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Endpoint to get all countries.
   *
   * @return A completion stage and status code 200 with countries in JSON body if successful, 500
   *     for errors.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> getCountries() {
    return destinationRepository
        .getCountries()
        .thenApplyAsync(
            countries -> {
              JsonNode countriesJson = Json.toJson(countries);
              return ok(countriesJson);
            },
            httpExecutionContext.current())
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Endpoint to get destination types.
   *
   * @return A completion stage and status code 200 with destination types in body if successful,
   *     500 for errors.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> getDestinationTypes() {
    return destinationRepository
        .getDestinationTypes()
        .thenApplyAsync(
            destinationTypes -> {
              JsonNode destinationTypesJson = Json.toJson(destinationTypes);
              return ok(destinationTypesJson);
            },
            httpExecutionContext.current())
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Adds a photo to a destination
   *
   * @param destinationId the destination to add a photo to
   * @param request the request object
   * @return - 400 If request body fields don't exist - 404 If the destination or photo could not be
   *     found - 403 If the user is not the owner of the destination - 201 Photo has been added
   *     successfully
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> addPhoto(int destinationId, Http.Request request) {
    User userFromMiddleware = request.attrs().get(ActionState.USER);
    JsonNode requestJson = request.body().asJson();
    int photoId;
    try {
      photoId = requestJson.get("photoId").asInt(0);
    } catch (NullPointerException e) {
      return supplyAsync(() -> badRequest("Please make sure to add the photoId"));
    }

    return destinationRepository
        .getDestinationById(destinationId)
        .thenComposeAsync(
            optionalDestination -> {
              if (!optionalDestination.isPresent()) {
                throw new CompletionException(new NotFoundException("Could not found destination"));
              }

              if (!optionalDestination.get().getIsPublic()
                  && !optionalDestination
                      .get()
                      .getDestinationOwner()
                      .equals(userFromMiddleware.getUserId())
                  && !userFromMiddleware.isAdmin()) {
                throw new CompletionException(new ForbiddenRequestException("Forbidden"));
              }

              return photoRepository
                  .getPhotoById(photoId)
                  .thenComposeAsync(
                      optionalPhoto -> {
                        if (!optionalPhoto.isPresent()) {
                          throw new CompletionException(
                              new NotFoundException("Could not find photo"));
                        }

                        if (!userFromMiddleware.isAdmin()
                            && optionalPhoto.get().getOwnerId() != userFromMiddleware.getUserId()) {
                          throw new CompletionException(
                              new ForbiddenRequestException("User does not own the image"));
                        }

                        if (optionalDestination.get().getIsPublic()
                            && // The destination is public
                            optionalDestination.get().getDestinationOwner() != null
                            && // The owner is not already null
                            !optionalDestination
                                .get()
                                .getDestinationOwner()
                                .equals(
                                    userFromMiddleware
                                        .getUserId())) { // The user doesn't own the destination
                          optionalDestination.get().setDestinationOwner(null);
                          destinationRepository.update(optionalDestination.get());
                        }

                        Destination destination = optionalDestination.get();
                        PersonalPhoto personalPhoto = optionalPhoto.get();
                        DestinationPhoto destinationPhoto =
                            new DestinationPhoto(destination, personalPhoto);

                        return destinationRepository.savePhoto(destinationPhoto);
                      })
                  .thenApplyAsync(
                      destinationPhoto -> {
                        JsonNode destinationPhotoJson = Json.toJson(destinationPhoto);
                        return created(destinationPhotoJson);
                      });
            })
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Get all the photos linked to this destination
   *
   * @param destinationId the id of the destination
   * @param request the HTTP request trying to get the destination photos
   * @return a response according to the API spec
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> getPhotos(int destinationId, Http.Request request) {

    ObjectNode res = Json.newObject();
    User user = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getDestinationById(destinationId)
        .thenApplyAsync(
            (optionalDestination -> {
              if (!optionalDestination.isPresent()) {
                res.put(MESSAGE_KEY, "Destination " + destinationId + " does not exist");
                return notFound(res);
              }
              Destination destination = optionalDestination.get();
              List<DestinationPhoto> destinationPhotos = destination.getDestinationPhotos();

              if (user.isAdmin() || user.isDefaultAdmin()) {
                return ok(Json.toJson(destinationPhotos));
              }

              List<DestinationPhoto> photosToReturn = new ArrayList<>();
              List<DestinationPhoto> publicDestinationPhotos =
                  destination.getPublicDestinationPhotos();
              List<DestinationPhoto> privatePhotosForUser =
                  destination.getPrivatePhotosForUserWithId(user.getUserId());
              photosToReturn.addAll(publicDestinationPhotos);
              photosToReturn.addAll(privatePhotosForUser);

              return ok(Json.toJson(photosToReturn));
            }),
            httpExecutionContext.current());
  }

  /**
   * Remove the association between a personal photo and a destination
   *
   * @param photoId the id of the photo
   * @param request the HTTP request
   * @return a response that complies with the API spec
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> deletePhoto(int destinationId, int photoId, Http.Request request) {
    User user = request.attrs().get(ActionState.USER);
    ObjectNode res = Json.newObject();

    return destinationRepository
        .getDestinationPhotoById(destinationId, photoId)
        .thenApplyAsync(
            optionalDestinationPhoto -> {
              // return 404 if destination photo is not found
              if (!optionalDestinationPhoto.isPresent()) {
                res.put(
                    MESSAGE_KEY,
                    String.format(
                        "Photo %d is not linked to destination %d", photoId, destinationId));
                return notFound(res);
              }

              // allow deletion of the destination photo to any admin
              DestinationPhoto destinationPhoto = optionalDestinationPhoto.get();
              if (user.isDefaultAdmin() || user.isAdmin()) {
                destinationRepository.deleteDestinationPhoto(destinationPhoto);
                res.put(MESSAGE_KEY, "The destination photo link was deleted");
                return ok(res);
              }

              // allow the photo's owner to delete the destination photo
              PersonalPhoto personalPhoto = destinationPhoto.getPersonalPhoto();
              if (personalPhoto.getUser().equals(user)) {
                destinationRepository.deleteDestinationPhoto(destinationPhoto);
                res.put(MESSAGE_KEY, "The destination photo link was deleted");
                return ok(res);
              }

              // else tell the user they are not allowed :P
              res.put(
                  MESSAGE_KEY,
                  String.format(
                      "User %d is not allowed to delete destination photo %d",
                      user.getUserId(), photoId));
              return forbidden(res);
            },
            httpExecutionContext.current());
  }

  /**
   * Endpoint controller method for undoing a personal photo deletion. return status codes are as
   * follows: - 200 - OK - successful undo. - 400 - Bad Request - The photo has not been deleted. -
   * 401 - Unauthorised - the user is not authorised. - 403 - Forbidden - The user does not have
   * permission to undo this deletion. - 404 - Not Found - The photo cannot be found.
   *
   * @param photoId the id of the photo.
   * @param request the http request.
   * @return the completion stage containing the result.
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> undoPhotoDelete(
      int destinationId, int photoId, Http.Request request) {
    User user = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getPhotoByIdWithSoftDelete(destinationId, photoId)
        .thenComposeAsync(
            optionalPhoto -> {
              if (!optionalPhoto.isPresent()) {
                throw new CompletionException(new NotFoundException("Photo not found"));
              }
              DestinationPhoto photo = optionalPhoto.get();
              PersonalPhoto personalPhoto = photo.getPersonalPhoto();
              if (!user.isAdmin() && user.getUserId() != personalPhoto.getOwnerId()) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "You do not have permission to undo this deletion"));
              }
              if (!photo.isDeleted()) {
                throw new CompletionException(
                    new BadRequestException("This photo has not been deleted"));
              }
              return destinationRepository.undoDeleteDestinationPhoto(photo);
            })
        .thenApplyAsync(photo -> ok(Json.toJson(photo)))
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Allows a user to add a proposal
   *
   * @param request Request object to get user object
   * @param destinationId ID of destination to propose to
   * @return A response that complies with the API spec
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> addProposal(int userId, Http.Request request, int destinationId) {
    User userFromMiddleware = request.attrs().get(ActionState.USER);

    User user = User.find.byId(userId);

    if (Security.userHasPermission(userFromMiddleware, userId)) {
      return supplyAsync(Controller::forbidden);
    }

    if (user == null) {
      return supplyAsync(() -> notFound("User not found"));
    }

    return destinationRepository
        .getDestinationById(destinationId)
        .thenComposeAsync(
            optionalDestination -> {
              if (!optionalDestination.isPresent()) {
                throw new CompletionException(new BadRequestException("Destination not found"));
              }

              Destination destination = optionalDestination.get();

              // Cannot make proposals for destinations that are not public
              if (!destination.getIsPublic()) {
                throw new CompletionException(
                    new ForbiddenRequestException("Destination is not public"));
              }

              // Get traveller type objects from ID's
              JsonNode travellerTypeIds = request.body().asJson().get(TRAVELLER_TYPE_IDS_KEY);
              List<TravellerType> allTravellerTypes = TravellerType.find.all();
              List<TravellerType> travellerTypes =
                  destinationUtil.transformTravellerTypes(travellerTypeIds, allTravellerTypes);

              DestinationProposal proposal =
                  new DestinationProposal(destination, travellerTypes, user);
              return destinationRepository.createProposal(proposal);
            })
        .thenApplyAsync(proposal -> ok(Json.toJson(proposal)))
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Function to handle the request for modifying a destination proposal
   *
   * @param destinationProposalId the id of the destination proposal to modify
   * @param request the HTTP request object containing a list of traveller type ids
   * @return A response that complies with the API spec http status codes: - 200 - OK - Successfully
   *     updated proposal. - 400 - Bad Request - Request body incorrect. - 404 - Not Found -
   *     Destination proposal not found.
   */
  @With({LoggedIn.class, Admin.class})
  public CompletionStage<Result> modifyProposal(int destinationProposalId, Http.Request request) {
    return destinationRepository
        .getDestinationProposalById(destinationProposalId)
        .thenComposeAsync(
            optionalDestinationProposal -> {
              if (!optionalDestinationProposal.isPresent()) {
                throw new CompletionException(
                    new NotFoundException("Destination proposal not found"));
              }
              DestinationProposal destinationProposal = optionalDestinationProposal.get();
              JsonNode travellerTypeIds = request.body().asJson().get(TRAVELLER_TYPE_IDS_KEY);
              List<TravellerType> allTravellerTypes = TravellerType.find.all();
              List<TravellerType> travellerTypes =
                  destinationUtil.transformTravellerTypes(travellerTypeIds, allTravellerTypes);

              destinationProposal.setTravellerTypes(travellerTypes);
              return destinationRepository.updateDestinationProposal(destinationProposal);
            })
        .thenApplyAsync(destinationProposal -> ok(Json.toJson(destinationProposal)))
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Allows an admin to accept a traveller type proposal change
   *
   * @param destinationProposalId Id of destination prosposal to accept
   * @return A response that complies with the API spec
   */
  @With({LoggedIn.class, Admin.class})
  public CompletionStage<Result> acceptProposal(int destinationProposalId) {
    return destinationRepository
        .getDestinationProposalById(destinationProposalId)
        .thenApplyAsync(
            optionalDestinationProposal -> {
              if (!optionalDestinationProposal.isPresent()) {
                return notFound("Proposal could not be found");
              }
              DestinationProposal destinationProposal = optionalDestinationProposal.get();

              Destination destination = destinationProposal.getDestination();
              destination.setTravellerTypes(destinationProposal.getTravellerTypes());
              destination.update();
              destinationProposal.delete();
              return ok();
            });
  }

  /**
   * A function that allows an admin to get the destination proposal by the given ID
   *
   * @param destinationProposalId the ID of the destination proposal to be retrieved
   * @return A response that complies with the API spec
   */
  @With({LoggedIn.class, Admin.class})
  public CompletionStage<Result> getProposalById(int destinationProposalId) {
    return destinationRepository
        .getDestinationProposalById(destinationProposalId)
        .thenApplyAsync(
            optionalDestinationProposal -> {
              if (!optionalDestinationProposal.isPresent()) {
                ObjectNode message = Json.newObject();
                message.put(MESSAGE_KEY, "The proposal with the given ID does not exist.");
                return notFound(message);
              }
              DestinationProposal destinationProposal = optionalDestinationProposal.get();
              return ok(Json.toJson(destinationProposal));
            });
  }

  /**
   * Allows an admin to reject a destination proposal
   *
   * @param destinationProposalId the ID of the proposal to reject
   * @return A response that complies with the API spec
   */
  @With({LoggedIn.class})
  public CompletionStage<Result> rejectProposal(
      int userId, int destinationProposalId, Http.Request request) {
    User userFromMiddleware = request.attrs().get(ActionState.USER);

    User user = User.find.byId(userId);
    if (Security.userHasPermission(userFromMiddleware, userId)) {
      return supplyAsync(() -> forbidden("Not authorized to reject proposal"));
    }

    if (user == null) {
      return supplyAsync(() -> notFound("User not found"));
    }

    return destinationRepository
        .getDestinationProposalById(destinationProposalId)
        .thenComposeAsync(
            optionalDestinationProposal -> {
              if (!optionalDestinationProposal.isPresent()) {
                throw new CompletionException(
                    new NotFoundException(
                        "The destination proposal you want to reject does not exist."));
              }
              DestinationProposal destinationProposal = optionalDestinationProposal.get();

              if (!user.isAdmin()
                  && destinationProposal.getUser().getUserId() != user.getUserId()) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "User is not permitted to reject other proposals"));
              }

              ObjectNode success = Json.newObject();
              success.put(MESSAGE_KEY, "Successfully rejected the given destination proposal");
              return this.destinationRepository.deleteDestinationProposal(destinationProposal);
            },
            httpExecutionContext.current())
        .thenApplyAsync(destinationProposal -> (Result) ok(), httpExecutionContext.current())
        .exceptionally(exceptionUtil::getResultFromError);
  }

  /**
   * Allows an admin to get all the destination proposals on a given page
   *
   * @param request the Http request object containing the query string for the page to retrieve
   * @return A response that complies with the API spec
   */
  @With({LoggedIn.class, Admin.class})
  public CompletionStage<Result> getProposals(Http.Request request) {
    int page = 1;
    try {
      String pageString = request.getQueryString("page");
      page = Integer.parseInt(pageString);
    } catch (Exception e) {
      log.info("No page provided, using default of 1");
    }
    return destinationRepository
        .getDestinationProposals(page)
        .thenApplyAsync(destinationProposals -> ok(Json.toJson(destinationProposals)));
  }

  /**
   * The method that undoes the deletion of a destination proposal The following are the status
   * codes: - 200 - OK - successful undo. - 400 - Bad Request - The destination proposal has not
   * been deleted. - 401 - Unauthorised - the user is not authorised. - 403 - Forbidden - The user
   * does not have permission to undo this deletion. - 404 - Not Found - The destination proposal
   * cannot be found.
   *
   * @param destinationProposalId the id of the destination proposal to undo deletion of
   * @param request the http request
   * @return the completion stage containing the result
   */
  @With(LoggedIn.class)
  public CompletionStage<Result> undoDeleteDestinationProposal(
      int destinationProposalId, Http.Request request) {
    User user = request.attrs().get(ActionState.USER);
    return destinationRepository
        .getDestinationProposalByIdWithSoftDelete(destinationProposalId)
        .thenComposeAsync(
            optionalDestinationProposal -> {
              if (!optionalDestinationProposal.isPresent()) {
                throw new CompletionException(
                    new NotFoundException(
                        "The destination proposal you are undoing does not exist."));
              }
              DestinationProposal destinationProposal = optionalDestinationProposal.get();

              if (!user.isAdmin()
                  && destinationProposal.getUser().getUserId() != user.getUserId()) {
                throw new CompletionException(
                    new ForbiddenRequestException(
                        "You do not have the permission to undo the deletion."));
              }

              if (!destinationProposal.isDeleted()) {
                throw new CompletionException(
                    new BadRequestException("This destination proposal has not been deleted."));
              }
              return destinationRepository.undoDestinationProposalDelete(destinationProposal);
            })
        .thenApplyAsync(destinationProposal -> ok(Json.toJson(destinationProposal)))
        .exceptionally(exceptionUtil::getResultFromError);
  }
}
