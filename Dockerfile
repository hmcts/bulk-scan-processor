ARG APP_INSIGHTS_AGENT_VERSION=2.5.0

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0

COPY lib/applicationinsights-agent-2.5.0.jar lib/AI-Agent.xml /opt/app/
COPY build/libs/bulk-scan-processor.jar /opt/app/

EXPOSE 8581

CMD ["bulk-scan-processor.jar"]

