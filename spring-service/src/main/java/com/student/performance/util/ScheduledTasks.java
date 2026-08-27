package com.student.performance.util;

import com.student.performance.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final TestService testService;

    public ScheduledTasks(TestService testService) {
        this.testService = testService;
    }

    @Scheduled(fixedDelay = 60000)
    public void expireTimedOutTests() {
        try {
            testService.expireTimedOutTests();
        } catch (Exception ex) {
            log.warn("Failed to expire timed-out tests: {}", ex.getMessage());
        }
    }
}
