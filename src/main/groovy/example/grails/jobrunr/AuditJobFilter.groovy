package example.grails.jobrunr

import example.grails.AuditLog
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.filters.ApplyStateFilter
import org.jobrunr.jobs.states.JobState
import org.springframework.stereotype.Component

/**
 * Custom job filter that logs state transitions to the AuditLog domain class.
 * Registered as a Spring bean — JobRunr's Spring Boot starter auto-discovers it.
 */
@Slf4j
@Component
class AuditJobFilter implements ApplyStateFilter {

    @Override
    void onStateApplied(Job job, JobState oldState, JobState newState) {
        String jobName = job.jobName ?: job.jobDetails?.className ?: 'Unknown'
        String jobId = job.id?.toString() ?: 'N/A'
        String oldStateName = oldState?.name?.toString() ?: 'NONE'
        String newStateName = newState.name.toString()

        log.info("Job [{}] {} state change: {} -> {}", jobId, jobName, oldStateName, newStateName)

        try {
            AuditLog.withNewTransaction {
                new AuditLog(
                    jobId: jobId,
                    jobName: jobName.take(255),
                    oldState: oldStateName.take(50),
                    newState: newStateName.take(50)
                ).save(flush: true, failOnError: true)
            }
        } catch (Exception e) {
            log.warn("Failed to write audit log for job {}: {}", jobId, e.message)
        }
    }
}
