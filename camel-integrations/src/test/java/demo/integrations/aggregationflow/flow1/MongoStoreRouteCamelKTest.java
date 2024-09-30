package demo.integrations.aggregationflow.flow1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static demo.integrations.aggregationflow.flow1.EnrichContractsRouteCamelKTest.getEnrichedDataDefaultData;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class MongoStoreRouteCamelKTest {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    public void testHappyPath() throws Exception {
        String jsonInput = new ObjectMapper().writeValueAsString(getEnrichedDataDefaultData());

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:database-call-mongo-store-route", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        AdviceWith.adviceWith(camelContext, "mongo-store", route ->
                route.interceptSendToEndpoint("mongodb:myMongoClient?database=aggregation-database&collection=contracts&operation=save")
                        .skipSendToOriginalEndpoint()
                        .to("mock:database-call-mongo-store-route"));

        producerTemplate.send("direct:kafkamock.enriched_data", exchange -> {
            exchange.getIn().setBody(jsonInput);
        });

        assertThat(mockEndpoint.getExchanges().size()).isEqualTo(1);
        String body = mockEndpoint.getExchanges().get(0).getMessage().getBody(String.class);
        Map<String, Object> storedResult = new ObjectMapper().readValue(body, new TypeReference<HashMap<String, Object>>() {
        });

        Map<String, Object> expectedStoredResult = getEnrichedDataDefaultData();
        expectedStoredResult.put("last_updated", storedResult.get("last_updated"));

        assertThat(storedResult).isEqualTo(expectedStoredResult);
    }
}
