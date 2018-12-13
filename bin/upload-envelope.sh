#!/bin/bash

export SAS_TOKEN='?sv=2018-03-28&ss=b&srt=sco&sp=rwdlac&se=2018-12-07T21:12:05Z&st=2018-12-07T13:12:05Z&spr=https,http&sig=Jwx7LsdiX9tUAoRaTmbzFZonTD8JFDWdlYFJGto7HAs%3D'
for payload in $1; do
        curl -X PUT -T $payload -H "x-ms-date: $(date -u)" -H "x-ms-blob-type: BlockBlob"  "https://bulkscan.aat.platform.hmcts.net/sscs/$payload$SAS_TOKEN" -i
done
