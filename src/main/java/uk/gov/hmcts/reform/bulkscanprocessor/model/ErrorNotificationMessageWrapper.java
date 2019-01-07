package uk.gov.hmcts.reform.bulkscanprocessor.model;

import com.microsoft.azure.servicebus.IMessage;

public class ErrorNotificationMessageWrapper {

    private final IMessage message;

    private final boolean assignedForDeadLettering;

    private final boolean completedAcknowledgement;

    private ErrorNotificationMessageWrapper(
        IMessage message,
        boolean assignedForDeadLettering,
        boolean completedAcknowledgement
    ) {
        this.message = message;
        this.assignedForDeadLettering = assignedForDeadLettering;
        this.completedAcknowledgement = completedAcknowledgement;
    }

    public static ErrorNotificationMessageWrapper forDeadLettering(IMessage message) {
        return new ErrorNotificationMessageWrapper(message, true, false);
    }

    public static ErrorNotificationMessageWrapper forAcknowledgement(IMessage message) {
        return new ErrorNotificationMessageWrapper(message, false, true);
    }

    public static ErrorNotificationMessageWrapper forAbandoning(IMessage message) {
        return new ErrorNotificationMessageWrapper(message, false, false);
    }

    public IMessage getMessage() {
        return message;
    }

    /**
     * Flag which represents the decision whether to move message to dead letter queue or abandon for later processing.
     * @return Flag
     */
    public boolean isAssignedForDeadLettering() {
        return assignedForDeadLettering;
    }

    /**
     * Flag which represents completion of message processing.
     * @return Flag
     */
    public boolean isCompletedAcknowledgement() {
        return completedAcknowledgement;
    }
}
