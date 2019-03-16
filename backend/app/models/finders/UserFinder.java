package models.finders;


import io.ebean.Finder;
import models.User;

public class UserFinder extends Finder<Integer, User> {

    /**
     * Construct using the default EbeanServer.
     */
    public UserFinder() {
        super(User.class);
    }
}
