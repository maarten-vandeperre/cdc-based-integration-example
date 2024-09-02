defaultNamespace=$(cat .namespace)
read -p "Project/namespace name in OpenShift to use [Default: $defaultNamespace]: " projectname
projectname=${projectname:-$defaultNamespace}
read -p "Do yo already have extracted your docker config and stored the file in automated-install folder? (yes/no) [Default: yes]" dockerexported
dockerexported=${dockerexported:-yes}

if [ "$dockerexported" != "yes" ]; then
  echo "Please extract your docker config first. Description on how to can be found here: step 5 of https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/wrap_up_operator_config.MD#metadata-configuration"
  exit 1
fi

read -p "Did yo already create a kafka cluster (with name 'my-cluster') ? (yes/no)" kafkacreated
kafkacreated=${kafkacreated:-yes}

if [ "$kafkacreated" != "yes" ]; then
  echo "Please create a kafka cluster first with name 'my-cluster'."
  echo "Info about installing the kafka operator can be found over here: https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/install_and_configure_the_operators.MD#amq-streams---install-operator"
  echo "Info about creating the kafka cluster 'my-cluster', can be found over here (up until step 5): https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/wrap_up_operator_config.MD#amq-streams---kafka"
  exit 1
fi

echo "Continuing with the script..."
echo "start script in namespace '$projectname'"

echo "########### Configure namespaces in manifest files"
sed -i.bak "s|<NAMESPACE>|$projectname|g" "manifests-step-1/kafka-connect-cluster.yaml"
sed -i.bak "s|<NAMESPACE>|$projectname|g" "manifests-step-1/debezium_postgres_connector.yaml"


echo "########### Create Postgres"
oc new-app \
    -e POSTGRES_USER=integration \
    -e POSTGRES_PASSWORD=averysecurepassword \
    -e POSTGRES_DB=integration \
    -e PGDATA=/tmp/data/pgdata \
    quay.io/appdev_playground/wal_postgres:0.0.2 \
    --name integration-database \
    -n $projectname

echo "########### Create MongoDB"
oc new-app \
    -e MONGO_INITDB_ROOT_USERNAME=mongo \
    -e MONGO_INITDB_ROOT_PASSWORD=mongo \
    mongo:4.2.24 \
    --name aggregation-database \
    -n $projectname

echo "########### Create docker config secret"
oc create secret generic \
   kafka-connect-cluster-push-secret \
   --from-file=.dockerconfigjson=./rh-ee-mvandepe-auth.json \
   --type=kubernetes.io/dockerconfigjson

echo "########### Create Kafka Connect cluster"
oc apply -f manifests-step-1/kafka-connect-cluster.yaml

echo "########### Sleep 5 minutes for Kafka Connect cluster to build"
sleep 300

echo "########### Install Debezium connector"
oc apply -f manifests-step-1/debezium_postgres_connector.yaml

echo "########### Install Camel K integrations"
kamel run ../camel-integrations/src/main/java/demo/integrations/aggregationflow/flow1/PeopleServiceRouteCamelK.java \
        --property postgres-service=integration-database.integration-project-2.svc.cluster.local
kamel run ../camel-integrations/src/main/java/demo/integrations/aggregationflow/flow1/EnrichContractsRouteCamelK.java \
        --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
        --property people-camel-base-endpoint=http://people-service-route-camel-k.demo-project.svc.cluster.local
kamel run ../camel-integrations/src/main/java/demo/integrations/aggregationflow/flow1/MongoStoreRouteCamelK.java \
        --property kafka.bootstrap.servers=my-cluster-kafka-bootstrap.integration-project-2.svc.cluster.local:9092 \
        --property mongo-connection-url="mongodb://mongo:mongo@aggregation-database.integration-project-2.svc.cluster.local:27017/?authSource=admin"