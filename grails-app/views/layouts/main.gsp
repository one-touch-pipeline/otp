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
    <link rel="shortcut icon" href="${assetPath(src: 'favicon.ico')}" type="image/x-icon">
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:stylesheet src="modules/defaultPageDependencies.css"/>
    <asset:stylesheet src="modules/style.css"/>
    <asset:script type="text/javascript">
        $('.body').height($(window).height()-260);
        $(window).resize(function(){
            $('.body').height($(window).height()-260);
        });
        $.otp.highlight(window.location.pathname);
    </asset:script>
    <g:layoutHead/>
</head>
<body id="otp">
    <div class="body_position">
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <div class="header">
                <img src="${assetPath(src: 'header_operator.png')}" alt="OTP"/>
                <g:if test="${otp.environmentName() != 'production'}">
                    <p class="environmentName"><otp:environmentName/></p>
                </g:if>
            </div>
        </sec:ifAllGranted>
        <sec:ifNotGranted roles="ROLE_OPERATOR">
            <div class="header">
                <g:if test="${controllerName == "info"}"><h1><g:message code="otp.title" /></h1></g:if>
                <img src="${assetPath(src: 'header.png')}" alt="OTP"/>
            </div>
        </sec:ifNotGranted>
        <div class="headerGraphic">
            <img class="headerGraphicImg" src="${assetPath(src: 'header_graphic.png')}" alt=""/>
        </div>
        <div class="menu">
            <div class="menuContainer menuContainerL">
                <ul>
                    <sec:ifLoggedIn>
                        <li class="menuContainerLCss"><g:link controller="projectOverview" action="laneOverview"><g:message code="otp.menu.overview"/></g:link></li>
                        <li class="menuContainerLCss"><g:link controller="individual" action="list"><g:message code="otp.menu.individuals"/></g:link></li>
                        <li class="menuContainerLCss"><g:link controller="sequence" action="index"><g:message code="otp.menu.sequences"/></g:link></li>
                        <li class="menuContainerLCss"><g:link controller="run" action="list"><g:message code="otp.menu.runs"/></g:link></li>
                        <li class="navigation menuContainerLCss project">
                            <ul>
                                <li class="overview_nav_container nav_container"><g:link class="menuLinkContainer"><g:message code="otp.menu.project"/> &#9661;</g:link>
                                    <ul>
                                        <li><g:link controller="projectOverview" action="index"><g:message code="otp.menu.projectSpecificStatistics"/></g:link></li><br>
                                        <li><g:link controller="projectConfig" action="index"><g:message code="otp.menu.projectConfig"/></g:link></li>
                                        <sec:ifAllGranted roles="ROLE_MMML_MAPPING">
                                            <li class="allGranted"><g:link controller="projectOverview" action="mmmlIdentifierMapping"><g:message code="otp.menu.mmmlIdentifierMapping"/></g:link></li><br>
                                        </sec:ifAllGranted>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <li class="navigation menuContainerLCss results">
                            <ul>
                                <li class="analysis_results_nav_container nav_container"><g:link class="menuLinkContainer"><g:message code="otp.menu.results"/> &#9661;</g:link>
                                    <ul>
                                        <li><g:link controller="alignmentQualityOverview" action="index"><g:message code="otp.menu.alignmentQuality"/></g:link></li><br>
                                        <li><g:link controller="snv" action="results"><g:message code="otp.menu.snv.results"/></g:link></li>
                                        <li><g:link controller="indel" action="results"><g:message code="otp.menu.indel.results"/></g:link></li>
                                        <li><g:link controller="aceseq" action="results"><g:message code="otp.menu.cnv.results"/></g:link></li>
                                        <li><g:link controller="sophia" action="results"><g:message code="otp.menu.sophia.results"/></g:link></li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <sec:ifAnyGranted roles="ROLE_OPERATOR">
                            <li class="navigation menuContainerLCss statistic">
                                <ul>
                                    <li class="allGranted overview_nav_container nav_container"><g:link class="menuLinkContainer"><g:message code="otp.menu.statistics"/> &#9661;</g:link>
                                        <ul>
                                            <li><g:link controller="overviewMB" action="index"><g:message code="otp.menu.generalStatistics"/></g:link></li><br>
                                            <li><g:link controller="clusterJobGeneral" action="index"><g:message code="otp.menu.jobstats.general"/></g:link></li><br>
                                            <li><g:link controller="clusterJobJobTypeSpecific" action="index"><g:message code="otp.menu.jobstats.jobTypeSpecific"/></g:link></li><br>
                                            <li><g:link controller="processingTimeStatistics" action="index"><g:message code="otp.menu.processingTimeStatistics"/></g:link></li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                            <li class="navigation menuContainerLCss operator">
                                <ul>
                                    <li class="allGranted operator_nav_container nav_container"><g:link class="menuLinkContainer"><g:message code="otp.menu.operatorSection"/> &#9661;</g:link>
                                        <ul>
                                            <li><g:link controller="metadataImport" action="index"><g:message code="otp.menu.importAndValidation"/></g:link></li>
                                            <li><g:link controller="bamMetadataImport" action="index"><g:message code="otp.menu.bamMetadataImport"/></g:link></li>
                                            <li><g:link controller="sampleIdentifierOverview" action="index"><g:message code="otp.menu.sampleIdentifierOverview"/></g:link></li>
                                            <li><g:link controller="metadataImport" action="blacklistedIlseNumbers"><g:message code="otp.menu.blacklistedIlseNumbers"/></g:link></li>
                                            <li><g:link controller="createProject" action="index"><g:message code="otp.menu.createProject"/></g:link></li>
                                            <li><g:link controller="individual" action="insert"><g:message code="otp.menu.createIndividual"/></g:link></li>
                                            <li><g:link controller="individual" action="insertMany"><g:message code="otp.menu.createSample"/></g:link></li>
                                            <li><g:link controller="processes" action="list"><g:message code="otp.menu.processes"/></g:link></li>
                                            <li><g:link controller="mergingCriteria" action="defaultSeqPlatformGroupConfiguration"><g:message code="otp.menu.seqPlatformGroup"/></g:link></li>
                                            <li><g:link controller="metaDataFields" action="index"><g:message code="otp.menu.metaDataFields"/></g:link></li>
                                            <li><g:link controller="softwareTool" action="list"><g:message code="otp.menu.softwareTool"/></g:link></li>
                                            <li><g:link controller="projectProgress" action="progress"><g:message code="otp.menu.progress"/></g:link></li>
                                            <li><g:link controller="sampleSwap" action="index"><g:message code="otp.menu.sampleSwap"/></g:link></li>C
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </sec:ifAnyGranted>
                        <sec:ifAnyGranted roles="ROLE_ADMIN">
                            <li class="navigation menuContainerLCss admin">
                                <ul>
                                    <li class="allGranted admin_nav_container nav_container"><g:link class="menuLinkContainer"><g:message code="otp.menu.adminSection"/> &#9661;</g:link>
                                        <ul>
                                            <li><g:link controller="userAdministration"><g:message code="otp.menu.userAdministration"/></g:link></li>
                                            <li><g:link controller="group"><g:message code="otp.menu.groupAdministration"/></g:link></li>
                                            <li><g:link controller="crashRecovery"><g:message code="otp.menu.crashRecovery"/></g:link></li>
                                            <li><g:link controller="processingOption"><g:message code="otp.menu.processingOptions"/></g:link></li>
                                            <li><g:link controller="jobErrorDefinition"><g:message code="otp.menu.jobErrorDefinition"/></g:link></li>
                                            <li><g:link controller="shutdown"><g:message code="otp.menu.planServerShutdown"/></g:link></li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
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
                        <li><g:link controller="logout" action="index"><g:message code="otp.menu.logout"/></g:link></li>
                        <li><g:link controller="home" action="index"><g:message code="otp.menu.home"/></g:link></li>
                    </sec:ifLoggedIn>
                </ul>
            </div>
        </div>
        <div id="infoBox"></div>
        <g:layoutBody/>
        <div class="footer" role="contentinfo">
            &copy;2011-2017 <a href="https://www.dkfz.de" target="_blank">DKFZ</a> -
                <a href="http://ibios.dkfz.de/tbi/" target="_blank">eilslabs</a> |
            <a href="http://ibios.dkfz.de/tbi/index.php/data-management/software/otp" target="_blank">About OTP</a> |
            <g:render template="/templates/version"/>
        </div>
        <div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
    </div>
    <asset:deferredScripts/>
    <g:render template="/layouts/piwik"/>
</body>
</html>
