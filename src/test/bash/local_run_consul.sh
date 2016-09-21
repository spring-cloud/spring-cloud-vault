#!/bin/bash

###########################################################################
# Start Consul on localhost:8500                                           #
###########################################################################

BASEDIR=`dirname $0`/../../..

mkdir -p ${BASEDIR}/consul/config
mkdir -p ${BASEDIR}/consul/data

./consul/consul agent -server \
            -bootstrap-expect 1 \
            -data-dir ${BASEDIR}/consul/data \
            -config-file=${BASEDIR}/src/test/bash/consul.json

exit $?
