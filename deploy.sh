#!/bin/bash

#
# Script to deploy dev version
#
# Usage:
#   SERVICE=daigou VERSION=some_version ./deploy-dev.sh
#

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

if [ -z "${SERVICE:-}" ]; then
    SERVICE_LIST=("daigou" "songfan")
    for (( c=0; c<${#SERVICE_LIST[@]}; c++ ))
    do
        echo "[$c] ${SERVICE_LIST[c]}"
    done
    echo "Select SERVICE, and press [ENTER]"
    read service_selection
    SERVICE=${SERVICE_LIST[service_selection]}
    echo "SERVICE: $SERVICE"
fi

if [ -z "${VERSION:-}" ]; then
    VERSION_LIST=("dev" "prod")
    for (( c=0; c<${#VERSION_LIST[@]}; c++ ))
    do
        echo "[$c] ${VERSION_LIST[c]}"
    done
    echo "Select VERSION, and press [ENTER]"
    read version_selection
    VERSION=${VERSION_LIST[version_selection]}
    echo "VERSION: $VERSION"
fi

echo "

    !! Deploy service '$SERVICE' version '$VERSION' !!

"

# Copy shared configs into $service/WEB-INF
cp $ROOT/appengine_shared/cron.xml.template $ROOT/daigou-service/daigou-appengine/src/main/webapp/WEB-INF/
cp $ROOT/appengine_shared/queue.xml.template $ROOT/daigou-service/daigou-appengine/src/main/webapp/WEB-INF/
cp $ROOT/appengine_shared/cron.xml.template $ROOT/songfan-service/songfan-appengine/src/main/webapp/WEB-INF/
cp $ROOT/appengine_shared/queue.xml.template $ROOT/songfan-service/songfan-appengine/src/main/webapp/WEB-INF/

# echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

$ROOT/gradlew :${SERVICE}-service:${SERVICE}-appengine:appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "https://$VERSION-dot-$SERVICE-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"
