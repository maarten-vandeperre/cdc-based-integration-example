package demo.integrations.aggregationflow.flow1;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.log.LogDetail;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.camel.builder.Builder.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class PeopleServiceRouteCamelKTest {

    @Inject
    CamelContext camelContext;

    @Test
    public void testHappyPath() throws Exception {
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:database-call-people-service-route", MockEndpoint.class);
        mockEndpoint.returnReplyBody(constant(
                List.of(
                        Map.of(
                                "code", "urn:person:t1:0001",
                                "first_name", "Maarten",
                                "last_name", "Tenant 1",
                                "status", "PAS_A",
                                "gender", "G_M"
                        )
                )
        ));
        mockEndpoint.expectedMessageCount(1);

        AdviceWith.adviceWith(camelContext, "fetch-from-database", route ->
                route.interceptSendToEndpoint("jdbc:tenant1DataSource")
                        .skipSendToOriginalEndpoint()
                        .to("mock:database-call-people-service-route"));

        given()
                .when()
                .get("/people/urn:person:t1:0001")
                .then()
                .log().ifValidationFails(LogDetail.BODY)
                .statusCode(200)
                .body("[0].code", is("urn:person:t1:0001"))
                .body("[0].first_name", is("Maarten"))
                .body("[0].last_name", is("Tenant 1"))
                .body("[0].status", is("PAS_A"))
                .body("[0].gender", is("G_M"))
        ;
//                .extract().body().jsonPath().getList(".", Person.class);

        assertThat(mockEndpoint.getExchanges().size()).isEqualTo(1);
        assertThat(mockEndpoint.getExchanges().get(0).getProperties().get("dataSourceName")).isEqualTo("tenant1DataSource");
        assertThat(mockEndpoint.getExchanges().get(0).getIn().getBody().toString())
                .isEqualTo("SELECT code, first_name, last_name, status, gender FROM people WHERE code = 'urn:person:t1:0001'");
        mockEndpoint.assertIsSatisfied();
    }

}