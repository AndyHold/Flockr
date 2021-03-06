package testingUtilities;


import models.Destination;
import models.Role;
import models.User;
import play.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used to reuse common Cucumber steps between different Cucumber scenarios. Allows to hold common state between
 * scenarios.
 */
public class TestState {

    private Application application;
    private static TestState testState;
    private FakeClient fakeClient;
    private Role superAdminRole;
    private Role adminRole;
    private Role travellerRole;
    private List<Destination> destinations;
    private List<User> users;
    Map<String, String> userData;

    /**
     * Constructor to create any lists of objects to be used in multiple classes
     */
    public TestState() {
        this.users = new ArrayList<>();
        this.destinations = new ArrayList<>();
    }

    /**
     * Get an instance of this class, creates it if necessary.
     *
     * @return the instance of this class.
     */
    public static TestState getInstance() {
        if (testState == null) {
            testState = new TestState();
        }
        return testState;
    }

    /**
     * Clears current test state to wipe saved data.
     */
    public static void clear() {
        testState = null;

    }

    public Map<String, String> getUserData() {
        return userData;
    }

    public void setUserData(Map<String, String> userData) {
        this.userData = userData;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public FakeClient getFakeClient() {
        return fakeClient;
    }

    public void setFakeClient(FakeClient fakeClient) {
        this.fakeClient = fakeClient;
    }

    public Role getSuperAdminRole() {
        return superAdminRole;
    }

    public void setSuperAdminRole(Role superAdminRole) {
        this.superAdminRole = superAdminRole;
    }

    public Role getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(Role adminRole) {
        this.adminRole = adminRole;
    }

    public Role getTravellerRole() {
        return travellerRole;
    }

    public void setTravellerRole(Role travellerRole) {
        this.travellerRole = travellerRole;
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public User getUser(int index) {
        return this.users.get(index);
    }

    public User removeUser(int index) {
        return this.users.remove(index);
    }

    public void addDestination(Destination destination) {
        this.destinations.add(destination);
    }

    public Destination getDestination(int index) {
        return this.destinations.get(index);
    }

    public Destination removeDestination(int index) {
        return this.destinations.remove(index);
    }

    public List<Destination> getDestinations() {
        return destinations;
    }


}
