package demo.integrations.aggregationflow.flow1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.builder.Builder.constant;
import static org.assertj.core.api.Assertions.assertThat;


@QuarkusTest
public class EnrichContractsRouteCamelKTest {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:result_enrich_contracts")
    MockEndpoint mockEnrichedDataEndpoint;

    @BeforeEach
    public void setup() {
        mockEnrichedDataEndpoint.reset();
    }

    @ConfigProperty(name = "quarkus.http.test-port")
    int testPort;

    @Test
    public void testEnrichment() throws Exception {
        // Mock/stub people API
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:database-call", MockEndpoint.class);
        mockEndpoint.returnReplyBody(constant(
                List.of(
                        Map.of(
                                "code", "urn:person:t3:0001",
                                "first_name", "Maarten",
                                "last_name", "Tenant 3",
                                "status", "PAS_A",
                                "gender", "G_M"
                        )
                )
        ));
        mockEndpoint.expectedMessageCount(1);

        AdviceWith.adviceWith(camelContext, "fetch-from-database", route ->
                route.interceptSendToEndpoint("jdbc:tenant3DataSource")
                        .skipSendToOriginalEndpoint()
                        .to("mock:database-call"));

        // Start enrichment test config
        camelContext.getPropertiesComponent().addOverrideProperty("people-camel-base-endpoint", "http://localhost:" + testPort);

        String jsonInput = "{ \"payload\": { \"after\": { \"code\": \"urn:contract:t3:1\", \"type\": \"CT_L\", \"name\": \"Lease Agreement - updated\", \"owner\": \"urn:person:t3:0001\" } } }";

        mockEnrichedDataEndpoint.expectedMessageCount(1);

        producerTemplate.send("direct:kafkamock.legacydatachanged.tenant_1.contracts", exchange -> {
            exchange.getIn().setBody(jsonInput);
        });

        // Validate result
        mockEnrichedDataEndpoint.assertIsSatisfied();
        String body = mockEnrichedDataEndpoint.getExchanges().get(0).getMessage().getBody(String.class);
        Map<String, Object> enrichedResult = new ObjectMapper().readValue(body, new TypeReference<HashMap<String, Object>>() {});

        Map<String, Object> expectedEnrichedData = getEnrichedDataDefaultData();

        assertThat(enrichedResult).isEqualTo(expectedEnrichedData);
    }

    public static Map<String, Object> getEnrichedDataDefaultData() {
        Map<String, Object> expectedEnrichedData = new HashMap<>();

        expectedEnrichedData.put("code", "urn:contract:t3:1");
        expectedEnrichedData.put("type", "CT_L");
        expectedEnrichedData.put("name", "Lease Agreement - updated");
        expectedEnrichedData.put("owner", "urn:person:t3:0001");
        expectedEnrichedData.put("codeTenant", "t3");
        expectedEnrichedData.put("ownerTenant", "t3");

        Map<String, String> peopleData = new HashMap<>();
        peopleData.put("status", "PAS_A");
        peopleData.put("last_name", "Tenant 3");
        peopleData.put("first_name", "Maarten");
        peopleData.put("gender", "G_M");
        peopleData.put("code", "urn:person:t3:0001");

        expectedEnrichedData.put("peopleData", peopleData);
        return expectedEnrichedData;
    }
}
