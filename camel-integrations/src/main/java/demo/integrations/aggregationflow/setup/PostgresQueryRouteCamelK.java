// camel-k: language=java dependency=camel-quarkus-rest dependency=camel-jdbc dependency=camel-quarkus-sql dependency=mvn:org.postgresql:postgresql:42.2.10

/**

 kamel run src/main/java/demo/integrations/aggregationflow/setup/PostgresQueryRouteCamelK.java \
 --property postgres-service=integration-database.integration-project-2.svc.cluster.local

 */
// kamel get
// kamel log postgres-query-route-camel-k

package demo.integrations.aggregationflow.setup;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.postgresql.ds.PGSimpleDataSource;

public class PostgresQueryRouteCamelK extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        String postgresService = getContext().resolvePropertyPlaceholders("{{postgres-service}}");

        PGSimpleDataSource tenant1DataSource = new PGSimpleDataSource();
        tenant1DataSource.setServerNames(new String[]{postgresService});
        tenant1DataSource.setDatabaseName("postgres");
        tenant1DataSource.setUser("integration");
        tenant1DataSource.setPassword("averysecurepassword");
        bindToRegistry("dataSource", tenant1DataSource);

        String[] sqlCommands = {
                "select * from public.identifiers;"
        };

        from("timer://runOnce?repeatCount=1")
                .process(exchange -> {
                    ProducerTemplate template = exchange.getContext().createProducerTemplate();

                    for (String sql : sqlCommands) {
                        exchange.getIn().setBody(sql);
                        template.send("jdbc:dataSource", exchange);
                    }
                })
                .log("SQL script executed successfully")
                .log("Output: ${body}");
    }
}
