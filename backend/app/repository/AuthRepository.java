package repository;

import io.ebean.*;
import models.User;
import play.db.ebean.EbeanConfig;
import play.db.ebean.EbeanDynamicEvolutions;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Repository methods for authentication
 */
public class AuthRepository {
    private final EbeanServer ebeanServer;
    private final DatabaseExecutionContext executionContext;
    private final EbeanDynamicEvolutions ebeanDynamicEvolutions;

    @Inject
    public AuthRepository(EbeanConfig ebeanConfig, EbeanDynamicEvolutions ebeanDynamicEvolutions, DatabaseExecutionContext executionContext) {
        this.ebeanDynamicEvolutions = ebeanDynamicEvolutions;
        this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
        this.executionContext = executionContext;
    }

    /**
     * Add a new user
     * @param user The user to add
     * @return The added user
     */
    public CompletionStage<User> insert(User user) {
        return supplyAsync(() -> {
            user.save();
            return user;
        }, executionContext);
    }

    public CompletionStage<Optional<User>> getByToken(String token) {
        return supplyAsync(() -> {
           Optional<User> user = User.find.query().where().eq("token", token).findOneOrEmpty();
           return user;
        });
    }
}
