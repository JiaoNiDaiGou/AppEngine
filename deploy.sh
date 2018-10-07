#!/bin/bash

#
# Script to deploy dev version
#
# Usage:
#   VERSION=some_version ./deploy-dev.sh
#

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

VERSION="${VERSION:-dev}"

if [ -z ${SERVICE} ]; then
    echo "ERROR: Service name is required. E.g. daigou, songfan"
    exit 1
fi

if [[ ${VERSION} =~ ^(prod.*)$ ]]; then
    echo "ERROR: Cannot deploy a version starts with ${VERSION}. That is reserved!"
    exit 1
fi

echo "

    !! Deploy service '$SERVICE' version '$VERSION' !!

"

# echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

$ROOT/gradlew :${SERVICE}-service/appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "https://$VERSION-dot-$SERVICE-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"
