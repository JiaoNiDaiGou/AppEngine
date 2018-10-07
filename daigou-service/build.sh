#!/bin/bash

#
# Script to build the whole project
#

set -o pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

echo "Fetching secrets ..."
$ROOT/scripts/fetch_secrets.sh

../gradlew cleanIdea clean idea build
