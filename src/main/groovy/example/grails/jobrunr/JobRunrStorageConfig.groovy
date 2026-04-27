package example.grails.jobrunr

import org.jobrunr.jobs.filters.JobFilter
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.server.BackgroundJobServer
import org.jobrunr.storage.StorageProvider
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import jakarta.annotation.PostConstruct
import javax.sql.DataSource

/**
 * Bridges the Grails-managed DataSource with JobRunr.
 *
 * Grails bypasses Spring Boot's DataSourceAutoConfiguration, so JobRunr's
 * auto-configured StorageProvider can't find the DataSource. We create it
 * explicitly here.
 *
 * The JobMapper must be set eagerly because the RecurringJobPostProcessor
 * (which handles @Recurring annotations) runs during bean initialization,
 * before JobRunr's auto-configuration would normally set the mapper.
 *
 * Job filters (like AuditJobFilter) are registered on the BackgroundJobServer
 * after it's created by the auto-configuration, since the auto-config doesn't
 * inject filter beans automatically.
 */
@Configuration
class JobRunrStorageConfig {

    @Bean
    StorageProvider storageProvider(DataSource dataSource) {
        StorageProvider provider = SqlStorageProviderFactory.using(dataSource, null, DatabaseOptions.CREATE)
        provider.setJobMapper(new JobMapper(new JacksonJsonMapper()))
        return provider
    }

    @Bean
    JobFilterRegistrar jobFilterRegistrar(BackgroundJobServer backgroundJobServer, List<JobFilter> jobFilters) {
        return new JobFilterRegistrar(backgroundJobServer, jobFilters)
    }

    static class JobFilterRegistrar {
        JobFilterRegistrar(BackgroundJobServer server, List<JobFilter> filters) {
            server.setJobFilters(filters)
        }
    }
}
