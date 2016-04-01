#!/bin/bash

###########################################################################
# Download and Install Vault                                              #
# This script is prepared for caching of the vault/download directory     #
###########################################################################


VAULT_VER="0.5.2"
UNAME=$(uname -s |  tr '[:upper:]' '[:lower:]')
VAULT_ZIP="vault_${VAULT_VER}_${UNAME}_amd64.zip"
IGNORE_CERTS="${IGNORE_CERTS:-no}"


# cleanup
mkdir -p vault/download
cd vault

if [[ ! -f "download/${VAULT_ZIP}" ]] ; then
    cd download
    # install Vault
    if [[ "${IGNORE_CERTS}" == "no" ]] ; then
      echo "Downloading consul with certs verification"
      wget "https://releases.hashicorp.com/vault/${VAULT_VER}/${VAULT_ZIP}"
    else
      echo "WARNING... Downloading consul WITHOUT certs verification"
      wget "https://releases.hashicorp.com/vault/${VAULT_VER}/${VAULT_ZIP}" --no-check-certificate
    fi

    if [[ $? != 0 ]] ; then
      echo "Cannot download vault"
      exit 1
    fi
    cd ..
fi

if [[ -f vault ]] ; then
  rm vault
fi

unzip download/${VAULT_ZIP}
chmod a+x vault

# check
./vault --version