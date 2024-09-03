# connect to postgres pod
oc get pod -n demo-project
oc exec -it $(oc get pod | grep integration-database  | awk '{print $1}') bash


oc create secret generic \
    kafka-connect-cluster-push-secret \
    --from-file=.dockerconfigjson=./rh-ee-mvandepe-auth.json \
    --type=kubernetes.io/dockerconfigjson
