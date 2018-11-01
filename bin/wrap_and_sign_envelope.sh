#!/bin/sh

INPUT_ZIP_NAME=$1
PRIVATE_KEY_FILE_NAME=$2
ENVELOPE_FILE_NAME=envelope.zip
SIGNATURE_FILE_NAME=signature

if [ "$#" -ne 2 ]; then
  echo "This script takes an unsigned envelope file, signs it and wraps (with the signature)"
  echo "in another file, which is stored in 'signed' subfolder."
  echo "Usage: ./wrap_and_sign_envelopes.sh {envelope file name} {private key file name (pem)}"
  exit 1
fi

if [ ! -f ${INPUT_ZIP_NAME} ]; then
  echo "Envelope file does not exist"
  exit 1
fi

if [ ! -f ${PRIVATE_KEY_FILE_NAME} ]; then
  echo "Private key file does not exist"
  exit 1
fi

mkdir signed
cp ${INPUT_ZIP_NAME} signed/${ENVELOPE_FILE_NAME}
cd signed
openssl dgst -sha256 -sign ../${PRIVATE_KEY_FILE_NAME} -out ${SIGNATURE_FILE_NAME} ${ENVELOPE_FILE_NAME}
zip -m ${INPUT_ZIP_NAME} ${ENVELOPE_FILE_NAME} ${SIGNATURE_FILE_NAME}
cd ..
