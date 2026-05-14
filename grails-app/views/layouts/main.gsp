<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:layoutTitle default="Grails + JobRunr Demo"/></title>
    <asset:stylesheet src="application.css"/>
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
