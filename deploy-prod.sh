#!/bin/bash

#
# Script to deploy prod version
#   ./deploy-prod.sh
#

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

VERSION="prod"

echo "

    !! Deploy version ''$VERSION' !!

"

# echo "Fetching secrets ..."
# $ROOT/scripts/fetch_secrets.sh

../gradlew :api:appengineUpdate -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION

open "https://$VERSION-dot-$PROJECT_ID.appspot.com/api/ping?input=helloworld"
