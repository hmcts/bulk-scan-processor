FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-0.1

ENV APP bulk-scan-processor.jar

COPY build/libs/$APP /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:8581/health || exit 1

EXPOSE 8581

# https://developers.redhat.com/blog/2014/07/22/dude-wheres-my-paas-memory-tuning-javas-footprint-in-openshift-part-2/
ENV JAVA_OPTS "-XX:+PrintFlagsFinal"
ENV JAVA_TOOL_OPTIONS "-XX:+UseContainerSupport -XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=70.0 -XX:MinRAMPercentage=10.0 -XX:+UseParallelOldGC -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xms128M"

#CMD ["\$APP"]
CMD ["bulk-scan-processor.jar"]

