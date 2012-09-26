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
                <sec:ifNotLoggedIn>
                    <li><g:link controller="login" action="auth">Login</g:link></li>
                </sec:ifNotLoggedIn>
                <sec:ifLoggedIn>
                    <li><g:link url="${request.contextPath}/">Home</g:link></li>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <li><g:link controller="overviewMB" action="index">Overview</g:link></li>
                    </sec:ifAllGranted>
                    <li><g:link controller="individual" action="list">Individuals</g:link></li>
                    <li><g:link controller="sequence" action="index">Sequences</g:link></li>
                    <li><g:link controller="run" action="list">Runs</g:link></li>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <li><g:link controller="projectProgress" action="progress">Progress</g:link></li>
                    </sec:ifAllGranted>
                    <li><g:link controller="processes" action="list">Processes</g:link></li>
                    <sec:ifSwitched>
                        <li><a href='${request.contextPath}/j_spring_security_exit_user'>Resume as <sec:switchedUserOriginalUsername/></a></li>
                    </sec:ifSwitched>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <li><g:link controller="runSubmit" action="index">Run Submit</g:link></li>
                    </sec:ifAllGranted>
                    <li><g:link controller="logout" action="index">| Logout |</g:link></li>
                </sec:ifLoggedIn>
			</ul>
            <g:if test="${otp.environmentName() != 'production'}">
                <p class="environmentName"><otp:environmentName/></p>
            </g:if>
		</div>
		
        <div id="infoBox"></div>
		<g:layoutBody/>
		<div class="footer" role="contentinfo">OneTouchPipeline  | Eils Labs | TBI DKFZ | Build: <g:render template="/templates/version"/></div>
		<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
		<g:javascript library="application"/>
        <r:layoutResources/>
	</body>
</html>
