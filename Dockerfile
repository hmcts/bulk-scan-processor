ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Build image

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY lib/applicationinsights-agent-2.5.1.jar lib/AI-Agent.xml /opt/app/
COPY build/libs/bulk-scan-processor.jar /opt/app/

EXPOSE 8581

CMD ["bulk-scan-processor.jar"]
