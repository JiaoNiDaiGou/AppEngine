#!/bin/bash

#
# This script is to fetch secrets from Google GCS, and copy it to common/src/main/resources/secrets
# The key is managed by KMS.
#
# Prerequisites:
#  1. Install gcloud cli (https://cloud.google.com/sdk/docs/quickstart-macos).
#
# Usage:
#   ./fetch_secrets.sh

set -o pipefail

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

KEY_RING="jiaonidaigou-secrets"
KEY_VERSION=1
KEY_NAME="jiaonidaigou-secrets"
SECRETS_BUCKET=$PROJECT_ROOT_BUCKET/secrets
TEMP_CIPHER_DIR=/tmp/jiaonidaigou_secrets/tmp
LOCAL_SECRETS_DIR=$ROOT/common/src/main/resources/secrets

mkdir -p $TEMP_CIPHER_DIR

trap cleanup EXIT ERR

cleanup() {
    rm -rf $TEMP_CIPHER_DIR
}

if ! gcloud kms keyrings describe $KEY_RING --location=global --project $PROJECT_ID; then
    echo "No keyring $KEY_RING exists. Please publish secrets first"
    exit 1
fi

if ! gcloud kms keys describe $KEY_NAME --keyring=$KEY_RING --location=global --project $PROJECT_ID; then
    echo "No key $KEY_NAME exists. Please publish secrets first"
    exit 1
fi

if ! gsutil ls -L gs://$SECRETS_BUCKET; then
    echo "No secrets bucket gs://$SECRETS_BUCKET exists. Please publish secrets first"
    exit 1
fi

for gcs_obj in $(gsutil ls gs://$SECRETS_BUCKET/$KEY_RING/$KEY_NAME/$KEY_VERSION); do
    filenameOnly="$(basename $gcs_obj)"

    echo "Preparing $filenameOnly ..."

    tmp_enc_file=$TEMP_CIPHER_DIR/$filenameOnly
    gsutil cp $gcs_obj $tmp_enc_file
    gcloud kms decrypt --keyring=$KEY_RING --key=$KEY_NAME --location=global \
           --plaintext-file=$LOCAL_SECRETS_DIR/$filenameOnly \
           --ciphertext-file=$tmp_enc_file --project $PROJECT_ID
done
