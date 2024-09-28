// camel-k: language=java dependency=camel-kafka dependency=camel-mongodb dependency=camel-jackson dependency=camel-quarkus-mongodb

/**
  kamel run src/main/java/demo/integrations/aggregationflow/setup/MongoQueryRouteCamelK.java \
  --property mongo-connection-url="mongodb://mongo:mongo@aggregation-database.demo-project.svc.cluster.local:27017/?authSource=admin"
 */
// kamel get
// kamel log mongo-query-route-camel-k

package demo.integrations.aggregationflow.setup;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.camel.builder.RouteBuilder;
import org.bson.Document;

public class MongoQueryRouteCamelK extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        String camelRouteName = "mongo_store";
        boolean enabled = Boolean.valueOf(getContext().resolvePropertyPlaceholders("{{feature.flag.camel_routes." + camelRouteName + ".enabled}}"));

        if (enabled) {
            System.out.println("Camel route " + camelRouteName + " enabled");
            processCamelRoutes();
        } else {
            System.out.println("Camel route " + camelRouteName + " disabled");
        }
    }

    private void processCamelRoutes() {
        String mongoUri = getContext().resolvePropertyPlaceholders("{{mongo-connection-url}}");
        MongoClient mongoClient = MongoClients.create(mongoUri);
        getContext().getRegistry().bind("myMongoClient", mongoClient);

        from("timer://runOnce?repeatCount=1")
                .to("mongodb:myMongoClient?database=aggregation-database&collection=contracts&operation=findAll")
                .process(exchange -> {
                    Document[] documents = exchange.getIn().getBody(Document[].class);
                    for (Document doc : documents) {
                        log.info("Found document: " + doc.toJson());
                    }
                });
    }
}
