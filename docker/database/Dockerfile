FROM postgres:15

MAINTAINER "HMCTS BSP <https://github.com/hmcts>"
LABEL maintainer = "HMCTS BSP <https://github.com/hmcts>"

COPY init-db.sh /docker-entrypoint-initdb.d

EXPOSE 5432
