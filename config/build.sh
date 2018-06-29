#!/bin/bash

./src/test/bash/install_vault.sh
./src/test/bash/create_certificates.sh
./src/test/bash/local_run_vault.sh &
./mvnw clean install -Pdocs
