package controllers;

import exceptions.FailedToSignUpException;
import exceptions.ServerErrorException;
import models.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.libs.Json;
import play.mvc.Result;
import play.test.Helpers;
import utils.FakeClient;
import utils.FakePlayClient;
import utils.PlayResultToJson;
import utils.TestState;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class DestinationControllerTest {

    Application application;
    FakeClient fakeClient;
    User user;
    User otherUser;
    User adminUser;
    Destination destination;
    DestinationProposal destinationProposal;

    @Before
    public void setUp() throws ServerErrorException, IOException, FailedToSignUpException {
        Map<String, String> testSettings = new HashMap<>();
        testSettings.put("db.default.driver", "org.h2.Driver");
        testSettings.put("db.default.url", "jdbc:h2:mem:testdb;MODE=MySQL;");
        testSettings.put("play.evolutions.db.default.enabled", "true");
        testSettings.put("play.evolutions.db.default.autoApply", "true");
        testSettings.put("play.evolutions.db.default.autoApplyDowns", "true");

        application = Helpers.fakeApplication(testSettings);
        Helpers.start(application);

        // Make some users
        fakeClient = new FakePlayClient(application);
        user = fakeClient.signUpUser("Timmy", "Tester", "timmy@tester.com",
                "abc123");
        user = User.find.byId(user.getUserId());
        otherUser = fakeClient.signUpUser("Tammy", "Tester", "tammy@tester.com",
                "abc123");
        adminUser = fakeClient.signUpUser("Andy", "Admin", "andy@admin.com",
                "abc123");

        // Add admin role for admin user
        Role role = new Role(RoleType.ADMIN);
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        role.save();
        adminUser = User.find.byId(adminUser.getUserId());
        adminUser.setRoles(roles);
        adminUser.save();

        // Add some destinations
        DestinationType destinationType = new DestinationType("city");
        Country country = new Country("Peru", "PE", true);
        District district = new District("Test District", country);
        destination = new Destination("Test City", destinationType, district,
                0.0, 0.0, country, user.getUserId(), new ArrayList<>(), true);

        destinationType.save();
        country.save();
        district.save();
        destination.save();

        // Add some proposal
        TravellerType travellerType = new TravellerType("Gap Year");
        List<TravellerType> travellerTypes = new ArrayList<>();
        travellerTypes.add(travellerType);
        destinationProposal = new DestinationProposal(destination, travellerTypes, user);
        destinationProposal.save();
    }

    @After
    public void tearDown() {
        Helpers.stop(application);
        TestState.clear();
    }

    @Test
    public void undoDeleteGood() {
        destination.delete();
        Optional<Destination> optionalDestination = Destination.find.query()
                .where().eq("destination_id", destination.getDestinationId()).findOneOrEmpty();
        Assert.assertFalse(optionalDestination.isPresent());

        undoDeleteDestination(user.getToken(), destination.getDestinationId(), 200);
    }

    @Test
    public void undoDeleteGoodAdmin() {
        destination.delete();
        Optional<Destination> optionalDestination = Destination.find.query()
                .where().eq("destination_id", destination.getDestinationId()).findOneOrEmpty();
        Assert.assertFalse(optionalDestination.isPresent());
        undoDeleteDestination(adminUser.getToken(), destination.getDestinationId(), 200);
    }

    @Test
    public void undoDeleteBadRequest() {
        undoDeleteDestination(user.getToken(), destination.getDestinationId(), 400);
    }

    @Test
    public void undoDeleteUnauthorised() {
        destination.delete();
        Result result = fakeClient.makeRequestWithNoToken(
                "PUT",
                "/api/destinations/" + destination.getDestinationId() + "/undodelete");
        Assert.assertEquals(401, result.status());
    }

    @Test
    public void undoDeleteForbidden() {
        destination.delete();
        Optional<Destination> optionalDestination = Destination.find.query()
                .where().eq("destination_id", destination.getDestinationId()).findOneOrEmpty();
        Assert.assertFalse(optionalDestination.isPresent());
        undoDeleteDestination(otherUser.getToken(), destination.getDestinationId(), 403);
    }

    @Test
    public void undoDeleteNotFound() {
        undoDeleteDestination(user.getToken(), 10000000, 404);
    }

    /**
     * Calls the api to undo a deletion of a destination and checks the result.
     *
     * @param token the auth token to send with the request.
     * @param destinationId the destination id.
     * @param statusCode the status code of the result.
     */
    private void undoDeleteDestination(String token, int destinationId, int statusCode) {
        Result result = fakeClient.makeRequestWithToken(
                "PUT",
                "/api/destinations/" + destinationId + "/undodelete",
                token);
        Assert.assertEquals(statusCode, result.status());

        if (statusCode == 200) {
            Optional<Destination> optionalDestination = Destination.find.query()
                    .where().eq("destination_id", destination.getDestinationId()).findOneOrEmpty();
            Assert.assertTrue(optionalDestination.isPresent());
        }
    }

    @Test
    public void undoDeleteProposalGoodUser() {
        undoDeleteDestinationProposal(user.getToken(), destinationProposal.getDestinationProposalId(), 200, true);
    }

    @Test
    public void undoDeleteProposalAdmin() {
        undoDeleteDestinationProposal(adminUser.getToken(), destinationProposal.getDestinationProposalId(), 200, true);
    }

    @Test
    public void undoDeleteProposalForbidden() {
        undoDeleteDestinationProposal(otherUser.getToken(), destinationProposal.getDestinationProposalId(), 403, true);
    }

    @Test
    public void undoDeleteProposalUnauthorised() {
        Result result = fakeClient.makeRequestWithNoToken(
                "PUT",
                "/api/destinations/proposals/" + destinationProposal.getDestinationProposalId() + "/undoReject");
        Assert.assertEquals(401, result.status());

    }

    @Test
    public void undoDeleteProposalNotFound() {
        undoDeleteDestinationProposal(user.getToken(), 999999999, 404, true);
    }

    @Test
    public void undoDeleteProposalBadRequest() {
        undoDeleteDestinationProposal(user.getToken(), destinationProposal.getDestinationProposalId(), 400, false);
    }


    private void undoDeleteDestinationProposal(String token, int destinationProposalId, int statusCode, boolean deleted) {
        if (deleted) {
            destinationProposal.delete();

            Optional<DestinationProposal> optionalDestinationProposal = DestinationProposal.find.query()
                    .where().eq("destination_proposal_id", destinationProposalId).findOneOrEmpty();
            Assert.assertFalse(optionalDestinationProposal.isPresent());
        }

        Result result = fakeClient.makeRequestWithToken(
                "PUT",
                "/api/destinations/proposals/" + destinationProposalId + "/undoReject",
                token);
        Assert.assertEquals(statusCode, result.status());

        if (statusCode == 200) {
            Optional<DestinationProposal> optionalDestinationProposal = DestinationProposal.find.query()
                    .where().eq("destination_proposal_id", destinationProposalId).findOneOrEmpty();
            Assert.assertTrue(optionalDestinationProposal.isPresent());
        }
    }

    @Test
    public void getProposalAdmin()  {
        getProposalById(adminUser.getToken(), destinationProposal.getDestinationProposalId(), 200);
    }

    @Test
    public void getProposalUnauthorised() {
        getProposalById(user.getToken(), destinationProposal.getDestinationProposalId(), 401);
    }

    @Test
    public void getProposalNotFound() {
        getProposalById(adminUser.getToken(), 9999999, 404);
    }

    private void getProposalById(String token, int destinationProposalId, int statusCode) {
        Result result = fakeClient.makeRequestWithToken(
                "GET",
                "/api/destinations/proposals/" + destinationProposalId,
                token);
        Assert.assertEquals(statusCode, result.status());
    }

    @Test
    public void rejectProposalGoodUser() {
        rejectProposal(user.getToken(), destinationProposal.getDestinationProposalId(), user.getUserId(), 200);
    }

    @Test
    public void rejectProposalAdmin() {
        rejectProposal(adminUser.getToken(), destinationProposal.getDestinationProposalId(), adminUser.getUserId(), 200);
    }

    @Test
    public void rejectProposalUnauthorised() {
        Result result = fakeClient.makeRequestWithNoToken(
                "DELETE",
                "/api/users/" + user.getUserId() + "/destinations/proposals/" + destinationProposal.getDestinationProposalId());
        Assert.assertEquals(401, result.status());
    }

    @Test
    public void rejectProposalForbidden() {
        rejectProposal(otherUser.getToken(), destinationProposal.getDestinationProposalId(), user.getUserId(), 403);
    }

    private void rejectProposal(String token, int destinationProposalId, int userId, int statusCode) {
        Result result = fakeClient.makeRequestWithToken(
                "DELETE",
                "/api/users/" + userId + "/destinations/proposals/" + destinationProposalId,
                token);
        Assert.assertEquals(statusCode, result.status());
    }

    @Test
    public void acceptProposalsAdmin() {
        acceptProposals(adminUser.getToken(), destinationProposal.getDestinationProposalId(), 200);
        Optional<DestinationProposal> optionalDestinationProposal = DestinationProposal.find.query()
                .where().eq("destination_proposal_id", destinationProposal.getDestinationProposalId()).findOneOrEmpty();
        Assert.assertFalse(optionalDestinationProposal.isPresent());
    }

    @Test
    public void acceptProposalsUnauthorised() {
        acceptProposals(user.getToken(), destinationProposal.getDestinationProposalId(), 401);
    }

    public void acceptProposals(String token, int destinationProposalId, int statusCode) {
        Result result = fakeClient.makeRequestWithToken(
                "PATCH",
                "/api/destinations/proposals/" + destinationProposalId,
                token);
        Assert.assertEquals(statusCode, result.status());
    }
}