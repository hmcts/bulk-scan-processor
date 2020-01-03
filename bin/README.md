# CLI scripts for Bulk Scan Processor

## Table of contents

1. [Zip and sign and upload](#zip-and-sign-and-upload)

### Zip and sign and upload

Tool `sign-zip-upload.sh` provides the capabilities to zip the directory of your choosing.
It is considered to have correct metafile json and required pdf files.
It may contain anything else but for correctness - script only takes `*.json` and `*.pdf` files.

After all files are zipped, the signing technique is applied and uploaded to `demo` Bulk Scan Demo blob storage.

Note.
`CONTAINER` must be set within the script.
If help required - ask BSP team.

#### Sample command line

```bash
$ ./sign-zip-upload.sh envelope
```

where `envelope` is folder name existing in working directory.

#### Pre-requisites

- Azure CLI
- JQ CLI processor
- &lt;folder-name&gt; e.g. `envelope`
- `CONTAINER=""` set to some existing one in blob storage. It is hardcoded as only needed one value per team
- `SAS_TOKEN` environment variable. Current session where command is executed

#### Commands used

- az
- jq
- fold
- awk
- date
- openssl
- zip
- curl
