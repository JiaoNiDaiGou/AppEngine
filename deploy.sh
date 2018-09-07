#!/bin/bash

#
# Script to build the whole project
#

set -o pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

VERSION="${1:-jiaonidaigou}"

echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

./gradlew :api:appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION
# ./gradlew :api:appengineUpdateQueues -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION
# ./gradlew :api:appengineUpdateCron -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "http://$VERSION-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"
