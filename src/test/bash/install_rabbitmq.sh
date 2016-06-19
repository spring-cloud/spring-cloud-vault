#!/bin/bash

###########################################################################
# Download and Install RabbitMQ                                           #
# This script is prepared for caching of the download directory           #
###########################################################################


RABBITMQ_VER="3.6.2"
UNAME=$(uname -s |  tr '[:upper:]' '[:lower:]')

if [[ ${UNAME} == "darwin" ]] ; then
    RABBITMQ_ZIP="rabbitmq-server-mac-standalone-${RABBITMQ_VER}.tar.xz"
else
    echo "Installation of RabbitMQ on ${UNAME} not supported by this script"
    exit 1
fi
IGNORE_CERTS="${IGNORE_CERTS:-no}"

# cleanup
mkdir -p rabbitmq
mkdir -p download

if [[ ! -f "download/${RABBITMQ_ZIP}" ]] ; then
    cd download
    # install Vault
    if [[ "${IGNORE_CERTS}" == "no" ]] ; then
      echo "Downloading RabbitMQ with certs verification"
      wget "https://www.rabbitmq.com/releases/rabbitmq-server/v${RABBITMQ_VER}/${RABBITMQ_ZIP}"
    else
      echo "WARNING... Downloading RabbitMQ WITHOUT certs verification"
      wget "https://www.rabbitmq.com/releases/rabbitmq-server/v${RABBITMQ_VER}/${RABBITMQ_ZIP}" --no-check-certificate
    fi

    if [[ $? != 0 ]] ; then
      echo "Cannot download RabbitMQ"
      exit 1
    fi
    cd ..
fi

cd rabbitmq

if [[ -d ebin ]] ; then
  rm -Rf ../rabbitmq/*
fi

tar xzf ../download/${RABBITMQ_ZIP}
