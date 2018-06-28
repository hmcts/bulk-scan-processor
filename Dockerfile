FROM openjdk:8-jre

COPY build/bootScripts/bulk-scan-processor /opt/app/bin/

COPY build/libs/bulk-scan-processor.jar /opt/app/lib/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:8581/health

EXPOSE 8581

ENTRYPOINT ["/opt/app/bin/bulk-scan-processor"]
