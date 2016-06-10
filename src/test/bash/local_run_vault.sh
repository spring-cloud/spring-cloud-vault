#!/bin/bash

###########################################################################
# Start Vault on localhost:8200                                           #
###########################################################################

BASEDIR=`dirname $0`/../../..
./vault/vault server -config=${BASEDIR}/spring-cloud-vault-config/src/test/resources/vault.conf
exit $?
