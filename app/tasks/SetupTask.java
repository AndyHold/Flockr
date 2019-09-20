package tasks;

import static java.util.concurrent.CompletableFuture.runAsync;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.ebean.Ebean;
import io.ebean.SqlUpdate;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import play.Environment;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

public class SetupTask {

  private final ActorSystem actorSystem;
  private final ExecutionContext executionContext;
  private final Environment environment;

  @Inject
  public SetupTask(
      ActorSystem actorSystem, ExecutionContext executionContext, Environment environment) {
    this.actorSystem = actorSystem;
    this.executionContext = executionContext;
    this.environment = environment;
    this.initialise();
  }

  private void initialise() {

    if (environment.isDev()) {
      return;
    }

    this.actorSystem
        .scheduler()
        .scheduleOnce(
            Duration.create(0, TimeUnit.SECONDS),
            () -> {
              runAsync(
                  () -> {
                    String statement =
                        "ALTER TABLE trip_node_parent ADD COLUMN child_index INTEGER";

                    SqlUpdate sqlUpdate = Ebean.createSqlUpdate(statement);
                    sqlUpdate.execute();

                    System.out.println("Ended Setup tasks");
                  });
            },
            this.executionContext);
  }
}
