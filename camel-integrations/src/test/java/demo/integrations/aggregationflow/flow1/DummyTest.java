package demo.integrations.aggregationflow.flow1;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(DummyTest.class)
public class DummyTest implements QuarkusTestProfile {
    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "feature.flag.camel_routes.enrich_contracts.enabled", "true"
        );
    }

    @Test
    public void test() throws InterruptedException {
        producerTemplate.sendBody("direct:start", "Hello World");

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);

        assertThat(mockEndpoint.getExchanges().get(0).getIn().getBody()).isEqualTo("adfasfdasdf");
    }
}
