#!/bin/bash

set -euo pipefail

pushd $HOME/workspace/credhub-release/src/credhub/
    DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

    rm -rf $DIR/build
    $DIR/setup_dev_mtls.sh
    $DIR/gradlew --no-daemon assemble
    exec $DIR/gradlew --no-daemon bootRun -Djava.security.egd=file:/dev/urandom -Djdk.tls.ephemeralDHKeySize=4096 -Djdk.tls.namedGroups="secp384r1" -Djavax.net.ssl.trustStore=src/test/resources/auth_server_trust_store.jks -Djavax.net.ssl.trustStorePassword=changeit $@
popd
