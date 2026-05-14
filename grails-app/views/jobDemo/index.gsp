<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Grails + JobRunr Demo</title>
</head>
<body>
    <h2 class="section-title">Trigger Background Jobs</h2>
    <div class="grid">

        <div class="card">
            <span class="feature-tag">Fire-and-Forget</span>
            <h2>Process Order</h2>
            <p>Enqueues an order processing job that runs immediately in the background. Uses <code>@Job</code> annotation for naming and retry configuration.</p>
            <g:form action="fireAndForget" method="POST">
                <button type="submit" class="btn btn-blue">Enqueue Order Processing</button>
            </g:form>
        </div>

        <div class="card">
            <span class="feature-tag">Scheduled / Delayed</span>
            <h2>Schedule Email (2 min delay)</h2>
            <p>Schedules an email confirmation job to run 2 minutes from now. Uses <code>JobContext</code> for progress bar and dashboard logging.</p>
            <g:form action="scheduleDelayed" method="POST">
                <button type="submit" class="btn btn-green">Schedule for 2 min</button>
            </g:form>
        </div>

        <div class="card">
            <span class="feature-tag">Automatic Retries</span>
            <h2>Process Order (Retry Demo)</h2>
            <p>This job randomly fails to demonstrate JobRunr's automatic retry with exponential backoff. Watch the retry count in the dashboard!</p>
            <g:form action="fireAndForgetWithRetry" method="POST">
                <button type="submit" class="btn btn-red">Enqueue (May Fail)</button>
            </g:form>
        </div>

        <div class="card">
            <span class="feature-tag">Standalone JobRequestHandler</span>
            <h2>Import Products</h2>
            <p>Uses a standalone <code>JobRequestHandler</code> with progress bar and dashboard logging. Imports 5 simulated products.</p>
            <g:form action="importProducts" method="POST">
                <button type="submit" class="btn btn-purple">Import Products</button>
            </g:form>
        </div>

        <div class="card">
            <span class="feature-tag">Bulk Enqueueing</span>
            <h2>Sync All Inventory</h2>
            <p>Enqueues one sync job per product. Watch multiple jobs appear in the dashboard at once!</p>
            <g:form action="bulkSync" method="POST">
                <button type="submit" class="btn btn-orange">Bulk Sync Inventory</button>
            </g:form>
        </div>

        <div class="card">
            <span class="feature-tag">Programmatic Recurring</span>
            <h2>Trigger Cleanup</h2>
            <p>Manually triggers the cleanup job that also runs as a programmatic recurring job registered in <code>BootStrap.groovy</code>.</p>
            <g:form action="triggerCleanup" method="POST">
                <button type="submit" class="btn btn-teal">Run Cleanup Now</button>
            </g:form>
        </div>

    </div>

    <h2 class="section-title">Current Orders</h2>
    <div class="card">
        <table>
            <thead>
                <tr><th>Order #</th><th>Customer</th><th>Amount</th><th>Status</th><th>Created</th></tr>
            </thead>
            <tbody>
                <g:each in="${orders}" var="order">
                    <tr>
                        <td>${order.orderNumber}</td>
                        <td>${order.customerEmail}</td>
                        <td>\$${order.totalAmount}</td>
                        <td><span class="status status-${order.status}">${order.status}</span></td>
                        <td><g:formatDate date="${order.dateCreated}" format="yyyy-MM-dd HH:mm"/></td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>

    <h2 class="section-title">Products</h2>
    <div class="card">
        <table>
            <thead>
                <tr><th>Name</th><th>SKU</th><th>Price</th><th>Stock</th></tr>
            </thead>
            <tbody>
                <g:each in="${products}" var="product">
                    <tr>
                        <td>${product.name}</td>
                        <td>${product.sku}</td>
                        <td>\$${product.price}</td>
                        <td class="${product.stockQuantity < 10 ? 'low-stock' : ''}">${product.stockQuantity}</td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>

    <h2 class="section-title">Recent Audit Logs (Job Filter)</h2>
    <div class="card">
        <p class="audit-explainer">
            These entries are created by <code>AuditJobFilter</code> (a custom <code>ApplyStateFilter</code>) whenever a job changes state.
        </p>
        <table>
            <thead>
                <tr><th>Job ID</th><th>Job Name</th><th>Old State</th><th>New State</th><th>Time</th></tr>
            </thead>
            <tbody>
                <g:each in="${auditLogs}" var="entry">
                    <tr>
                        <td class="job-id">${entry.jobId?.take(12)}...</td>
                        <td>${entry.jobName}</td>
                        <td>${entry.oldState}</td>
                        <td>${entry.newState}</td>
                        <td><g:formatDate date="${entry.dateCreated}" format="HH:mm:ss"/></td>
                    </tr>
                </g:each>
                <g:if test="${!auditLogs}">
                    <tr><td colspan="5" class="empty-state">No audit logs yet. Trigger a job to see filter activity.</td></tr>
                </g:if>
            </tbody>
        </table>
    </div>

    <div class="recurring-jobs-panel">
        <h2>Recurring Jobs (annotation-based)</h2>
        <p>
            The following recurring jobs are registered via <code>@Recurring</code> annotations on <code>ReportGenerationService</code>:
        </p>
        <ul>
            <li><strong>daily-sales-report</strong> &mdash; CRON: <code>0 2 * * *</code> (daily at 2 AM)</li>
            <li><strong>inventory-snapshot</strong> &mdash; Interval: <code>PT6H</code> (every 6 hours)</li>
        </ul>
        <p>
            Plus two programmatic recurring jobs from <code>BootStrap.groovy</code>:
        </p>
        <ul>
            <li><strong>nightly-audit-cleanup</strong> &mdash; CRON: <code>0 3 * * *</code> (daily at 3 AM)</li>
            <li><strong>weekly-order-cleanup</strong> &mdash; CRON: <code>0 4 * * SUN</code> (Sundays at 4 AM)</li>
        </ul>
        <p>
            View all recurring jobs in the <a href="http://localhost:8000" target="_blank">JobRunr Dashboard</a> under the "Recurring Jobs" tab.
        </p>
    </div>
</body>
</html>
