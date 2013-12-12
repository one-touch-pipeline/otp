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
    <link rel="shortcut icon" href="${resource(dir: 'images', file: 'favicon.ico')}" type="image/x-icon">
    <r:require module="style"/>
    <r:require module="core"/>
    <g:javascript>
        $('.body').height($(window).height()-260);
        $(window).resize(function(){
            $('.body').height($(window).height()-260);
        });
        $.otp.highlight(window.location.pathname);
    </g:javascript>
    <r:layoutResources/>
    <g:layoutHead/>
</head>
<body>
    <div class="body_position">
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <div class="header">
                <img src="${resource(dir: 'images', file: 'header_operator.png')}" alt="Grails"/>
                <g:if test="${otp.environmentName() != 'production'}">
                    <p class="environmentName"><otp:environmentName/></p>
                </g:if>
            </div>
        </sec:ifAllGranted>
        <sec:ifNotGranted roles="ROLE_OPERATOR">
            <div class="header"><img src="${resource(dir: 'images', file: 'header.png')}" alt="Grails"/></div>
        </sec:ifNotGranted>
        <div class="headerGraphic">
            <img class="headerGraphicImg" src="${resource(dir: 'images', file: 'header_graphic.png')}" alt="Grails"/>
        </div>
        <div class="menu">
            <div class="menuContainer menuContainerL">
                <ul>
                    <sec:ifLoggedIn>
                        <li class="menuContainerLCss" id="individual"><g:link controller="individual" action="list"><g:message code="otp.menu.individuals"/></g:link></li>
                        <li class="menuContainerLCss" id="sequence"><g:link controller="sequence" action="index"><g:message code="otp.menu.sequences"/></g:link></li>
                        <li class="menuContainerLCss" id="run"><g:link controller="run" action="list"><g:message code="otp.menu.runs"/></g:link></li>
                        <li class="menuContainerLCss" id="processes"><g:link controller="processes" action="list"><g:message code="otp.menu.processes"/></g:link></li>
                        <li class="overview_nav menuContainerLCss">
                            <ul>
                                <li class="overview_nav_container" id="overview"><g:link ><g:message code="otp.menu.overview"/> &#9661;</g:link>
                                    <ul>
                                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                                            <li class="allGranted_general" id="overviewMB"><g:link controller="overviewMB" action="index"><g:message code="otp.menu.generalStatistics"/></g:link></li><br>
                                        </sec:ifAllGranted>
                                        <li id="projectOverview"><g:link controller="projectOverview" action="index"><g:message code="otp.menu.projectSpecificStatistics"/></g:link></li><br>
                                        <li id="laneOverview"><g:link controller="projectOverview" action="laneOverview"><g:message code="otp.menu.projectSpecificStatisticsTableOverview"/></g:link></li><br>
                                        <li id="projectStatistic"><g:link controller="projectStatistic" action="index"><g:message code="otp.menu.projectStatistics"/></g:link></li><br>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <li class="allGranted menuContainerLCss" id="projectProgress"><g:link controller="projectProgress" action="progress"><g:message code="otp.menu.progress"/></g:link></li>
                            <li class="allGranted menuContainerLCss" id="runSubmit"><g:link controller="runSubmit" action="index"><g:message code="otp.menu.runSubmit"/></g:link></li>
                        </sec:ifAllGranted>
                        <sec:ifAnyGranted roles="ROLE_ADMIN">
                            <li class="admin_nav menuContainerLCss">
                                <ul>
                                    <li class="allGranted admin_nav_container" id="admin"><g:link><g:message code="otp.menu.adminSection"/> &#9661;</g:link>
                                        <ul>
                                            <li id="userAdministration"><g:link controller="userAdministration"><g:message code="otp.menu.userAdministration"/></g:link></li><br>
                                            <li id="group"><g:link controller="group"><g:message code="otp.menu.groupAdministration"/></g:link></li><br>
                                            <li id="crashRecovery"><g:link controller="crashRecovery"><g:message code="otp.menu.crashRecovery"/></g:link></li><br>
                                            <li id="shutdown"><g:link controller="shutdown"><g:message code="otp.menu.planServerShutdown"/></g:link></li><br>
                                            <li id="notification"><g:link controller="notification"><g:message code="otp.menu.manageNotifications"/></g:link></li><br>
                                            <li id="processingOption"><g:link controller="processingOption"><g:message code="otp.menu.processingOptions"/></g:link></li><br>
                                            <li id="softwareTool"><g:link controller="softwareTool" action="list"><g:message code="otp.menu.softwareTool"/></g:link></li><br>
                                        </ul>
                                     </li>
                                </ul>
                            </li>
                        </sec:ifAnyGranted>
                        <sec:ifAnyGranted roles="ROLE_SWITCH_USER">
                            <li class="switchUserRight menuContainerLCss" id="switchUser"><g:link controller="switchUser"><g:message code="otp.menu.switchUser"/></g:link></li>
                        </sec:ifAnyGranted>
                        <sec:ifSwitched>
                            <li><a href='${request.contextPath}/j_spring_security_exit_user'>Resume as <sec:switchedUserOriginalUsername/></a></li>
                        </sec:ifSwitched>
                    </sec:ifLoggedIn>
                </ul>
            </div>
            <div class="menuContainer_empty"></div>
            <div class="menuContainer menuContainerR">
                <ul>
                    <sec:ifLoggedIn>
                        <li id="logout"><g:link controller="logout" action="index"><g:message code="otp.menu.logout"/></g:link></li>
                        <li id="home"><g:link url="${request.contextPath}/"><g:message code="otp.menu.home"/></g:link></li>
                    </sec:ifLoggedIn>
                </ul>
            </div>
        </div>
        <div id="infoBox"></div>
        <g:layoutBody/>
        <g:javascript library="application"/>
        <div class="footer" role="contentinfo"><a href="http://ibios.dkfz.de/tbi/index.php/data-management/software/otp" target="_blank">OneTouchPipeline</a>  | <a href="http://ibios.dkfz.de/tbi/index.php" target="_blank">Eils Labs</a> | TBI DKFZ | Build: <g:render template="/templates/version"/></div>
        <div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
    </div>
    <r:layoutResources/>
</body>
</html>
