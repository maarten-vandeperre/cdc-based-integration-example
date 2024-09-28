//// camel-k: language=java dependency=camel-kafka dependency=camel-jackson dependency=camel-quarkus-rest dependency=camel-quarkus-http
//
///**
// * kamel run src/main/java/demo/integrations/aggregationflow/flow1/EnrichContractsRouteCamelK.java \
// * --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.demo-project.svc.cluster.local:9092 \
// * --property people-camel-base-endpoint=http://people-service-route-camel-k.demo-project.svc.cluster.local
// */
//// kamel get
//// kamel log enrich-contracts-route-camel-k
//
//package demo.integrations.aggregationflow.flow1;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import jakarta.enterprise.context.ApplicationScoped;
//import org.apache.camel.builder.RouteBuilder;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//@ApplicationScoped
//public class EnrichContractsRouteCamelK extends RouteBuilder {
//
//    @Override
//    public void configure() throws Exception {
//        String tenantList = getContext().resolvePropertyPlaceholders("{{tenant_list}}");
//        String tenantBrokerUriStringTemplate= getContext().resolvePropertyPlaceholders("{{camel_route.enrich_contracts.in_uri_string_template}}");
//
////        onException(Exception.class)
////                .log(LoggingLevel.ERROR, "Exception occurred: ${exception.message}")
////                .handled(true);
////        getContext().setTracing(true);
//
//        List<String> tenants = Optional.ofNullable(tenantList)
//                .map(tl -> Arrays.stream(tl.split(",")).toList())
//                .orElse(Collections.emptyList());
//
//        tenants.forEach(t -> {
//            String camelRoute = String.format(tenantBrokerUriStringTemplate, t);
//            System.out.println("EnrichContractsRouteCamelK - Initiated camel route - " + camelRoute);
//            from(camelRoute)
//                    .id("route-" + t)
//                    .to("direct:do-processing");
//        });
//
//        from("direct:do-processing")
//                .log("Headers: ${headers}")
//                .log("Body: ${body}")
//                .unmarshal().json(JsonNode.class)
//                .process(exchange -> {
//                    JsonNode body = exchange.getIn().getBody(JsonNode.class);
//                    String code = body.at("/payload/after/code").asText();
//                    String type = body.at("/payload/after/type").asText();
//                    String name = body.at("/payload/after/name").asText();
//                    String owner = body.at("/payload/after/owner").asText();
//
//                    String codeTenant = extractTenantFromUrn(code);
//                    String ownerTenant = extractTenantFromUrn(owner);
//
//                    JsonNode peopleData = getPersonData(owner);
////                    String firstName = peopleData.path("first_name").asText();
////                    String lastName = peopleData.path("last_name").asText();
//
//                    // Create enriched JSON
//                    ObjectMapper mapper = new ObjectMapper();
//                    ObjectNode enrichedData = mapper.createObjectNode();
//                    enrichedData.put("code", code);
//                    enrichedData.put("type", type);
//                    enrichedData.put("name", name);
//                    enrichedData.put("owner", owner);
//                    enrichedData.put("codeTenant", codeTenant);
//                    enrichedData.put("ownerTenant", ownerTenant);
//                    enrichedData.set("peopleData", peopleData);
//
//                    exchange.getIn().setBody(enrichedData);
//                })
//                .marshal().json()
//                .to("{{camel_route.enrich_contracts.out_uri_string_template}}")
//                .log("Enriched message sent to Kafka: ${body}");
//    }
//
//    private String extractTenantFromUrn(String urn) {
//        if (urn != null && urn.contains(":")) {
//            String[] parts = urn.split(":");
//            if (parts.length >= 3) {
//                return parts[2];
//            }
//        }
//        return null;
//    }
//
//    private JsonNode getPersonData(String code) {
//        try {
//            String peopleCamelEndpoint = getContext().resolvePropertyPlaceholders("{{people-camel-base-endpoint}}");
//            HttpClient client = HttpClient.newHttpClient(); //TODO try with resources enablement
//            String url = peopleCamelEndpoint + "/people/" + code;
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .GET()
//                    .build();
//
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() == 200) {
//                String responseBody = response.body();
//                ObjectMapper objectMapper = new ObjectMapper();
//                return objectMapper.readTree(responseBody).get(0); //TODO should not be a list
//            } else {
//                System.out.println("Failed to fetch data. HTTP error code: " + response.statusCode());
//                return null;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//}
