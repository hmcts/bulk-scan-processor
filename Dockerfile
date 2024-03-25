ARG APP_INSIGHTS_AGENT_VERSION=3.4.8

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/

COPY build/libs/bulk-scan-processor.jar /opt/app/

EXPOSE 8581

CMD ["bulk-scan-processor.jar"]
