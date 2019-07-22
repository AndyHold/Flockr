package tasks;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Inject;
import models.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import util.CountrySchedulerUtil;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This class contains the code needed to sync the countries from the external countries API.
 * Only runs when the server is compiled.
 */
public class CountrySyncTask {

    private ActorSystem actorSystem;
    private ExecutionContext executionContext;
    private final CountrySchedulerUtil countrySchedulerUtil;
    final Logger log = LoggerFactory.getLogger(this.getClass());
    private final WSClient ws;

    /**
     * Please refer to Play documentation: https://www.playframework.com/documentation/2.7.x/ScheduledTasks
     * @param actorSystem
     * @param executionContext
     */
    @Inject
    public CountrySyncTask(ActorSystem actorSystem, ExecutionContext executionContext, WSClient ws, CountrySchedulerUtil countrySchedulerUtil) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.ws = ws;
        this.initialise();
        this.countrySchedulerUtil = countrySchedulerUtil;
    }

    /**
     * Fetches all countries from restcountries api
     * @return all countries as a map
     */
    private CompletionStage<Map<String, Country>> fetchCountryApi() {
        String countryUrl = "https://restcountries.eu/rest/v2/all?fields=name;alpha3Code";
        WSRequest request = ws.url(countryUrl);
        return ws.url(countryUrl).get() .thenApplyAsync(response -> {
                    JsonNode resJson = response.asJson();
                    Map<String, Country> countries = new HashMap<>();

                    for (JsonNode currentCountry : resJson) {
                        String countryName = currentCountry.get("name").asText();
                        String ISOCode = currentCountry.get("alpha3Code").asText();

                        countries.put(ISOCode, new Country(countryName, ISOCode, true));
                    }

                    return countries;
                });
    }


    /**
     * Gets all countries already in the db
     * @return current countries as a map
     */
    public Map<String, Country> getCurrentCountries() {
        Map<String, Country> currentCountriesMap = new HashMap<>();
        List<Country> currentCountries = Country.find.all();

        for (Country country : currentCountries) {
            currentCountriesMap.put(country.getISOCode(), country);
        }

        return currentCountriesMap;
    }

    /**
     * Saves all countries that need to be inserted or updated
     * @param countries
     */
    public void saveCountries(List<Country> countries) {
        for (Country country: countries) {
            country.save();
        }
    }

    /**
     * Define the code to be run, and when it should be run
     * NOTE - internet enabler must be turned on.
     */
    private void initialise() {
        this.actorSystem
            .scheduler()
            .schedule(
                Duration.create(1, TimeUnit.HOURS), // initial delay
                Duration.create(1, TimeUnit.HOURS), // interval
                () -> {
                    log.info("Country Schedule started");
                    long startTime = System.currentTimeMillis();
                    fetchCountryApi()
                            .thenApplyAsync(newCountries -> {
                                Map<String, Country> oldCountries = getCurrentCountries();
                                List<Country> countriesToSave = countrySchedulerUtil.getCountriesToSave(oldCountries, newCountries);
                                saveCountries(countriesToSave);
                                long endTime = System.currentTimeMillis();
                                long duration = (endTime - startTime) / 1000;
                                log.info("Country schedule finished, took: " + duration + " seconds");
                                return null;
                           });
                },
                this.executionContext
            );
    }
}
