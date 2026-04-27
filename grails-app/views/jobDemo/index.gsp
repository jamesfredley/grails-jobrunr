<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Grails + JobRunr Demo</title>
    <style>
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .card { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .card h2 { font-size: 1.1em; margin-bottom: 8px; color: #2c3e50; }
        .card p { font-size: 0.9em; color: #666; margin-bottom: 15px; }
        .card .feature-tag { display: inline-block; background: #eee; color: #555; font-size: 0.75em; padding: 2px 8px; border-radius: 3px; margin-bottom: 10px; }
        .btn { display: inline-block; padding: 10px 20px; border: none; border-radius: 5px; color: white; cursor: pointer; font-size: 0.9em; text-decoration: none; }
        .btn-blue { background: #3498db; }
        .btn-blue:hover { background: #2980b9; }
        .btn-green { background: #27ae60; }
        .btn-green:hover { background: #219a52; }
        .btn-orange { background: #e67e22; }
        .btn-orange:hover { background: #d35400; }
        .btn-purple { background: #8e44ad; }
        .btn-purple:hover { background: #7d3c98; }
        .btn-red { background: #e74c3c; }
        .btn-red:hover { background: #c0392b; }
        .btn-teal { background: #16a085; }
        .btn-teal:hover { background: #1abc9c; }
        table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 0.85em; }
        th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #eee; }
        th { background: #f8f9fa; font-weight: 600; }
        .status { padding: 2px 8px; border-radius: 3px; font-size: 0.8em; font-weight: 600; }
        .status-PENDING { background: #fff3cd; color: #856404; }
        .status-PROCESSING { background: #cce5ff; color: #004085; }
        .status-SHIPPED { background: #d4edda; color: #155724; }
        .status-DELIVERED { background: #d1ecf1; color: #0c5460; }
        .status-CANCELLED { background: #f8d7da; color: #721c24; }
        h2.section-title { margin: 30px 0 15px; color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 8px; }
    </style>
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
                        <td style="${product.stockQuantity < 10 ? 'color: red; font-weight: bold;' : ''}">${product.stockQuantity}</td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>

    <h2 class="section-title">Recent Audit Logs (Job Filter)</h2>
    <div class="card">
        <p style="color: #666; font-size: 0.85em; margin-bottom: 10px;">
            These entries are created by <code>AuditJobFilter</code> (a custom <code>ApplyStateFilter</code>) whenever a job changes state.
        </p>
        <table>
            <thead>
                <tr><th>Job ID</th><th>Job Name</th><th>Old State</th><th>New State</th><th>Time</th></tr>
            </thead>
            <tbody>
                <g:each in="${auditLogs}" var="entry">
                    <tr>
                        <td style="font-family: monospace; font-size: 0.8em;">${entry.jobId?.take(12)}...</td>
                        <td>${entry.jobName}</td>
                        <td>${entry.oldState}</td>
                        <td>${entry.newState}</td>
                        <td><g:formatDate date="${entry.dateCreated}" format="HH:mm:ss"/></td>
                    </tr>
                </g:each>
                <g:if test="${!auditLogs}">
                    <tr><td colspan="5" style="text-align:center; color:#999;">No audit logs yet. Trigger a job to see filter activity.</td></tr>
                </g:if>
            </tbody>
        </table>
    </div>

    <div style="margin-top: 30px; padding: 20px; background: #fff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
        <h2 style="font-size: 1em; margin-bottom: 10px;">Recurring Jobs (annotation-based)</h2>
        <p style="font-size: 0.85em; color: #666;">
            The following recurring jobs are registered via <code>@Recurring</code> annotations on <code>ReportGenerationService</code>:
        </p>
        <ul style="font-size: 0.85em; color: #555; margin: 10px 0 10px 20px;">
            <li><strong>daily-sales-report</strong> &mdash; CRON: <code>0 2 * * *</code> (daily at 2 AM)</li>
            <li><strong>inventory-snapshot</strong> &mdash; Interval: <code>PT6H</code> (every 6 hours)</li>
        </ul>
        <p style="font-size: 0.85em; color: #666;">
            Plus two programmatic recurring jobs from <code>BootStrap.groovy</code>:
        </p>
        <ul style="font-size: 0.85em; color: #555; margin: 10px 0 10px 20px;">
            <li><strong>nightly-audit-cleanup</strong> &mdash; CRON: <code>0 3 * * *</code> (daily at 3 AM)</li>
            <li><strong>weekly-order-cleanup</strong> &mdash; CRON: <code>0 4 * * SUN</code> (Sundays at 4 AM)</li>
        </ul>
        <p style="font-size: 0.85em; color: #666; margin-top: 10px;">
            View all recurring jobs in the <a href="http://localhost:8000" target="_blank">JobRunr Dashboard</a> under the "Recurring Jobs" tab.
        </p>
    </div>
</body>
</html>
