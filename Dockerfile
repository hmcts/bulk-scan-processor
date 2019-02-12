FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-1.0

COPY build/libs/bulk-scan-processor.jar /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:8581/health || exit 1

EXPOSE 8581

CMD ["bulk-scan-processor.jar"]

