# Using JobRunr with Grails

This guide walks you through integrating [JobRunr](https://www.jobrunr.io/) - a distributed background job processing library - with [Apache Grails](https://grails.org/) 8. By the end you will have a working demo app that showcases every major JobRunr OSS feature: fire-and-forget jobs, scheduled jobs, recurring jobs, retries, progress tracking, job filters, and the built-in dashboard.

The demo uses an e-commerce order processing theme with domain classes for orders, products, and audit logs.

## Prerequisites

- **Java 21 or later** (Grails 8 minimum)
- No Grails CLI required - the project includes the Gradle wrapper

> **For Grails 7.1.0**: pin `grailsVersion=7.1.0` in `gradle.properties` and swap `jobrunr-spring-boot-4-starter` for `jobrunr-spring-boot-3-starter` (same version). The rest of the demo works unchanged on JDK 17+.

## Quick Start

```bash
cd grails-jobrunr
./gradlew bootRun
```

- **Demo app**: http://localhost:8080 — page with buttons to trigger each job type
- **JobRunr Dashboard**: http://localhost:8000/dashboard — monitor jobs, view progress, inspect retries

---

## Table of Contents

1. [Project Setup](#1-project-setup)
2. [Bridging Grails and JobRunr](#2-bridging-grails-and-jobrunr)
3. [Configuration](#3-configuration)
4. [Why Groovy Cannot Use JobRunr's Lambda API](#4-why-groovy-cannot-use-jobrunrs-lambda-api)
5. [The JobRequest Pattern](#5-the-jobrequest-pattern)
6. [Fire-and-Forget Jobs](#6-fire-and-forget-jobs)
7. [Scheduled Jobs](#7-scheduled-jobs)
8. [Recurring Jobs](#8-recurring-jobs)
9. [The @Job Annotation](#9-the-job-annotation)
10. [Job Progress and Dashboard Logging](#10-job-progress-and-dashboard-logging)
11. [Automatic Retries](#11-automatic-retries)
12. [Job Filters](#12-job-filters)
13. [Bulk Enqueueing](#13-bulk-enqueueing)
14. [GORM in Background Jobs](#14-gorm-in-background-jobs)
15. [The Dashboard](#15-the-dashboard)
16. [Production Considerations](#16-production-considerations)
17. [Pitfalls Reference](#17-pitfalls-reference)

---

## 1. Project Setup

### Dependencies

Add these to `build.gradle`:

```groovy
dependencies {
    // Grails Web Starter
    implementation 'org.apache.grails:grails-dependencies-starter-web'

    // GORM Hibernate — you need BOTH artifacts
    implementation 'org.apache.grails:grails-data-hibernate5'           // Grails plugin
    implementation 'org.apache.grails:grails-data-hibernate5-spring-boot' // Spring Boot auto-config
    implementation 'org.apache.grails:grails-datasource'

    // H2 for development
    runtimeOnly 'com.h2database:h2'

    // macOS file watcher (prevents noisy ClassNotFoundException on startup)
    developmentOnly 'io.methvin:directory-watcher:0.18.0'

    // JobRunr (Grails 8 ships Spring Boot 4, so use the SB4 starter;
    // on Grails 7.x replace with jobrunr-spring-boot-3-starter at the same version)
    implementation 'org.jobrunr:jobrunr-spring-boot-4-starter:8.5.1'
}
```

> **Pitfall: Two Hibernate artifacts.** `grails-data-hibernate5` registers the Grails Hibernate plugin. `grails-data-hibernate5-spring-boot` provides the Spring Boot auto-configuration. If you only include the `-spring-boot` one, GORM will not initialize and you'll get: _"Either class [X] is not a domain class or GORM has not been initialized correctly."_

> **Pitfall: JobRunr starter must match Spring Boot major version.** The `jobrunr-spring-boot-3-starter` is compiled against Spring Boot 3 / Spring Framework 6 APIs (declared `provided` scope). Putting it on a Grails 8 (Spring Boot 4 / Spring Framework 7) classpath will fail at bean wiring or class load time. Use the matching starter for your Spring Boot major version.

> **Pitfall: macOS file watcher.** Without `io.methvin:directory-watcher`, startup logs a long `NoClassDefFoundError` stacktrace for `MacOSXListeningWatchService`. It's harmless but noisy.

### Application Class

Your `Application.groovy` needs two annotations beyond the default:

```groovy
@Import([JobRunrStorageConfig, HibernateGormAutoConfiguration])
@ComponentScan('example.grails.jobrunr')
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}
```

- `@Import(JobRunrStorageConfig)` - loads the custom StorageProvider bean (see next section)
- `@Import(HibernateGormAutoConfiguration)` - Grails bypasses Spring Boot's `DataSourceAutoConfiguration`, which GORM's Hibernate auto-config depends on. Without this explicit import, GORM won't initialize.
- `@ComponentScan('example.grails.jobrunr')` - ensures Spring discovers `@Component` classes in `src/main/groovy/`. Grails does not component-scan that directory by default.

> **Do not put `@CompileStatic` on `Application.groovy`.** `GrailsAutoConfiguration`'s lifecycle hooks (`doWithSpring`, `doWithApplicationContext`, `doWithDynamicMethods`, ...) dispatch into Groovy closures. Static compilation breaks those hooks the moment you override one.

---

## 2. Bridging Grails and JobRunr

Grails configures its DataSource via `dataSource:` blocks in `application.yml` (not the `spring.datasource.*` keys Spring Boot's `DataSourceAutoConfiguration` looks for), and the bean is registered late, by `HibernateGormAutoConfiguration` calling `beanFactory.registerSingleton('dataSource', datastore.getDataSource())` from inside its `@Bean` method.

JobRunr's `JobRunrSqlStorageAutoConfiguration` is annotated `@AutoConfigureAfter(DataSourceAutoConfiguration.class)` and `@ConditionalOnBean(DataSource.class)`. Because Spring Boot's `DataSourceAutoConfiguration` finds no `spring.datasource.*` config and contributes nothing, JobRunr's storage auto-config evaluates its `@ConditionalOnBean` against a context where the GORM-managed DataSource has not been registered yet. The result varies by Spring Boot version and bean ordering: in this demo it is unreliable enough that we register the `StorageProvider` explicitly to remove the timing dependency.

There are three things this configuration class must handle:

```groovy
@Configuration
class JobRunrStorageConfig {

    @Bean
    StorageProvider storageProvider(DataSource dataSource) {
        StorageProvider provider = SqlStorageProviderFactory.using(
            dataSource, null, DatabaseOptions.CREATE)
        provider.setJobMapper(new JobMapper(new JacksonJsonMapper()))
        return provider
    }

    @Bean
    JobFilterRegistrar jobFilterRegistrar(
            BackgroundJobServer backgroundJobServer, List<JobFilter> jobFilters) {
        return new JobFilterRegistrar(backgroundJobServer, jobFilters)
    }

    static class JobFilterRegistrar {
        JobFilterRegistrar(BackgroundJobServer server, List<JobFilter> filters) {
            server.setJobFilters(filters)
        }
    }
}
```

### Why each piece matters

1. **`StorageProvider` bean** — JobRunr's auto-config can't create one without a Spring Boot-managed DataSource. We create it manually from the Grails-managed DataSource.

2. **`setJobMapper(...)` called eagerly** — The `RecurringJobPostProcessor` (which processes `@Recurring` annotations) runs during Spring bean initialization. If the `JobMapper` hasn't been set on the `StorageProvider` yet, you'll get a `NullPointerException` inside `RecurringJobTable`.

3. **`JobFilterRegistrar` bean** — JobRunr 8.x auto-configuration does NOT inject `JobFilter` beans into the `BackgroundJobServer`. Without this registrar, any custom `ApplyStateFilter` or `ElectStateFilter` you write will be silently ignored.

---

## 3. Configuration

### application.yml

```yaml
# JobRunr settings (in a separate YAML document)
---
jobrunr:
    background-job-server:
        enabled: true
        poll-interval-in-seconds: 5
        worker-count: 4
    dashboard:
        enabled: true
        port: 8000
    jobs:
        default-number-of-retries: 10
    database:
        skip-create: false
```

### H2 DataSource

```yaml
environments:
    development:
        dataSource:
            dbCreate: create-drop
            url: jdbc:h2:mem:devDb;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE
            driverClassName: org.h2.Driver
            username: sa
            password: ''
```

> **Pitfall: `DB_CLOSE_DELAY=-1` is required.** Without it, H2's in-memory database is destroyed when the last JDBC connection closes. JobRunr opens connections during startup to run migrations, then closes them. By the time your first job runs, the tables are gone: _"Table JOBRUNR_RECURRING_JOBS not found (this database is empty)."_

### Domain Class Naming

> **Pitfall: `Order` is a SQL reserved word.** If you have a domain class called `Order`, Hibernate will generate `DROP TABLE order` which fails. Add a table mapping:
>
> ```groovy
> class Order {
>     static mapping = {
>         table 'customer_order'
>     }
> }
> ```

---

## 4. Why Groovy Cannot Use JobRunr's Lambda API

JobRunr's primary Java API looks like this:

```java
jobScheduler.enqueue(svc -> svc.processOrder(orderId));
```

**This does not work in Groovy.** Even with Groovy 4's arrow syntax, closures compile to different bytecode than Java lambdas. JobRunr uses ASM to analyze lambda bytecode for serialization, and it rejects Groovy closures with:

```
IllegalArgumentException: Please provide a lambda expression
```

There is no workaround — Groovy closures fundamentally cannot be used with `JobScheduler.enqueue()`, `schedule()`, or `scheduleRecurrently()`.

### The solution

Use JobRunr's **JobRequest pattern** instead. It avoids lambdas entirely. (Kotlin users have a separate code path - JobRunr ships a `KotlinJobDetailsFinder` for Kotlin lambdas - so this section is specific to Groovy.)

Instead of `JobScheduler`, inject `JobRequestScheduler`:

```groovy
@Autowired
JobRequestScheduler jobRequestScheduler

// Fire-and-forget
jobRequestScheduler.enqueue(new OrderJobRequest(orderId))

// Scheduled
jobRequestScheduler.schedule(Instant.now().plusSeconds(120), new SendConfirmationRequest(orderId))

// Recurring
jobRequestScheduler.scheduleRecurrently("cleanup-id", "0 3 * * *", new CleanupJobRequest('audit-logs'))
```

---

## 5. The JobRequest Pattern

Every job needs two things: a **request** (what to do) and a **handler** (how to do it).

### The Request

A simple Groovy class that implements `JobRequest`. It must have a no-arg constructor for deserialization:

```groovy
class OrderJobRequest implements JobRequest {
    Long orderId

    OrderJobRequest() {}
    OrderJobRequest(Long orderId) { this.orderId = orderId }

    @Override
    Class<OrderProcessingService> getJobRequestHandler() {
        return OrderProcessingService
    }
}
```

### The Handler

Your Grails service can implement `JobRequestHandler` directly — no extra handler class needed:

```groovy
@Transactional
class OrderProcessingService implements JobRequestHandler<OrderJobRequest> {

    @Override
    @Job(name = "Process order", retries = 5, labels = ["order-processing"])
    void run(OrderJobRequest request) throws Exception {
        processOrder(request.orderId)
    }

    void processOrder(Long orderId) {
        Order order = Order.get(orderId)
        order.status = 'PROCESSING'
        order.save(flush: true)
        // ...
    }
}
```

For standalone handlers that aren't Grails services, annotate with `@Component` and `@Transactional`:

```groovy
@Component
class ImportProductsJobRequestHandler implements JobRequestHandler<ImportProductsJobRequest> {

    @Override
    @Job(name = "Import products", retries = 3)
    @Transactional
    void run(ImportProductsJobRequest request) throws Exception {
        // ...
    }
}
```

### @Job on `run()` vs business methods

> **Pitfall: Put `@Job` on the `run()` method, not on the delegated business method.** JobRunr records the job as a call to `run(Request)`. Any `@Job` annotation on `processOrder()` is never evaluated because JobRunr doesn't know about that internal delegation.
>
> Also avoid `%0` in `@Job(name = "...")` on `run()` methods — `%0` resolves to the request object's `toString()`, not to a specific field. Use a fixed name instead.

---

## 6. Fire-and-Forget Jobs

```groovy
jobRequestScheduler.enqueue(new OrderJobRequest(orderId))
```

The job executes immediately in JobRunr's background thread pool.

---

## 7. Scheduled Jobs

```groovy
Instant runAt = Instant.now().plusSeconds(120)
jobRequestScheduler.schedule(runAt, new SendConfirmationRequest(orderId))
```

The job appears in the dashboard's "Scheduled" tab and executes at the specified time.

---

## 8. Recurring Jobs

### Annotation-based

Use `@Recurring` on service methods. Grails services are lazy-initialized by default, so you must set `static lazyInit = false` or JobRunr won't discover the annotations at startup:

```groovy
class ReportGenerationService {
    static lazyInit = false

    @Recurring(id = "daily-sales-report", cron = "0 2 * * *")
    @Job(name = "Generate daily sales report")
    void generateDailySalesReport() { /* ... */ }

    @Recurring(id = "inventory-snapshot", interval = "PT6H")
    @Job(name = "Generate inventory snapshot")
    void generateInventorySnapshot() { /* ... */ }
}
```

> **Pitfall: `static lazyInit = false`.** Without this, the service bean isn't created until first use. Since nothing calls it during startup, the `@Recurring` annotations are never scanned and no recurring jobs are registered.

### Programmatic

Register recurring jobs in `BootStrap.groovy` using `JobRequestScheduler`:

```groovy
@Autowired
JobRequestScheduler jobRequestScheduler

def init = { servletContext ->
    jobRequestScheduler.scheduleRecurrently(
        "nightly-audit-cleanup", "0 3 * * *",
        new CleanupJobRequest('audit-logs')
    )
}
```

---

## 9. The @Job Annotation

```groovy
@Job(name = "Process order", retries = 5, labels = ["order-processing"])
void run(OrderJobRequest request) { /* ... */ }
```

- **`name`** — Display name in the dashboard.
- **`retries`** — Override the default retry count.
- **`labels`** — Tags for filtering in the dashboard.

---

## 10. Job Progress and Dashboard Logging

Access the `JobContext` via `ThreadLocalJobContext.getJobContext()` inside a `JobRequestHandler.run()` method:

```groovy
import org.jobrunr.server.runner.ThreadLocalJobContext

void run(ImportProductsJobRequest request) throws Exception {
    JobContext context = ThreadLocalJobContext.jobContext
    JobDashboardLogger jobLogger = context.logger()
    JobDashboardProgressBar progressBar = context.progressBar(request.batchSize)

    jobLogger.info("Starting import of ${request.batchSize} products")
    progressBar.incrementSucceeded()
}
```

> **Pitfall: `JobRequestHandler.jobContext()` is `@Deprecated` in 8.x.** The default method still works (it delegates to `ThreadLocalJobContext.getJobContext()`), but the official replacement is `ThreadLocalJobContext.getJobContext()` called directly. See [`JobRequestHandler.java`](https://github.com/jobrunr/jobrunr/blob/v8.5.1/core/src/main/java/org/jobrunr/jobs/lambdas/JobRequestHandler.java).

> **Pitfall: JobRunr 8.x API changes.** Three things changed from earlier versions:
>
> 1. **Get logger/progressBar from context** - Use `context.logger()` and `context.progressBar(n)`. Do NOT call `new JobDashboardLogger(context)` - the constructor now takes a `Job` object, not a `JobContext`, and Groovy's constructor resolution will fail with `MissingMethodException`.
>
> 2. **`incrementSucceeded()` not `increaseByOne()`** - The progress bar method was renamed in 8.x.
>
> 3. **`info(String)` only** - `JobDashboardLogger.info()` takes a single `String`. It does NOT support SLF4J-style `info("text {}", arg)` formatting. Use Groovy string interpolation: `jobLogger.info("Processing ${product.name}")`.

---

## 11. Automatic Retries

JobRunr automatically retries failed jobs with exponential backoff. Configure defaults:

```yaml
jobrunr:
    jobs:
        default-number-of-retries: 10
```

Override per-job with `@Job(retries = 3)`.

---

## 12. Job Filters

Implement `ApplyStateFilter` to hook into job state transitions:

```groovy
@Component
class AuditJobFilter implements ApplyStateFilter {

    @Override
    void onStateApplied(Job job, JobState oldState, JobState newState) {
        try {
            AuditLog.withNewTransaction {
                new AuditLog(
                    jobId: job.id?.toString(),
                    jobName: job.jobName,
                    oldState: oldState?.name?.toString() ?: 'NONE',
                    newState: newState.name.toString()
                ).save(flush: true, failOnError: true)
            }
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.message)
        }
    }
}
```

> **Pitfall: Use `withNewTransaction`, not `withNewSession`.** Filters run outside the Grails request lifecycle with no active transaction. `withNewSession` opens a Hibernate session but not a transaction, causing: _"no transaction is in progress"_. Always use `withNewTransaction` which provides both.

> **Pitfall: Filters must be registered manually in JobRunr 8.x.** The auto-configuration does not inject `JobFilter` beans into the `BackgroundJobServer`. See the `JobFilterRegistrar` in [Section 2](#2-bridging-grails-and-jobrunr).

> **Pitfall: GORM in filters trips JobRunr's 10 ms warning.** A `withNewTransaction` block that writes a domain object typically takes 10 to 60 ms on first invocation. JobRunr OSS does emit a warning every time the filter exceeds the budget - observed verbatim during boot of this demo on Grails 8: _"JobFilter of type 'example.grails.jobrunr.AuditJobFilter' has slow performance of 57ms (a Job Filter should run under 10ms) which negatively impacts the overall functioning of JobRunr. JobRunr Pro can run slow running Job Filters without a negative performance impact."_ Keep filter work minimal, push heavy work into a fire-and-forget child job, or accept the warning for low-throughput audit use cases.

---

## 13. Bulk Enqueueing

Enqueue one job per item:

```groovy
products.each { product ->
    jobRequestScheduler.enqueue(new SyncProductRequest(product.id))
}
```

---

## 14. GORM in Background Jobs

JobRunr runs jobs in its own thread pool, outside the Grails request lifecycle. GORM needs an active Hibernate session and transaction.

| Context | Solution |
|---------|----------|
| Grails service (in `grails-app/services/`) | `@Transactional` on the class — Grails provides session + transaction automatically |
| `@Component` handler (in `src/main/groovy/`) | `@Transactional` on the `run()` method |
| Job filter | `DomainClass.withNewTransaction { ... }` |

---

## 15. The Dashboard

Enabled at `http://localhost:8000/dashboard` with:

```yaml
jobrunr:
    dashboard:
        enabled: true
        port: 8000
```

The dashboard shows:
- **Jobs** by state (Enqueued, Processing, Succeeded, Failed, Scheduled)
- **Recurring Jobs** with their schedules and next run times
- **Servers** — connected background job server instances
- **Progress bars and logs** for running jobs (from `context.logger()` and `context.progressBar()`)

---

## 16. Production Considerations

### Storage Backend

This demo uses H2. For production, switch to a persistent database:

```groovy
// build.gradle
runtimeOnly 'org.postgresql:postgresql'
```

```yaml
# application.yml
environments:
    production:
        dataSource:
            dbCreate: none
            url: jdbc:postgresql://localhost:5432/myapp
            driverClassName: org.postgresql.Driver
            username: myuser
            password: mypass
```

JobRunr supports PostgreSQL, MySQL/MariaDB, Oracle, SQL Server, and MongoDB.

### Secure (or Disable) the Dashboard

The dashboard is **unauthenticated by default** and binds to all interfaces on port 8000. The single most common JobRunr production footgun is leaving it exposed. Either disable it:

```yaml
jobrunr:
    dashboard:
        enabled: false
```

Or enable HTTP Basic Auth:

```yaml
jobrunr:
    dashboard:
        enabled: true
        port: 8000
        username: ${JOBRUNR_DASHBOARD_USER}
        password: ${JOBRUNR_DASHBOARD_PASSWORD}
```

A reverse proxy (nginx, Caddy, an ingress controller) doing TLS termination + auth in front of port 8000 is the more robust pattern.

### CSRF Protection

The demo's GSP forms do not enable CSRF tokens because the demo has no Spring Security on the classpath. Production apps should add the [Spring Security Core plugin](https://plugins.grails.org/plugin/grails/spring-security-core) and use Spring Security's CSRF integration with `<g:form>`, or fall back to the legacy `<g:form useToken="true">` + `withForm` controller pattern.

### Static Compilation

Prefer `@GrailsCompileStatic` over plain `@CompileStatic` for Grails artefacts (controllers and services) - it understands Grails-specific dynamic methods (`params`, `flash`, `redirect`, `respond`, GORM dynamic finders) that plain static compilation rejects.

This demo applies `@GrailsCompileStatic` to controllers and to the simpler services. Two services that use heavy GORM DSLs (`createCriteria { projections { ... } }` in `ReportGenerationService`, `where { ... }.deleteAll()` in `DataCleanupService`) are left without it - those DSLs do not survive static compilation. Verify in your build before adding the annotation; do not assume it is safe.

Avoid static compilation on **domain classes**: GORM injects `save()`, `get()`, finders, `withTransaction`, and so on at runtime via AST transforms, and the trade-offs are not worth the friction.

---

## 17. Pitfalls Reference

A quick-reference of every Grails-specific pitfall covered in this guide:

| Pitfall | Symptom | Fix |
|---------|---------|-----|
| Groovy closures used with `JobScheduler` | `IllegalArgumentException: Please provide a lambda expression` | Use `JobRequestScheduler` with `JobRequest` objects |
| Missing `grails-data-hibernate5` dependency | `GORM has not been initialized correctly` | Add both `grails-data-hibernate5` AND `grails-data-hibernate5-spring-boot` |
| Missing `@Import(HibernateGormAutoConfiguration)` | `GORM has not been initialized correctly` | Add to `Application.groovy` |
| Missing `@ComponentScan` for `src/main/groovy` | `NoSuchBeanDefinitionException` for `@Component` classes | Add `@ComponentScan('your.package')` to `Application.groovy` |
| No `StorageProvider` bean | `No qualifying bean of type StorageProvider` | Create one in a `@Configuration` class with `@Import` |
| `JobMapper` not set on `StorageProvider` | `NullPointerException` in `RecurringJobTable` | Call `provider.setJobMapper(...)` when creating the `StorageProvider` bean |
| H2 without `DB_CLOSE_DELAY=-1` | `Table JOBRUNR_RECURRING_JOBS not found (this database is empty)` | Add `DB_CLOSE_DELAY=-1` to the JDBC URL |
| Domain class named `Order` | DDL error on `DROP TABLE order` | Add `static mapping = { table 'customer_order' }` |
| `@Recurring` not discovered | No recurring jobs in dashboard | Set `static lazyInit = false` on the service |
| `@Job(name = "...%0")` on `run()` method | Job name shows request object's `toString()` | Use a fixed name without `%0` on `run()` methods |
| `JobRequestHandler.jobContext()` | `@Deprecated` in 8.x | Use `ThreadLocalJobContext.getJobContext()` |
| `new JobDashboardLogger(context)` | `MissingMethodException` - constructor takes `Job` not `JobContext` | Use `context.logger()` instead |
| `progressBar.increaseByOne()` | `MissingMethodException` | Renamed to `progressBar.incrementSucceeded()` in 8.x |
| `jobLogger.info("text {}", arg)` | `MissingMethodException` - only `info(String)` exists | Use Groovy interpolation: `jobLogger.info("text ${arg}")` |
| `withNewSession` in job filters | `no transaction is in progress` | Use `withNewTransaction` instead |
| Job filters silently ignored | Filter bean exists but `onStateApplied` never called | Register filters on `BackgroundJobServer` via `setJobFilters()` |
| GORM writes inside a filter | OSS warns: "has slow performance of N ms (a Job Filter should run under 10ms)" | Keep filter work minimal, or offload heavy work to a child job (Pro has async filters) |
| `@CompileStatic` on a domain class | GORM dynamic methods (`save`, `get`, finders) fail to compile | Use `@GrailsCompileStatic` on services/controllers instead; never on domains |
| Missing `static allowedMethods` on a controller with state-changing actions | `GET /controller/save` mutates data | Declare `static allowedMethods = [save: 'POST', ...]` |
| `Order.list(max: 1).first()` on an empty table | `NoSuchElementException` from `List.first()` | Use `Order.first()` (GORM dynamic finder, returns null) |
| Dashboard exposed on port 8000 with no auth | Anyone on the network can trigger jobs | Set `jobrunr.dashboard.username` / `password`, or front it with a reverse proxy |
