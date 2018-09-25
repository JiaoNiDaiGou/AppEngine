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

if [[ ${VERSION} =~ ^(prod.*)$ ]]; then
    echo "ERROR: Cannot deploy a version starts with ${VERSION}. That is reserved!"
    exit 1
fi

# echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

./gradlew :api:appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "https://$VERSION-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"
