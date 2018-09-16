#!/bin/bash

#
# This script is to push secrets into Google GCS.
# The key is managed by KMS.
#
# Prerequisites:
#  1. Install gcloud cli (https://cloud.google.com/sdk/docs/quickstart-macos).
#
# Usage:
#   SECRETS_DIR=<directory_for_secrets> ./push_secrets.sh

set -o pipefail

if [ -z "$SECRETS_DIR" ]; then
    echo "SECRETS_DIR must be provided."
    exit 1
fi

export ROOT="${ROOT:-$(git rev-parse --show-toplevel)}"
. $ROOT/scripts/exports.sh

KEY_RING="jiaonidaigou-secrets"
KEY_VERSION=1
KEY_NAME="jiaonidaigou-secrets"
SECRETS_BUCKET=$PROJECT_ROOT_BUCKET/secrets
TEMP_CIPHER_DIR=/tmp/jiaonidaigou_secrets/tmp

mkdir -p $TEMP_CIPHER_DIR

trap cleanup EXIT ERR

cleanup() {
    rm -rf $TEMP_CIPHER_DIR
}

# Create keyring if necessary
if ! gcloud kms keyrings describe $KEY_RING --location=global --project $PROJECT_ID; then
    echo "Create key ring $KEY_RING ..."
    gcloud kms keyrings create $KEY_RING --location=global --project $PROJECT_ID
fi

# Create key if necessary
if ! gcloud kms keys describe $KEY_NAME --keyring=$KEY_RING --location=global --project $PROJECT_ID; then
    echo "Create key $KEY_NAME ..."
    gcloud kms keys create $KEY_NAME --keyring=$KEY_RING \
           --purpose=encryption --location=global \
           --project $PROJECT_ID
fi

# Create bucket if necessary
if ! gsutil ls -L gs://$SECRETS_BUCKET; then
    gsutil mb -l us -c multi_regional -p $PROJECT_ID gs://$SECRETS_BUCKET
    gsutil acl set private gs://$SECRETS_BUCKET
    gsutil acl ch -g jiaonidaigou@googlegroups.com:O gs://$SECRETS_BUCKET
fi

for rawfile in $SECRETS_DIR/*; do
    rawfilename="$(basename $rawfile)"
    cipher_file=$TEMP_CIPHER_DIR/$rawfilename.enc

    # Encrypt key
    gcloud kms encrypt --keyring=$KEY_RING --key=$KEY_NAME --location=global --version=$KEY_VERSION \
           --plaintext-file=$rawfile --ciphertext-file=$cipher_file --project $PROJECT_ID

    gcs_obj=gs://$SECRETS_BUCKET/$KEY_RING/$KEY_NAME/$KEY_VERSION/$rawfilename
    gsutil cp $cipher_file $gcs_obj
    gsutil acl set private $gcs_obj
    gsutil acl ch -g jiaonidaigou@googlegroups.com:O $gcs_obj
done

echo $SECRETS_BUCKET
