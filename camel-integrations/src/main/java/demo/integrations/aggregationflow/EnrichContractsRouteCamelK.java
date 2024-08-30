// camel-k: language=java dependency=camel-kafka dependency=camel-jackson dependency=camel-quarkus-rest dependency=camel-quarkus-http

// kamel run src/main/java/demo/integrations/aggregationflow/EnrichContractsRouteCamelK.java --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092
// kamel get
// kamel log enrich-contracts-route-camel-k

package demo.integrations.aggregationflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.builder.RouteBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

                    JsonNode peopleData = getPersonData(owner);
//                    String firstName = peopleData.path("first_name").asText();
//                    String lastName = peopleData.path("last_name").asText();

                    // Create enriched JSON
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode enrichedData = mapper.createObjectNode();
                    enrichedData.put("code", code);
                    enrichedData.put("type", type);
                    enrichedData.put("name", name);
                    enrichedData.put("owner", owner);
                    enrichedData.put("codeTenant", codeTenant);
                    enrichedData.put("ownerTenant", ownerTenant);
                    enrichedData.set("peopleData", peopleData);

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

    private JsonNode getPersonData(String code) {
        try {
            HttpClient client = HttpClient.newHttpClient(); //TODO try with resources enablement
            String url = "http://people-service-route-camel-k-integration-project-2.apps.cluster-475kf.475kf.sandbox268.opentlc.com/people/" + code;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(responseBody).get(0); //TODO should not be a list
            } else {
                System.out.println("Failed to fetch data. HTTP error code: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
