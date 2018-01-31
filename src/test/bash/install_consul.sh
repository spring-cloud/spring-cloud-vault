#!/bin/bash

###########################################################################
# Download and Install Consul                                             #
# This script is prepared for caching of the download directory           #
###########################################################################


CONSUL_VER="1.0.3"
UNAME=$(uname -s |  tr '[:upper:]' '[:lower:]')
CONSUL_ZIP="consul_${CONSUL_VER}_${UNAME}_amd64.zip"
IGNORE_CERTS="${IGNORE_CERTS:-no}"

# cleanup
mkdir -p consul
mkdir -p download

if [[ ! -f "download/${CONSUL_ZIP}" ]] ; then
    cd download
    # install Vault
    if [[ "${IGNORE_CERTS}" == "no" ]] ; then
      echo "Downloading Consul with certs verification"
      wget "https://releases.hashicorp.com/consul/${CONSUL_VER}/${CONSUL_ZIP}"
    else
      echo "WARNING... Downloading Consul WITHOUT certs verification"
      wget "https://releases.hashicorp.com/consul/${CONSUL_VER}/${CONSUL_ZIP}" --no-check-certificate
    fi

    if [[ $? != 0 ]] ; then
      echo "Cannot download Consul"
      exit 1
    fi
    cd ..
fi

cd consul

if [[ -f consul ]] ; then
  rm consul
fi

unzip ../download/${CONSUL_ZIP}
chmod a+x consul

# check
./consul --version
