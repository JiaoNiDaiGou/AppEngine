#!/bin/bash

#
# Script to deploy dev version
#
# Usage:
#   SERVICE=daigou VERSION=some_version ./deploy-dev.sh
#

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"


. ./exports.sh

version="${1:-dev}"
echo "Deploy ${version}"

$ROOT/gradlew :core-service:appengineUpdate -PgaeAppId=${PROJECT_ID} -PgaeVersion=${VERSION}
#
#open "https://${VERSION}-dot-daigou-dot-${PROJECT_ID}.appspot.com/api/ping?input=helloworld"
