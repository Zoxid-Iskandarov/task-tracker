package com.walking.scheduler.scheduler;

import com.walking.scheduler.service.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportScheduler {
    private final ReportGeneratorService reportGeneratorService;

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyReport() {
        reportGeneratorService.generateDailyReport();
    }
}
