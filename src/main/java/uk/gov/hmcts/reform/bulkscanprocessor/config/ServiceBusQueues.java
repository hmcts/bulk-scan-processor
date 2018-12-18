package uk.gov.hmcts.reform.bulkscanprocessor.config;

public enum ServiceBusQueues {

    ENVELOPES_PUSH("envelopes"),
    NOTIFICATIONS_PUSH("notifications");

    private final String queueName;

    ServiceBusQueues(String queueName) {
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }
}
