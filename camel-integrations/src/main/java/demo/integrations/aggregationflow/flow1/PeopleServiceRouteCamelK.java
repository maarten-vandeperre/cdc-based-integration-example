// camel-k: language=java dependency=camel-quarkus-rest dependency=camel-jdbc dependency=camel-quarkus-sql dependency=mvn:org.postgresql:postgresql:42.2.10

/**

 kamel run src/main/java/demo/integrations/aggregationflow/flow1/PeopleServiceRouteCamelK.java \
        --property postgres-service=integration-database.integration-project-2.svc.cluster.local

 */
// kamel get
// kamel log people-service-route-camel-k

package demo.integrations.aggregationflow.flow1;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.postgresql.ds.PGSimpleDataSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeopleServiceRouteCamelK extends RouteBuilder {
    private final Map<String, String> tenantDataSourceMap = new ConcurrentHashMap<>();

    public PeopleServiceRouteCamelK() {
        // Map tenant identifier to DataSource name
        tenantDataSourceMap.put("t1", "tenant1DataSource");
        tenantDataSourceMap.put("t2", "tenant2DataSource");
        tenantDataSourceMap.put("t3", "tenant3DataSource");
    }

    @Override
    public void configure() throws Exception {
        String postgresService = getContext().resolvePropertyPlaceholders("{{postgres-service}}");

        PGSimpleDataSource tenant1DataSource = new PGSimpleDataSource();
        tenant1DataSource.setServerNames(new String[]{postgresService});
        tenant1DataSource.setDatabaseName("postgres");
        tenant1DataSource.setUser("tenant_1");
        tenant1DataSource.setPassword("integration");
        bindToRegistry("tenant1DataSource", tenant1DataSource);
        
        PGSimpleDataSource tenant2DataSource = new PGSimpleDataSource();
        tenant1DataSource.setServerNames(new String[]{postgresService});
        tenant2DataSource.setDatabaseName("postgres");
        tenant2DataSource.setUser("tenant_2");
        tenant2DataSource.setPassword("integration");
        bindToRegistry("tenant2DataSource", tenant2DataSource);

        PGSimpleDataSource tenant3DataSource = new PGSimpleDataSource();
        tenant1DataSource.setServerNames(new String[]{postgresService});
        tenant3DataSource.setDatabaseName("postgres");
        tenant3DataSource.setUser("tenant_1");
        tenant3DataSource.setPassword("integration");
        bindToRegistry("tenant3DataSource", tenant3DataSource);

        rest("/people/{code}")
                .get()
                .to("direct:fetchPeopleData");

        from("direct:fetchPeopleData")
                .routeId("fetchPeopleData")
                .log("Headers: ${headers}")
                .log("Body: ${body}")
                .bean(this.getClass(), "setDataSourceBasedOnTenant")
                .log("Tenant DataSource: ${exchangeProperty.dataSourceName}")
                .setBody(simple("SELECT code, first_name, last_name, status, gender FROM people WHERE code = '${header.code}'"))
                .toD("jdbc:${exchangeProperty.dataSourceName}")
                .log("Selected data: ${body}")
                .marshal().json();
    }

    public void setDataSourceBasedOnTenant(Exchange exchange) throws Exception {
        // Extract tenant identifier from the code
        String code = exchange.getIn().getHeader("code", String.class);
        if (code != null) {
            String[] parts = code.split(":");
            if (parts.length > 2) {
                String tenantId = parts[2];

                // Map tenant identifier to DataSource name
                String dataSourceName = tenantDataSourceMap.get(tenantId);
                if (dataSourceName != null) {
                    exchange.setProperty("dataSourceName", dataSourceName);
                } else {
                    throw new IllegalArgumentException("Unknown tenant identifier: " + tenantId);
                }
            } else {
                throw new IllegalArgumentException("Invalid code format: " + code);
            }
        } else {
            throw new IllegalArgumentException("Code header is missing");
        }
    }
}

