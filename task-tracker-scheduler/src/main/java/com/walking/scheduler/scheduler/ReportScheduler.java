package com.walking.scheduler.scheduler;

import com.walking.scheduler.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportScheduler {
    private final ReportGenerationService reportGenerationService;

    @Value("${app.scheduler.batch-size}")
    private final int batchSize;

    @Scheduled(cron = "${app.scheduler.cron}")
    public void sendDailyReport() {
        boolean hasMore = true;

        while (hasMore) {
            hasMore = reportGenerationService.processBatch(batchSize);
        }
    }
}
