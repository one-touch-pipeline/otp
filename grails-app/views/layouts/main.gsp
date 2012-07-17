<!doctype html>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js"><!--<![endif]-->
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<title><g:layoutTitle default="Grails"/></title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<link rel="shortcut icon" href="${resource(dir: 'images', file: 'favicon.ico')}" type="image/x-icon">
        <r:require module="style"/>
        <r:require module="core"/>
        <r:layoutResources/>
        <g:layoutHead/>
        <r:script>
        $.otp.contextPath = '${request.contextPath}';
        </r:script>
	</head>
	<body>
<%--		<div id="grailsLogo" role="banner"><a href="http://grails.org"><img src="${resource(dir: 'images', file: 'grails_logo.png')}" alt="Grails"/></a></div>--%>

		<div class="nav" role="navigation">
			<ul>
				<li><g:link url="${request.contextPath}">Home</g:link></li>
    			<li><g:link controller="overviewMB" action="index">Overview</g:link></li>
				<li><g:link controller="individual" action="list">Individuals</g:link></li>
				<li><g:link controller="run" action="list">Runs</g:link></li>
				<li><g:link controller="projectProgress" action="progress">Progress</g:link></li>
				<li><g:link controller="processes" action="index">Processes</g:link></li>
				<li><g:link controller="login" action="index">| Login |</g:link></li>
				<li><g:link controller="runSubmit" action="index">Run Submit</g:link></li>
			</ul>
		</div>
		
        <div id="infoBox"></div>
		<g:layoutBody/>
		<div class="footer" role="contentinfo">OneTouchPipeline  | Eils Labs | TBI DKFZ</div>
		<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
		<g:javascript library="application"/>
        <r:layoutResources/>
	</body>
</html>
