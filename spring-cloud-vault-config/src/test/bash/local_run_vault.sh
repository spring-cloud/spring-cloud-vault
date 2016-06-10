#!/bin/bash

###########################################################################
# Start Vault on localhost:8200                                           #
###########################################################################

BASEDIR=`dirname $0`/../../..
./vault/vault server -config=${BASEDIR}/src/test/resources/vault.conf
exit $?
