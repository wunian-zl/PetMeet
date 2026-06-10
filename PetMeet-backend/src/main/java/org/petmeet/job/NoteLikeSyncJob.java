package org.petmeet.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.service.CmsNoteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteLikeSyncJob {

    private final CmsNoteService cmsNoteService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void syncLikeCount() {
        try {
            cmsNoteService.syncLikeCountToDb();
        } catch (Exception e) {
            log.warn("sync like count failed", e);
        }
    }
}
