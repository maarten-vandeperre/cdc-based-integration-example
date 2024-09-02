# connect to postgres pod
oc get pod -n demo-project
oc exec -it $(oc get pod | grep integration-database  | awk '{print $1}') bash