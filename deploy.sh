#!/bin/bash

#
# Script to build the whole project
#
# Usage:
#   VERSION=some_version ./deploy.sh

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

VERSION="${VERSION:-jiaonidaigou}"

# echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

./gradlew :api:appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION
# ./gradlew :api:appengineUpdateQueues -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION
# ./gradlew :api:appengineUpdateCron -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "https://$VERSION-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"
