#!/bin/bash

#
# Script to build the whole project
#
# Usage:
#   VERSION=some_version ./deploy.sh

set -euo pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

VERSION="${VERSION:-dev}"

./gradlew :api:appengineRollback -PgaeAppId=$PROJECT_ID -PgaeVersion=$VERSION
