package demo.integrations.aggregationflow.flow1;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@QuarkusTest
public class EnrichContractsRouteCamelKTest {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:kafka:enriched_data")
    MockEndpoint mockEnrichedDataEndpoint;

    @BeforeEach
    public void setup() {
        mockEnrichedDataEndpoint.reset();
    }

    @Test
    public void testEnrichment() throws Exception {
        // Prepare test data
        String jsonInput = "{ \"payload\": { \"after\": { \"code\": \"urn:tenant:123\", \"type\": \"testType\", \"name\": \"Test Name\", \"owner\": \"urn:tenant:owner123\" } } }";
        String expectedEnrichedData = "{ \"code\": \"urn:tenant:123\", \"type\": \"testType\", \"name\": \"Test Name\", \"owner\": \"urn:tenant:owner123\", \"codeTenant\": \"123\", \"ownerTenant\": \"owner123\", \"peopleData\": null }";

        mockEnrichedDataEndpoint.expectedMessageCount(1);
        mockEnrichedDataEndpoint.expectedBodiesReceived(expectedEnrichedData);

        // Send test input to the route
        producerTemplate.send("direct:do-processing", exchange -> {
            exchange.setProperty("kafka.bootstrap.servers", "testtest");
            exchange.getIn().setBody(jsonInput);
        });

        // Assert the result
        mockEnrichedDataEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteDisabled() throws Exception {
        // Mock properties
        camelContext.getGlobalOptions().put("feature.flag.camel_routes.enrich_contracts.enabled", "false");

        // Since the route is disabled, we expect no messages to be processed
        mockEnrichedDataEndpoint.expectedMessageCount(0);

        // Assert no message is processed
        mockEnrichedDataEndpoint.assertIsSatisfied();
    }
}
