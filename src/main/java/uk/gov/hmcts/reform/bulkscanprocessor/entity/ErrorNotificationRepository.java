package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorNotificationRepository extends JpaRepository<ErrorNotification, Long> {
}
