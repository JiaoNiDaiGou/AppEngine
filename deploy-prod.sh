#!/bin/bash

#
# Script to deploy prod version
#
# Usage:
#   SERVICE=daigou ./deploy-prod.sh

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

VERSION="prod"

echo "

    !! Deploy service '$SERVICE' version '$VERSION' !!

"

# echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

$ROOT/gradlew :${SERVICE}-service:service:appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "https://$VERSION-dot-$SERVICE-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"

