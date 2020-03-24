package at.taaja.redcat;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class PositionResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/theBox/v1/position")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

}