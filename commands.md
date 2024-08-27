# Temp create postgres
oc new-app \
    -e POSTGRES_USER=integration \
    -e POSTGRES_PASSWORD=averysecurepassword \
    -e POSTGRES_DB=integration \
    -e PGDATA=/tmp/data/pgdata \
    quay.io/appdev_playground/wal_postgres:0.0.2 \
    --name integration-database

# Temp mongo
oc new-app \
    -e MONGO_INITDB_ROOT_USERNAME=mongo \
    -e MONGO_INITDB_ROOT_PASSWORD=mongo \
    mongo:4.2.24 \
    --name integration-mongo

# Connect to CDC topic
oc exec -it my-cluster-kafka-0 \
    -- bin/kafka-console-consumer.sh \
    --bootstrap-server my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
    --topic legacydatachanged.public.people

# Check topics created by Debezium/Kafka Connect
oc exec -i my-connect-cluster-connect-0 -- curl -X GET \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://my-connect-cluster-connect-0:8083/connectors/postgres-debezium-connector/topics | jq

oc exec -i my-connect-cluster-connect-0 -- curl -s -X GET \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://my-connect-cluster-connect-0:8083/connectors/postgres-debezium-connector/status | jq

# Listen to new topic
oc exec -it my-cluster-kafka-0 \
        -- bin/kafka-console-consumer.sh \
        --bootstrap-server my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
        --topic processedTopic


