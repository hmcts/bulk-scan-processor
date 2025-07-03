FROM hmctspublic.azurecr.io/imported/postgres:17-alpine

MAINTAINER "HMCTS BSP <https://github.com/hmcts>"
LABEL maintainer = "HMCTS BSP <https://github.com/hmcts>"

COPY init-db.sh /docker-entrypoint-initdb.d

EXPOSE 5432
