// camel-k: language=java dependency=camel-jackson dependency=camel-kafka
// kamel run src/main/java/demo/integrations/CdcRouteCamelK.java --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092
// kamel get
// kamel log cdc-route-camel-k

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

public class CdcRouteCamelK extends RouteBuilder {

    @Override
    public void configure() {

        JacksonDataFormat jsonDataFormat = new JacksonDataFormat(JsonNode.class);

        from("kafka:legacydatachanged.public.people?brokers={{kafka.bootstrap.servers}}")
                .unmarshal(jsonDataFormat)
                .log("Incoming JSON Body: ${body}")
                .process(exchange -> {
                    JsonNode jsonNode = exchange.getIn().getBody(JsonNode.class);

                    ObjectNode node = new ObjectMapper().createObjectNode();
                    node.set("ref", jsonNode.get("payload").get("after").get("ref"));
                    node.set("idRef", jsonNode.get("payload").get("after").get("ref"));
                    node.set("first_name", jsonNode.get("payload").get("after").get("first_name"));
                    node.set("last_name", jsonNode.get("payload").get("after").get("last_name"));

                    exchange.getIn().setBody(node);
                })

                .marshal(jsonDataFormat)
                .log("Outgoing JSON Body: ${body}")
                .to("kafka:processedTopic?brokers={{kafka.bootstrap.servers}}");
    }
}