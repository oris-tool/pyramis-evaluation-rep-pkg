#!/bin/bash

# Colori per output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Funzione per controllare se il comando precedente ha avuto successo
check_status() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Success${NC}"
    else
        echo -e "${RED}✗ Failed${NC}"
        exit 1
    fi
}

echo "Installing hierarchicalsmp dependency..."
mvn clean install:install-file \
  -Dfile=libs/hierarchicalsmp-1.0.0.jar \
  -DgroupId=it.unifi \
  -DartifactId=hierarchicalsmp \
  -Dversion=1.0.0 \
  -Dpackaging=jar

check_status

echo "Building and installing main project..."
mvn clean install

check_status

echo -e "${GREEN}All installations completed successfully!${NC}"