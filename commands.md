# Temp create postgres
oc new-app \
    -e POSTGRES_USER=integration \
    -e POSTGRES_PASSWORD=averysecurepassword \
    -e POSTGRES_DB=integration \
    -e PGDATA=/tmp/data/pgdata \
    quay.io/appdev_playground/wal_postgres:0.0.2 \
    --name integration-database

**Port forwarding to the postgres database**
oc port-forward $(oc get pod | grep integration-database  | awk '{print $1}') 5432:5432

# Temp mongo
oc new-app \
    -e MONGO_INITDB_ROOT_USERNAME=mongo \
    -e MONGO_INITDB_ROOT_PASSWORD=mongo \
    mongo:4.2.24 \
    --name aggregation-database

**Port forwarding to the mongo database**
oc port-forward $(oc get pod | grep aggregation-database  | awk '{print $1}') 27017:27017

# Populate database
Run openshift-manifest > populate_database.sql on the postgres (integration) database

# Create a container push secret
See: https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/wrap_up_operator_config.MD
```shell
oc create secret generic \
   kafka-connect-cluster-push-secret \
   --from-file=.dockerconfigjson=./rh-ee-mvandepe-auth.json \
   --type=kubernetes.io/dockerconfigjson
```

# Set up Debezium
* oc apply -f openshift-manifest/operator-group.yaml
* oc apply -f openshift-manifest/kafka-operator.yaml
* oc apply -f openshift-manifest/kafka-cluster.yaml
* oc apply -f openshift-manifest/kafka-connect-cluster.yaml
* oc apply -f openshift-manifest/debezium_postgres_connector.yaml

# Check topics created by Debezium/Kafka Connect
oc exec -i my-connect-cluster-connect-0 -- curl -X GET \
-H "Accept:application/json" \
-H "Content-Type:application/json" \
http://my-connect-cluster-connect-0:8083/connectors/postgres-debezium-connector/topics | jq

oc exec -i my-connect-cluster-connect-0 -- curl -s -X GET \
-H "Accept:application/json" \
-H "Content-Type:application/json" \
http://my-connect-cluster-connect-0:8083/connectors/postgres-debezium-connector/status | jq

# Connect to CDC topic
oc exec -it my-cluster-kafka-0 \
    -- bin/kafka-console-consumer.sh \
    --bootstrap-server my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
    --topic legacydatachanged.tenant_1.contracts

Change a field in a contract record and see the message come through.
E.g., 
```sql
update tenant_1.contracts set name = 'Lease Agreement - updated' where code = 'urn:contract:t1:1';
```

# Start first Camel(K) route
* kamel run src/main/java/demo/integrations/aggregationflow/PeopleServiceRouteCamelK.java
* kamel get
* kamel log people-service-route-camel-k

# Test first Camel(K) route
```shell
curl \
    --location 'http://people-service-route-camel-k-integration-project-2.apps.cluster-475kf.475kf.sandbox268.opentlc.com/people/urn:person:t1:0001' 
```

# Start second Camel(K) route
* kamel run src/main/java/demo/integrations/aggregationflow/EnrichContractsRouteCamelK.java --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 
* kamel get 
* kamel log enrich-contracts-route-camel-k

# Listen to new topic
oc exec -it my-cluster-kafka-0 \
        -- bin/kafka-console-consumer.sh \
        --bootstrap-server my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
        --topic enriched_data

Change a field in a contract record and see the message come through.
E.g.,
```sql
update tenant_1.contracts set name = 'Lease Agreement - updated' where code = 'urn:contract:t1:1';
```

# Start third Camel(K) route
* kamel run src/main/java/demo/integrations/aggregationflow/MongoStoreRouteCamelK.java --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092
* kamel get
* kamel log mongo-store-route-camel-k
