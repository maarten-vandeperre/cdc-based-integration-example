// camel-k: language=java dependency=camel-kafka dependency=camel-jackson dependency=camel-quarkus-rest dependency=camel-quarkus-http

// kamel run src/main/java/demo/integrations/aggregationflow/EnrichContractsRouteCamelK.java --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092
// kamel get
// kamel log enrich-contracts-route-camel-k

package demo.integrations.aggregationflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

public class EnrichContractsRouteCamelK extends RouteBuilder {

    @Override
    public void configure() throws Exception {

//        onException(Exception.class)
//                .log(LoggingLevel.ERROR, "Exception occurred: ${exception.message}")
//                .handled(true);
//        getContext().setTracing(true);

        from("kafka:legacydatachanged.tenant_1.contracts?brokers={{kafka.bootstrap.servers}}")
                .log("Headers: ${headers}")
                .log("Body: ${body}")
                .unmarshal().json(JsonNode.class)
                .process(exchange -> {
                    JsonNode body = exchange.getIn().getBody(JsonNode.class);
                    String code = body.at("/payload/after/code").asText();
                    String type = body.at("/payload/after/type").asText();
                    String name = body.at("/payload/after/name").asText();
                    String owner = body.at("/payload/after/owner").asText();

                    String codeTenant = extractTenantFromUrn(code);
                    String ownerTenant = extractTenantFromUrn(owner);

                    String peopleRef = owner;

                    // Fetch people data using the REST API call
                    String peopleData = null;
                    try {
                        ProducerTemplate template = exchange.getContext().createProducerTemplate(); // TODO in try catch
                        String peopleDataUrl = String.format("http://localhost:8080/people/%s", peopleRef);
                        peopleData = template.requestBody(peopleDataUrl, null, String.class);
                        System.out.println("####1 " + peopleData);
                    } catch (Exception e) {
                        System.out.println("####2 " + e.getLocalizedMessage());
                        System.err.println(e.getLocalizedMessage());
                        // Handle the exception if the REST call fails
                        exchange.getIn().setHeader("Error", "Failed to fetch people data: " + e.getMessage());
                    }

                    // Create enriched JSON
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode enrichedData = mapper.createObjectNode();
                    enrichedData.put("code", code);
                    enrichedData.put("type", type);
                    enrichedData.put("name", name);
                    enrichedData.put("owner", owner);
                    enrichedData.put("codeTenant", codeTenant);
                    enrichedData.put("ownerTenant", ownerTenant);
//                    enrichedData.set("peopleData", mapper.readTree(peopleData));

                    exchange.getIn().setBody(enrichedData);
                })
                .marshal().json()
                .to("kafka:enriched_data?brokers={{kafka.bootstrap.servers}}")
                .log("Enriched message sent to Kafka: ${body}");
    }

    private String extractTenantFromUrn(String urn) {
        if (urn != null && urn.contains(":")) {
            String[] parts = urn.split(":");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        return null;
    }

}
