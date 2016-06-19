#!/bin/bash

###########################################################################
# Start RabbitMQ on localhost                                             #
###########################################################################

BASEDIR=`dirname $0`/../../..

RABBITMQ_VER="3.6.2"

cd rabbitmq/rabbitmq_server-${RABBITMQ_VER}

if [[ ! -f etc/rabbitmq/rabbitmq.config ]] ; then
    cp etc/rabbitmq/rabbitmq.config.example etc/rabbitmq/rabbitmq.config
fi

sbin/rabbitmq-plugins enable rabbitmq_management
sbin/rabbitmq-server

exit $?
