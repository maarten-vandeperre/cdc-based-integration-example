// camel-k: language=java dependency=camel-quarkus-rest dependency=camel-jdbc dependency=camel-quarkus-sql dependency=mvn:org.postgresql:postgresql:42.2.10

/**

 kamel run src/main/java/demo/integrations/aggregationflow/setup/PostgresRouteCamelK.java \
 --property postgres-service=integration-database.demo-project.svc.cluster.local

 */
// kamel get
// kamel log postgres-route-camel-k

package demo.integrations.aggregationflow.setup;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.postgresql.ds.PGSimpleDataSource;

public class PostgresRouteCamelK extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        String camelRouteName = "postgres";
        boolean enabled = Boolean.valueOf(getContext().resolvePropertyPlaceholders("{{feature.flag.camel_routes." + camelRouteName + ".enabled}}"));

        if (enabled) {
            System.out.println("Camel route " + camelRouteName + " enabled");
            processCamelRoutes();
        } else {
            System.out.println("Camel route " + camelRouteName + " disabled");
        }
    }

    private void processCamelRoutes() {
        String postgresService = getContext().resolvePropertyPlaceholders("{{postgres-service}}");

        PGSimpleDataSource tenant1DataSource = new PGSimpleDataSource();
        tenant1DataSource.setServerNames(new String[]{postgresService});
        tenant1DataSource.setDatabaseName("postgres");
        tenant1DataSource.setUser("integration");
        tenant1DataSource.setPassword("averysecurepassword");
        bindToRegistry("dataSource", tenant1DataSource);

        String[] sqlCommands = {
                // Step 1: Create Schemas
                "CREATE SCHEMA tenant_1;",
                "CREATE SCHEMA tenant_2;",
                "CREATE SCHEMA tenant_3;",

                // Step 2: Create Users with passwords
                "CREATE USER tenant_1 WITH PASSWORD 'integration';",
                "CREATE USER tenant_2 WITH PASSWORD 'integration';",
                "CREATE USER tenant_3 WITH PASSWORD 'integration';",

                // Step 3: Grant Schema Permissions to Users
                "GRANT ALL PRIVILEGES ON SCHEMA tenant_1 TO tenant_1;",
                "GRANT ALL PRIVILEGES ON SCHEMA tenant_2 TO tenant_2;",
                "GRANT ALL PRIVILEGES ON SCHEMA tenant_3 TO tenant_3;",
                "ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_1 GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tenant_1;",
                "ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_2 GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tenant_2;",
                "ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_3 GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tenant_3;",

                // Step 4: Create a Table in the Public Schema
                "CREATE TABLE public.identifiers (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "name VARCHAR(255) NOT NULL," +
                        "readable_code VARCHAR(50) NOT NULL," +
                        "type VARCHAR(50) NOT NULL);",

                // Step 5: Create Tables in Tenant Schemas
                // Tenant 1 Schema
                "CREATE TABLE tenant_1.people (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "first_name VARCHAR(100)," +
                        "last_name VARCHAR(100)," +
                        "gender VARCHAR(10)," +
                        "status VARCHAR(50));",

                "CREATE TABLE tenant_1.addresses (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "type VARCHAR(50)," +
                        "address_line_1 VARCHAR(255)," +
                        "address_line_2 VARCHAR(255)," +
                        "country VARCHAR(100));",

                "CREATE TABLE tenant_1.contracts (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "type VARCHAR(50)," +
                        "name VARCHAR(255)," +
                        "owner VARCHAR(50) REFERENCES tenant_1.people(code));",

                "CREATE TABLE tenant_1.people_addresses (" +
                        "people_id INTEGER REFERENCES tenant_1.people(id)," +
                        "address_id INTEGER REFERENCES tenant_1.addresses(id)," +
                        "PRIMARY KEY (people_id, address_id));",

                // Tenant 2 Schema
                "CREATE TABLE tenant_2.people (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "first_name VARCHAR(100)," +
                        "last_name VARCHAR(100)," +
                        "gender VARCHAR(10)," +
                        "status VARCHAR(50));",

                "CREATE TABLE tenant_2.addresses (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "type VARCHAR(50)," +
                        "address_line_1 VARCHAR(255)," +
                        "address_line_2 VARCHAR(255)," +
                        "country VARCHAR(100));",

                "CREATE TABLE tenant_2.contracts (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "type VARCHAR(50)," +
                        "name VARCHAR(255)," +
                        "owner VARCHAR(50) REFERENCES tenant_1.people(code));",

                "CREATE TABLE tenant_2.people_addresses (" +
                        "people_id INTEGER REFERENCES tenant_2.people(id)," +
                        "address_id INTEGER REFERENCES tenant_2.addresses(id)," +
                        "PRIMARY KEY (people_id, address_id));",

                // Tenant 3 Schema
                "CREATE TABLE tenant_3.people (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "first_name VARCHAR(100)," +
                        "last_name VARCHAR(100)," +
                        "gender VARCHAR(10)," +
                        "status VARCHAR(50));",

                "CREATE TABLE tenant_3.addresses (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "type VARCHAR(50)," +
                        "address_line_1 VARCHAR(255)," +
                        "address_line_2 VARCHAR(255)," +
                        "country VARCHAR(100));",

                "CREATE TABLE tenant_3.contracts (" +
                        "id SERIAL PRIMARY KEY," +
                        "code VARCHAR(50) NOT NULL UNIQUE," +
                        "type VARCHAR(50)," +
                        "name VARCHAR(255)," +
                        "owner VARCHAR(50) REFERENCES tenant_3.people(code));",

                "CREATE TABLE tenant_3.people_addresses (" +
                        "people_id INTEGER REFERENCES tenant_3.people(id)," +
                        "address_id INTEGER REFERENCES tenant_3.addresses(id)," +
                        "PRIMARY KEY (people_id, address_id));",

                // Step 6: Insert Sample Records
                // Insert into public.identifiers
                "INSERT INTO public.identifiers (name, code, readable_code, type) VALUES" +
                        "('ACTIVE', 'ACTIVE', 'PAS_A', 'PERSON_ACTIVITY_STATUS')," +
                        "('INACTIVE', 'INACTIVE', 'PAS_I', 'PERSON_ACTIVITY_STATUS')," +
                        "('MALE', 'MALE', 'G_M', 'GENDER')," +
                        "('FEMALE', 'FEMALE', 'G_F', 'GENDER')," +
                        "('HOME', 'HOME', 'AT_H', 'ADDRESS_TYPE')," +
                        "('OFFICE', 'OFFICE', 'AT_O', 'ADDRESS_TYPE')," +
                        "('LEASE', 'LEASE', 'CT_L', 'CONTRACT_TYPE')," +
                        "('SERVICE', 'SERVICE', 'CT_S', 'CONTRACT_TYPE');",

                // Insert into tenant_1.people
                "INSERT INTO tenant_1.people (code, first_name, last_name, gender, status) VALUES" +
                        "('urn:person:t1:0001', 'Maarten', 'Tenant 1', 'G_M', 'PAS_A')," +
                        "('urn:person:t1:0002', 'Pieter', 'Tenant 1', 'G_M', 'PAS_I')," +
                        "('urn:person:t1:0003', 'Alice', 'Tenant 1', 'G_F', 'PAS_A');",

                // Insert into tenant_1.addresses
                "INSERT INTO tenant_1.addresses (code, type, address_line_1, address_line_2, country) VALUES" +
                        "('urn:address:t1:1', 'AT_H', '123 Main St', 'Apt 4B', 'USA')," +
                        "('urn:address:t1:2', 'AT_O', '456 Business Rd', 'Suite 101', 'USA')," +
                        "('urn:address:t1:3', 'AT_H', '789 Beach Ave', 'Cottage 7', 'USA');",

                // Insert into tenant_1.contracts
                "INSERT INTO tenant_1.contracts (code, type, name, owner) VALUES" +
                        "('urn:contract:t1:1', 'CT_L', 'Lease Agreement', 'urn:person:t1:0001')," +
                        "('urn:contract:t1:2', 'CT_L', 'Employment Contract', 'urn:person:t1:0001')," +
                        "('urn:contract:t1:3', 'CT_S', 'Service Agreement', 'urn:person:t1:0002');",

                // Insert into tenant_1.people_addresses

                // Repeat sample data insertion for tenant_2 and tenant_3
                // Tenant 2
                "INSERT INTO tenant_2.people (code, first_name, last_name, gender, status) VALUES" +
                        "('urn:person:t2:0001', 'Maarten', 'Tenant 2', 'G_M', 'PAS_A')," +
                        "('urn:person:t2:0002', 'Pieter', 'Tenant 2', 'G_M', 'PAS_I')," +
                        "('urn:person:t2:0003', 'Alice', 'Tenant 2', 'G_F', 'PAS_A');",

                "INSERT INTO tenant_2.addresses (code, type, address_line_1, address_line_2, country) VALUES" +
                        "('urn:address:t2:1', 'AT_H', '123 Main St', 'Apt 4B', 'USA 2')," +
                        "('urn:address:t2:2', 'AT_O', '456 Business Rd', 'Suite 101', 'USA 2')," +
                        "('urn:address:t2:3', 'AT_H', '789 Beach Ave', 'Cottage 7', 'USA 2');",

                "INSERT INTO tenant_2.contracts (code, type, name, owner) VALUES" +
                        "('urn:contract:t2:1', 'CT_L', 'Lease Agreement', 'urn:person:t1:0001')," +
                        "('urn:contract:t2:2', 'CT_L', 'Employment Contract', 'urn:person:t1:0001')," +
                        "('urn:contract:t2:3', 'CT_S', 'Service Agreement', 'urn:person:t1:0002');",

                // Tenant 3
                "INSERT INTO tenant_3.people (code, first_name, last_name, gender, status) VALUES" +
                        "('urn:person:t3:0001', 'Maarten', 'Tenant 3', 'G_M', 'PAS_A')," +
                        "('urn:person:t3:0002', 'Pieter', 'Tenant 3', 'G_M', 'PAS_I')," +
                        "('urn:person:t3:0003', 'Alice', 'Tenant 3', 'G_F', 'PAS_A');",

                "INSERT INTO tenant_3.addresses (code, type, address_line_1, address_line_2, country) VALUES" +
                        "('urn:address:t3:1', 'AT_H', '123 Main St', 'Apt 4B', 'USA 3')," +
                        "('urn:address:t3:2', 'AT_O', '456 Business Rd', 'Suite 101', 'USA 3')," +
                        "('urn:address:t3:3', 'AT_H', '789 Beach Ave', 'Cottage 7', 'USA 3');",

                "INSERT INTO tenant_3.contracts (code, type, name, owner) VALUES" +
                        "('urn:contract:t3:1', 'CT_L', 'Lease Agreement', 'urn:person:t3:0001')," +
                        "('urn:contract:t3:2', 'CT_L', 'Employment Contract', 'urn:person:t3:0001')," +
                        "('urn:contract:t3:3', 'CT_S', 'Service Agreement', 'urn:person:t3:0002');",

                // Granting Permissions to Tables
                "DO $$ DECLARE tbl RECORD; BEGIN FOR tbl IN SELECT tablename FROM pg_tables WHERE schemaname = 'tenant_1' LOOP EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO tenant_1;', 'tenant_1', tbl.tablename); END LOOP; END $$;",
                "DO $$ DECLARE tbl RECORD; BEGIN FOR tbl IN SELECT tablename FROM pg_tables WHERE schemaname = 'tenant_2' LOOP EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO tenant_2;', 'tenant_2', tbl.tablename); END LOOP; END $$;",
                "DO $$ DECLARE tbl RECORD; BEGIN FOR tbl IN SELECT tablename FROM pg_tables WHERE schemaname = 'tenant_3' LOOP EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO tenant_3;', 'tenant_3', tbl.tablename); END LOOP; END $$;"
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
