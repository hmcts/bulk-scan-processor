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

    static ServiceBusQueues fromQueueName(String queueName) {
        for (ServiceBusQueues queue : values()) {
            if (queue.getQueueName().equals(queueName)) {
                return queue;
            }
        }

        return null;
    }
}
