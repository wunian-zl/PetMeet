package org.petmeet.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.service.impl.OmsOrderServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCloseJob {

    private final OmsOrderServiceImpl orderService;

    @Value("${app.order.pay-timeout-minutes:30}")
    private Integer payTimeoutMinutes;

    @Scheduled(cron = "${app.order.auto-close-cron:0 * * * * ?}")
    public void autoCloseUnpaidOrders() {
        int timeout = payTimeoutMinutes == null || payTimeoutMinutes <= 0 ? 30 : payTimeoutMinutes;
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeout);
        int closed = orderService.autoCloseExpiredUnpaidOrders(deadline);
        if (closed > 0) {
            log.info("Auto closed {} unpaid orders before {}", closed, deadline);
        }
    }
}
