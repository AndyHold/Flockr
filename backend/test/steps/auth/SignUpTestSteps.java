package steps.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.AfterStep;
import cucumber.api.java.Before;
import cucumber.api.java.BeforeStep;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import exceptions.FailedToSignUpException;
import io.cucumber.datatable.DataTable;
import models.User;
import org.junit.Assert;
import play.Application;
import play.ApplicationLoader;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import steps.TestState;
import utils.FakeClient;
import utils.FakePlayClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static play.test.Helpers.route;

public class SignUpTestSteps {

    private JsonNode userData;
    private Result result;
    private Result signUpResponse;
    private User user;
    private Exception exception;

    @Given("that I have valid user data to sign up:")
    public void thatIHaveValidUserDataToSignUp(DataTable dataTable) {
        List<Map<String, String>> list = dataTable.asMaps(String.class, String.class);
        Map<String, String> firstRow = list.get(0);
        this.userData = Json.toJson(firstRow);

        Assert.assertEquals(4, this.userData.size());
    }

    @Given("that I have incomplete user data to sign up:")
    public void thatIHaveIncompleteUserDataToSignUp(DataTable dataTable) {
        List<Map<String, String>> list = dataTable.asMaps(String.class, String.class);
        Map<String, String> firstRow = list.get(0);
        this.userData = Json.toJson(firstRow);
        Assert.assertTrue(this.userData.size() < 4);
    }

    @When("I sign up the user")
    public void iSignUpTheUser() throws IOException {
        FakeClient fakeClient = TestState.getInstance().getFakeClient();
        try {
            this.user = fakeClient.signUpUser(this.userData);
            Assert.assertNotNull(this.user);
            Assert.assertNotEquals(0, this.user.getUserId());
        } catch (FailedToSignUpException e) {
            this.exception = e;
            Assert.assertNotNull(this.exception);
        }
    }

    @Then("The user now exists in the system")
    public void theUserIsNowStoredInTheSystem() {
        User user = User.find.byId(this.user.getUserId());
        Assert.assertEquals(this.user.getUserId(), user.getUserId());
    }

    @Then("^I should receive an error message saying \"([^\"]*)\"$")
    public void iShouldReceiveAnErrorMessageSaying(String message) {
        Assert.assertEquals(message, this.exception.getMessage());
    }
}
