# CDC Based Integration Example

This project will showcase CDC (i.e., change data capture) integration patterns on OpenShift, making use of Kafka, Debezium, Camel and Knative.

## Data outline
The [initial data set](openshift-manifest/populate_database.sql) consists of a general schema, containing generic identifiers and three tenant 
schemas, all containing a contracts, people and addresses table (and a many-to-many link table in between people and addresses).

### Identifiers
```sql
select * from public.identifiers;
```
![](images/identifiers_data.png '')

### Contracts data
```sql
select tenant_database, code, type, name, owner
from (
         select 'tenant_1' as tenant_database, c.* from tenant_1.contracts c
         union
         select 'tenant_2' as tenant_database, c.* from tenant_2.contracts c
         union
         select 'tenant_3' as tenant_database, c.* from tenant_3.contracts c
     ) as tmp
order by tenant_database, code
;
```
![](images/contracts_data.png '')

### People data
```sql
select tenant_database, code, first_name, last_name, gender, status
from (
         select 'tenant_1' as tenant_database, c.* from tenant_1.people c
         union
         select 'tenant_2' as tenant_database, c.* from tenant_2.people c
         union
         select 'tenant_3' as tenant_database, c.* from tenant_3.people c
     ) as tmp
order by tenant_database, code
;
```
![](images/people_data.png '')

### Address data
```sql
select tenant_database, code, type, address_line_1, address_line_2, country
from (
         select 'tenant_1' as tenant_database, c.* from tenant_1.addresses c
         union
         select 'tenant_2' as tenant_database, c.* from tenant_2.addresses c
         union
         select 'tenant_3' as tenant_database, c.* from tenant_3.addresses c
     ) as tmp
order by tenant_database, code
;
```
![](images/addresses_data.png '')

## Integration (flows)

### 1. Debezium - Kafka - Aggregation in one code file
In this flow, we listen with Debezium for changes in contracts tables (all 3 tenants). Whenever 
a change occurs, a change message is put on a Kafka topic. A Camel integration is listening on 
this topic and enriches the data with people data (i.e., exposed through another Camel integration).
The enriched data is put on a second Kafka topic. A third Camel integration is listening on this 
enriched data topic and stores the data in an aggregation database (i.e., MongoDB). The integrations are
tenant agnostic, but can extract a tenant identifier to authenticate against the appropriate schemas.

![](images/cdc_flow_1.jpg '')

**Flow setup**
!! Be aware that you need to change to your base url.
1. 
    ```shell
    kamel run src/main/java/demo/integrations/aggregationflow/flow1/PeopleServiceRouteCamelK.java \
            --property postgres-service=integration-database.integration-project-2.svc.cluster.local;
   
    kamel log people-service-route-camel-k
    ```
   The following curl command should now return data:
    ```shell
    curl \
    --location 'http://people-service-route-camel-k-integration-project-2.apps.cluster-475kf.475kf.sandbox268.opentlc.com/people/urn:person:t1:0001'
    ```
2.
    ```shell
    kamel run src/main/java/demo/integrations/aggregationflow/flow1/EnrichContractsRouteCamelK.java \
            --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
            --property people-camel-endpoint=http://people-service-route-camel-k-integration-project-2.apps.cluster-475kf.475kf.sandbox268.opentlc.com;
    
    kamel log enrich-contracts-route-camel-k
3.
    ```shell
    kamel run src/main/java/demo/integrations/aggregationflow/flow1/MongoStoreRouteCamelK.java \
          --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
          --property mongo-connection-url="mongodb://mongo:mongo@aggregation-database.integration-project-2.svc.cluster.local:27017/?authSource=admin"

    kamel kamel log mongo-store-route-camel-k
    ```
4. Running the following update on the postgres database should result in:
    ```sql
    update tenant_1.contracts set name = 'Lease Agreement - updated' where code = 'urn:contract:t1:1';
    update tenant_2.contracts set name = 'Lease Agreement - updated 2' where code = 'urn:contract:t2:1';
    update tenant_3.contracts set name = 'Lease Agreement - updated' where code = 'urn:contract:t3:1';
    ```
   1. A message on the Kafka topic for CDC:
   ```shell
    oc exec -it my-cluster-kafka-0 \
        -- bin/kafka-console-consumer.sh \
        --bootstrap-server my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
        --topic legacydatachanged.tenant_2.contracts
    ```
   2. A message on the Kafka topic for enrichment:
   ```shell
    oc exec -it my-cluster-kafka-0 \
        -- bin/kafka-console-consumer.sh \
        --bootstrap-server my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
        --topic enriched_data
    ```
   3. A new document in the MongoDB database 'aggregation-database'.


### 2. Debezium - Kafka - Aggregation in one code file (with keeping aggregation data in sync)
This flow is an extension on [1. Debezium - Kafka - Aggregation in one code file](#1-debezium---kafka---aggregation-in-one-code-file):
The first flow was not complete as the enriched person data was not synced when it changed in the master database. This is solved in this
integration flow, by adding a second Debezium connector.

![](images/cdc_flow_1b.jpg '')

## Installation/Configuration
TODO, manifest files can be found in [this folder](openshift-manifest) and a summier description
can be found in [commands.md](commands.md)