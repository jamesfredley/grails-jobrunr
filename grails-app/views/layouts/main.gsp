<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:layoutTitle default="Grails + JobRunr Demo"/></title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; color: #333; line-height: 1.6; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        header { background: #2c3e50; color: white; padding: 20px 0; margin-bottom: 30px; }
        header .container { display: flex; justify-content: space-between; align-items: center; }
        header h1 { font-size: 1.5em; }
        header a { color: #3498db; text-decoration: none; }
        header a:hover { color: #5dade2; }
        .flash-message { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 15px; border-radius: 6px; margin-bottom: 20px; }
    </style>
    <g:layoutHead/>
</head>
<body>
    <header>
        <div class="container">
            <h1>Grails + JobRunr Demo</h1>
            <a href="http://localhost:8000" target="_blank">Open JobRunr Dashboard (port 8000)</a>
        </div>
    </header>
    <div class="container">
        <g:if test="${flash.message}">
            <div class="flash-message">${flash.message}</div>
        </g:if>
        <g:layoutBody/>
    </div>
</body>
</html>
