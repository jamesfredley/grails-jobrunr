package example.grails.jobrunr

import example.grails.AuditLog
import groovy.util.logging.Slf4j
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.filters.ApplyStateFilter
import org.jobrunr.jobs.states.JobState
import org.springframework.stereotype.Component

/**
 * Custom job filter that logs state transitions to the AuditLog domain class.
 *
 * Registered as a Spring @Component, but JobRunr's Spring Boot 3 starter does NOT
 * auto-discover JobFilter beans - the BackgroundJobServer is wired only with the
 * built-in RetryFilter. Custom filters are pushed onto the BackgroundJobServer by
 * JobRunrStorageConfig.JobFilterRegistrar, which runs after the BackgroundJobServer
 * bean is constructed.
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
