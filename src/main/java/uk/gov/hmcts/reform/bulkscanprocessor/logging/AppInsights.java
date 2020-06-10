package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.lang.System.currentTimeMillis;

@Aspect
@Component
public class AppInsights {

    private final TelemetryClient telemetryClient;

    public AppInsights(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void aroundSchedule(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestTelemetryContext requestTelemetry =  new RequestTelemetryContext(currentTimeMillis(), null);
        boolean success = false;

        try {
            joinPoint.proceed();

            success = true;
        } finally {
            handleRequestTelemetry(requestTelemetry, joinPoint.getTarget().getClass().getSimpleName(), success);
        }
    }

    private void handleRequestTelemetry(
        RequestTelemetryContext requestTelemetryContext,
        String caller,
        boolean success
    ) {
        String requestName = "Schedule /" + caller;

        RequestTelemetry requestTelemetry = requestTelemetryContext.getHttpRequestTelemetry();
        if (requestTelemetry != null) {
            requestTelemetry.setName(requestName);
            requestTelemetry.setDuration(new Duration(
                currentTimeMillis() - requestTelemetryContext.getRequestStartTimeTicks())
            );

            requestTelemetry.setSuccess(success);

            telemetryClient.trackRequest(requestTelemetry);
        }

    }

}
