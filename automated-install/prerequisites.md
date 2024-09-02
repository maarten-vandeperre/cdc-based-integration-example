# Prerequisites
1. You have an OpenShift cluster.
2. Your oc command line is authenticated (i.e., oc login ...).
3. You have a docker config. Description on how to can be found here: step 5 of https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/wrap_up_operator_config.MD#metadata-configuration
4. You have a Kafka cluster 'my-cluster'.
   1. Info about installing the kafka operator can be found over here: https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/install_and_configure_the_operators.MD#amq-streams---install-operator
   2. Info about creating the kafka cluster 'my-cluster', can be found over here (up until step 5): https://github.com/maarten-vandeperre/knative-serverless-example-workshop/blob/main/workshop/wrap_up_operator_config.MD#amq-streams---kafka
5. You have installed the Camel K operator