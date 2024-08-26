oc create secret generic \
        kafka-connect-cluster-push-secret \
        --from-file=.dockerconfigjson=./rh-ee-mvandepe-auth.json \
        --type=kubernetes.io/dockerconfigjson