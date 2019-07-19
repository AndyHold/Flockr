package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.BadRequestException;
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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TreasureHuntControllerTest {

    @Inject
    private Application application;
    private User user;
    private User otherUser;
    private User adminUser;
    private FakeClient fakeClient;
    private TreasureHunt treasureHunt;
    private Destination editedDestination;

    @Before
    public void setUp() throws ServerErrorException, IOException, FailedToSignUpException, BadRequestException {
        Map<String, String> testSettings = new HashMap<>();
        testSettings.put("db.default.driver", "org.h2.Driver");
        testSettings.put("db.default.url", "jdbc:h2:mem:testdb;MODE=MySQL;");
        testSettings.put("play.evolutions.db.default.enabled", "true");
        testSettings.put("play.evolutions.db.default.autoApply", "true");
        testSettings.put("play.evolutions.db.default.autoApplyDowns", "true");

        application = Helpers.fakeApplication(testSettings);
        Helpers.start(application);

        TestState.getInstance().setApplication(application);
        TestState.getInstance().setFakeClient(new FakePlayClient(application));

        fakeClient = TestState.getInstance().getFakeClient();
        user = fakeClient.signUpUser("Timmy", "Tester", "timmy@tester.com", "abc123");
        otherUser = fakeClient.signUpUser("Tammy", "Tester", "tammy@tester.com", "abc123");
        adminUser = fakeClient.signUpUser("Andy", "Admin", "andy@admin.com", "abc123");

        Role role = new Role(RoleType.ADMIN);
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        role.save();
        adminUser = User.find.byId(adminUser.getUserId());
        adminUser.setRoles(roles);
        adminUser.save();

        DestinationType destinationType = new DestinationType("city");
        Country country = new Country("Test Nation");
        District district = new District("Test District", country);
        Destination destination = new Destination("Test City", destinationType, district, 0.0, 0.0, country, user.getUserId(), true);
        editedDestination = new Destination("Edited Destination", destinationType, district, 0.0, 0.0, country, user.getUserId(), false);

        destinationType.save();
        country.save();
        district.save();
        editedDestination.save();
        destination.save();

        treasureHunt = new TreasureHunt("Test Hunt",
                user.getUserId(),
                destination.getDestinationId(),
                "Its a test",
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(Duration.ofDays(30))));
        treasureHunt.save();
    }

    @After
    public void tearDown() {
        Application application = TestState.getInstance().getApplication();
        Helpers.stop(application);
        TestState.clear();
    }

    @Test
    public void editTreasureHuntGoodName() throws IOException {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("treasureHuntName", "Edited Hunt");
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(200, result.status());
        JsonNode jsonNode = PlayResultToJson.convertResultToJson(result);

        //Assert response is correct.
        Assert.assertTrue(jsonNode.has("treasureHuntName"));
        Assert.assertEquals("Edited Hunt", jsonNode.get("treasureHuntName").asText());

        //Assert database has been updated correctly.
        TreasureHunt editedTreasureHunt = TreasureHunt.find.byId(treasureHunt.getTreasureHuntId());
        Assert.assertNotNull(editedTreasureHunt);
        Assert.assertEquals("Edited Hunt", editedTreasureHunt.getTreasureHuntName());
    }

    @Test
    public void editTreasureHuntGoodDestinationId() throws IOException {

        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("treasureHuntDestinationId", editedDestination.getDestinationId());
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(200, result.status());
        JsonNode jsonNode = PlayResultToJson.convertResultToJson(result);

        //Assert response is correct.
        Assert.assertTrue(jsonNode.has("treasureHuntDestinationId"));
        Assert.assertEquals(editedDestination.getDestinationId(), jsonNode.get("treasureHuntDestinationId").asInt());

        //Assert database has been updated correctly.
        TreasureHunt editedTreasureHunt = TreasureHunt.find.byId(treasureHunt.getTreasureHuntId());
        Assert.assertNotNull(editedTreasureHunt);
        Assert.assertEquals(editedDestination.getDestinationId(), editedTreasureHunt.getTreasureHuntDestinationId());
    }

    @Test
    public void editTreasureHuntGoodRiddle() throws IOException {

        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("riddle", "New Riddle");
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(200, result.status());
        JsonNode jsonNode = PlayResultToJson.convertResultToJson(result);

        //Assert response is correct.
        Assert.assertTrue(jsonNode.has("riddle"));
        Assert.assertEquals("New Riddle", jsonNode.get("riddle").asText());

        //Assert database has been updated correctly.
        TreasureHunt editedTreasureHunt = TreasureHunt.find.byId(treasureHunt.getTreasureHuntId());
        Assert.assertNotNull(editedTreasureHunt);
        Assert.assertEquals("New Riddle", editedTreasureHunt.getRiddle());
    }

    @Test
    public void editTreasureHuntGoodStartDate() throws IOException, ParseException {

        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String newDate = dateFormat.format(Date.from(Instant.now().minus(Duration.ofDays(365))));
        treasureHuntObject.put("startDate", newDate);
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(200, result.status());
        JsonNode jsonNode = PlayResultToJson.convertResultToJson(result);

        //Assert response is correct.
        Assert.assertTrue(jsonNode.has("startDate"));
        Assert.assertEquals((dateFormat.parse(newDate)).toInstant().getEpochSecond() * 1000, jsonNode.get("startDate").asLong());

        //Assert database has been updated correctly.
        TreasureHunt editedTreasureHunt = TreasureHunt.find.byId(treasureHunt.getTreasureHuntId());
        Assert.assertNotNull(editedTreasureHunt);
        Assert.assertEquals(dateFormat.parse(newDate), editedTreasureHunt.getStartDate());
    }

    @Test
    public void editTreasureHuntGoodEndDate() throws IOException, ParseException {

        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String newDate = dateFormat.format(Date.from(Instant.now().plus(Duration.ofDays(365))));
        treasureHuntObject.put("endDate", newDate);
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(200, result.status());
        JsonNode jsonNode = PlayResultToJson.convertResultToJson(result);

        //Assert response is correct.
        Assert.assertTrue(jsonNode.has("endDate"));
        Assert.assertEquals((dateFormat.parse(newDate)).toInstant().getEpochSecond() * 1000, jsonNode.get("endDate").asLong());

        //Assert database has been updated correctly.
        TreasureHunt editedTreasureHunt = TreasureHunt.find.byId(treasureHunt.getTreasureHuntId());
        Assert.assertNotNull(editedTreasureHunt);
        Assert.assertEquals(dateFormat.parse(newDate), editedTreasureHunt.getEndDate());
    }

    @Test
    public void editTreasureHuntGoodNameAdmin() throws IOException {

        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("treasureHuntName", "Admin Edited Hunt");
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), adminUser.getToken());
        //Assert response code is correct.
        Assert.assertEquals(200, result.status());
        JsonNode jsonNode = PlayResultToJson.convertResultToJson(result);

        //Assert response is correct.
        Assert.assertTrue(jsonNode.has("treasureHuntName"));
        Assert.assertEquals("Admin Edited Hunt", jsonNode.get("treasureHuntName").asText());

        //Assert database has been updated correctly.
        TreasureHunt editedTreasureHunt = TreasureHunt.find.byId(treasureHunt.getTreasureHuntId());
        Assert.assertNotNull(editedTreasureHunt);
        Assert.assertEquals("Admin Edited Hunt", editedTreasureHunt.getTreasureHuntName());
    }

    @Test
    public void editTreasureHuntBadName() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("treasureHuntName", "");
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(400, result.status());
    }

    @Test
    public void editTreasureHuntBadDestinationId() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("treasureHuntDestinationId", 10000);
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(400, result.status());
    }

    @Test
    public void editTreasureHuntBadRiddle() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("riddle", "");
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(400, result.status());
    }

    @Test
    public void editTreasureHuntBadStartDate() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("startDate", dateFormat.format(Date.from(treasureHunt.getEndDate().toInstant().plus(Duration.ofDays(1)))));
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(400, result.status());
    }

    @Test
    public void editTreasureHuntBadEndDate() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("endDate", dateFormat.format(Date.from(treasureHunt.getStartDate().toInstant().minus(Duration.ofDays(1)))));
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(400, result.status());
    }

    @Test
    public void editTreasureHuntBadStartAndEndDate() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("endDate", dateFormat.format(treasureHunt.getStartDate()));
        treasureHuntObject.put("startDate", dateFormat.format(treasureHunt.getEndDate()));
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(400, result.status());
    }

    @Test
    public void editTreasureHuntNotFound() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("endDate", dateFormat.format(treasureHunt.getStartDate()));
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + 1000, user.getToken());
        //Assert response code is correct.
        Assert.assertEquals(404, result.status());
    }

    @Test
    public void editTreasureHuntUnauthorized() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("endDate", dateFormat.format(treasureHunt.getEndDate()));
        Result result = fakeClient.makeRequestWithNoToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId());
        //Assert response code is correct.
        Assert.assertEquals(401, result.status());
    }

    @Test
    public void editTreasureHuntForbidden() {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ObjectNode treasureHuntObject = Json.newObject();
        treasureHuntObject.put("endDate", dateFormat.format(treasureHunt.getEndDate()));
        Result result = fakeClient.makeRequestWithToken("PUT", treasureHuntObject,
                "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), otherUser.getToken());
        //Assert response code is correct.
        Assert.assertEquals(403, result.status());
    }

    @Test
    public void deleteTreasureHuntGood() {
        deleteTreasureHuntAsUser(user);
    }

    @Test
    public void deleteTreasureHuntGoodAdmin() {
        deleteTreasureHuntAsUser(adminUser);
    }

    private void deleteTreasureHuntAsUser(User adminUser) {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        Result result = fakeClient.makeRequestWithToken("DELETE", "/api/treasurehunts/" + treasureHunt.getTreasureHuntId(), adminUser.getToken());
        Assert.assertEquals(200, result.status());

        Optional<TreasureHunt> optionalTreasureHunt = TreasureHunt.find.query().where().eq(
                "treasure_hunt_id", this.treasureHunt.getTreasureHuntId()).findOneOrEmpty();
        Assert.assertFalse(optionalTreasureHunt.isPresent());
    }
}