// camel-k: language=java dependency=camel-kafka dependency=camel-mongodb dependency=camel-jackson dependency=camel-quarkus-mongodb

/**

 kamel run src/main/java/demo/integrations/aggregationflow/flow1/MongoStoreRouteCamelK.java \
        --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.demo-project.svc.cluster.local:9092 \
        --property mongo-connection-url="mongodb://mongo:mongo@aggregation-database.demo-project.svc.cluster.local:27017/?authSource=admin"

 */

// kamel get
// kamel log mongo-store-route-camel-k

package demo.integrations.aggregationflow.flow1;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.camel.builder.RouteBuilder;
import java.util.Date;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MongoStoreRouteCamelK extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        String mongoUri = getContext().resolvePropertyPlaceholders("{{mongo-connection-url}}");
        MongoClient mongoClient = MongoClients.create(mongoUri);
        getContext().getRegistry().bind("myMongoClient", mongoClient);

        from("{{camel_route.mongo_store.in_uri_string_template}}")
                .id("mongo-store")
                .unmarshal().json(JsonNode.class)
                .process(exchange -> {
                    JsonNode body = exchange.getIn().getBody(JsonNode.class);
                    ((ObjectNode) body).put("last_updated", new Date().toString());

                    // Prepare MongoDB query to upsert document
                    String code = body.get("code").asText();
                    exchange.getIn().setHeader("CamelMongoDbCriteria", String.format("{ \"code\": \"%s\" }", code));
                    exchange.getIn().setHeader("CamelMongoDbOperation", "save");
                    exchange.getIn().setBody(body.toString());
                })
                .to("mongodb:myMongoClient?database=aggregation-database&collection=contracts&operation=save")
                .log("Document stored in MongoDB: ${body}");
    }
}