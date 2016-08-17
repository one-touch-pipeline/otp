<!doctype html>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js ${otp.environmentName() }"><!--<![endif]-->
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title><g:layoutTitle default="Grails"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="contextPath" content="${request.contextPath}">
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:stylesheet src="modules/defaultPageDependencies.css"/>
    <asset:stylesheet src="processingTimeStatistics.less"/>
    <asset:javascript src="pages/processingTimeStatistics/index/datatable.js"/>
    <g:layoutHead/>
</head>
<body id="otp">
<g:layoutBody/>
<asset:deferredScripts/>
</body>
</html>