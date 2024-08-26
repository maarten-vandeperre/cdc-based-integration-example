// camel-k: language=java dependency=camel-jdbc dependency=camel-quarkus-rest dependency=camel-quarkus-http dependency=mvn:org.postgresql:postgresql:42.2.10
// kamel run src/main/java/demo/integrations/ExposePeopleRouteCamelK.java
// kamel get
// kamel log expose-people-route-camel-k

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.postgresql.ds.PGSimpleDataSource;

public class ExposePeopleRouteCamelK extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{"integration.integration-project.svc.cluster.local"});
        dataSource.setDatabaseName("postgres");
        dataSource.setUser("integration");
        dataSource.setPassword("averysecurepassword");

        bindToRegistry("myDataSource", dataSource);

        from("platform-http:/api/people?httpMethodRestrict=GET")
//                .setHeader("first_name", constant("Maarten"))
//                .setBody(simple("SELECT ref, first_name, last_name FROM people where first_name = :?first_name"))
                .setBody(simple("SELECT ref, first_name, last_name FROM people"))
                .to("jdbc:myDataSource?useHeadersAsParameters=true")
                .marshal().json()
                .log("Selected data: ${body}");

        from("platform-http:/api/people-enriched?httpMethodRestrict=GET")
                .setBody(simple("SELECT ref, first_name, last_name FROM people"))
                .to("jdbc:myDataSource?useHeadersAsParameters=true")
                .marshal().json()
                .setHeader(Exchange.HTTP_URI, constant(""))
                .setHeader(Exchange.HTTP_PATH, constant(""))
                .to("log:INFO?showBody=true&showHeaders=true")
                .enrich("http://expose-people-route-camel-k-integration-project.apps.cluster-475kf.475kf.sandbox268.opentlc.com/api/hello?bridgeEndpoint=true", (original, enrich) -> {
                    try {
                        String originalBody = original.getIn().getBody(String.class);
                        String enrichBody = enrich.getIn().getBody(String.class);

                        ObjectMapper mapper = new ObjectMapper();
                        ArrayNode originalJson = (ArrayNode) mapper.readTree(originalBody);
                        originalJson.forEach(jsonNode -> ((ObjectNode) jsonNode).put("added_field", enrichBody));

                        original.getIn().setBody(mapper.writeValueAsString(originalJson));
                        return original;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .log("Selected data: ${body}");

        from("platform-http:/api/hello?httpMethodRestrict=GET")
                .setBody(constant("Hello World"));
    }
}