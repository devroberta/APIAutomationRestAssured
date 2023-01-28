package User;

import com.github.javafaker.Faker;
import dio.com.entities.User;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThan;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserTests {

  private static User user;

  public static Faker faker;
  public static RequestSpecification request;

  @BeforeAll
  public static void setUp() {
    RestAssured.baseURI = "https://petstore.swagger.io/v2";

    faker = new Faker();

    user = new User(faker.name().username(),
            faker.name().firstName(),
            faker.name().lastName(),
            faker.internet().safeEmailAddress(),
            faker.internet().password(8,10),
            faker.phoneNumber().toString());
  }

  @BeforeEach
  void setRequest() {
    request = given().config(RestAssured.config().logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
            .header("api-key", "special-key")
            .contentType(ContentType.JSON);
  }

  @Test
  @Order(1)
  public void createNewUser_WithValidData_ReturnOk() {
    request.body(user)
            .when()
            .post("/user")
            .then()
            .assertThat().statusCode(200).and()
            .body("code", equalTo(200))
            .body("type", equalTo("unknown"))
            .body("message", isA(String.class))
            .body("size()", equalTo(3));
  }

  @Test
  @Order(2)
  public void GetLogin_ValidUser_ReturnOk() {
    request.param("username", user.getUsername())
            .param("password", user.getPassword())
            .when()
            .get("/user/login")
            .then()
            .assertThat()
            .statusCode(200)
            .and().time(lessThan(2000L))
            .and().body(matchesJsonSchemaInClasspath("loginResponseSchema.json"));
  }

  @Test
  @Order(3)
  public void GetUserByUsername_userIsValid_ReturnOk() {
    request.when()
            .get("/user/" + user.getUsername())
            .then()
            .assertThat().statusCode(200).and().time(lessThan(2000L))
            .and().body("firstname", equalTo(user.getFirstname()));
  }

  @Test
  @Order(4)
  public void DeleteUser_UserExists_ReturnOk() {
    request.when()
            .delete("/user" + user.getUsername())
            .then()
            .assertThat().statusCode(200).and().time(lessThan(2000L))
            .log();
  }

  @Test
  @Order(5)
  public void CreateNewUser_WithInvalidBody_ReturnBadRequest() {

    Response response = request.body("teste")
            .when()
            .post("/user")
            .then()
            .extract().response();

    Assertions.assertNotNull(response);
    Assertions.assertEquals(400, response.statusCode());
    Assertions.assertEquals(true, response.getBody().asPrettyString().contains("unknown"));
    Assertions.assertEquals(3, response.body().jsonPath().getMap("$").size());
  }
}
