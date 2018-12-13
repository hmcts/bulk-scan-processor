#!/bin/bash

export SAS_TOKEN='?sv=2018-03-28&ss=b&srt=sco&sp=rwdlac&se=2018-12-11T23:26:36Z&st=2018-12-10T10:26:36Z&spr=https&sig=o90ZBrEAZlsqZIi7Qm4DhtvMhmxhZvNbhSDsPN2%2Bj0c%3D'

#?sv=2018-03-28&ss=b&srt=sco&sp=rwdlac&se=2018-12-10T23:26:36Z&st=2018-12-10T15:26:36Z&spr=https&sig=KorHawwJgRF017MQiSsZGD1VXJHZQ6lsZ7lmReDiflw%3D'

for payload in $1; do
	echo $payload
        curl -v -X PUT -T $payload -H "x-ms-date: $(date -u)" -H "x-ms-blob-type: BlockBlob"  "https://bulkscan.aat.platform.hmcts.net/sscs/$payload$SAS_TOKEN" -i

done
