%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page import="de.dkfz.tbi.otp.ProjectSelectionService" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="shortcut icon" href="${assetPath(src: 'v2/otp-favicon.ico')}" type="image/x-icon">
    <title><g:layoutTitle default="OTP"/></title>
    <meta name="contextPath" content="${request.contextPath}">
    <meta name="projectName" content="${selectedProject?.name}">
    <meta name="projectParameter" content="${projectParameter}">
    <meta name="controllerName" content="${controllerName}">
    <meta name="actionName" content="${actionName}">
    <asset:javascript src="v2/application.js"/>
    <asset:stylesheet src="v2/application.css"/>
    <asset:javascript src="v2/pages/${controllerName}/script.js"/>
    <asset:stylesheet src="v2/pages/${controllerName}/style.css"/>
    <asset:javascript src="v2/pageSpecific.js"/>
    <g:layoutHead/>
</head>
<body>
<div class="container">
    <sec:ifLoggedIn>
        <div class="col-xs-12 text-right">
            <sec:ifNotSwitched>
                ${g.message(code: "header.loggedIn", args: [sec.loggedInUserInfo(field: "displayName"), sec.username()])} •
                <g:link controller='logout'>${g.message(code: "header.logout")}</g:link>
            </sec:ifNotSwitched>
            <sec:ifSwitched>
                ${g.message(code: "header.switched", args: [sec.username()])} •
                <g:link controller='logout' action='impersonate'>${g.message(code: "header.switchBack", args: [sec.switchedUserOriginalUsername()])}</g:link>
            </sec:ifSwitched>
        </div>
    </sec:ifLoggedIn>
</div>

<div class="container">
    <div class="row">
        <div class="col-xs-10 col-xs-offset-1 col-sm-12 col-sm-offset-0">
            <div class="alert alert-success js-alert-success hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
            <div class="alert alert-info js-alert-info hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
            <div class="alert alert-warning js-alert-warning hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
            <div class="alert alert-danger js-alert-danger hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
        </div>
    </div>
</div>

<sec:ifLoggedIn>
    <nav class="navbar affix-top" data-spy="affix" data-offset-top="86">
        <div class="container">
            <div class="row">
                <div class="col-xs-12 col-sm-2 col-md-3 logo-col">
                    <div class="navbar-header">
                        <button type="button" class="navbar-toggle collapsed navbar-right" data-toggle="collapse" data-target=".navbar-collapse" aria-expanded="false">
                            <span class="sr-only">Toggle navigation</span>
                            <span class="icon-bar"></span>
                            <span class="icon-bar"></span>
                            <span class="icon-bar"></span>
                        </button>
                        <div class="navbar-logo">
                            <div class="logo-img">
                                <g:link class="logo" uri="/">
                                    <img class="img-responsive" src="${assetPath(src: 'v2/otp-logo.png')}" alt="OTP">
                                </g:link>
                            </div>
                            <div class="logo-slogan hidden-sm ${otp.environmentName() != 'production' ? "environment" : ""}">
                                <h1>
                                    One<br>Touch<br>Pipeline
                                    <g:if test="${otp.environmentName() != 'production'}">
                                        <br><span class="environment-name">${otp.environmentName()}</span>
                                    </g:if>
                                </h1>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-10 col-md-9">
                    <div class="row">
                        <div class="col-xs-12">
                            <div class="collapse navbar-collapse search-navbar">
                                <form class="navbar-form navbar-left">
                                    <div class="input-group">
                                        <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                            ${selectedProject.getDisplayName()} <span class="caret"></span>
                                        </button>
                                        <ul id="js-project-list" class="dropdown-menu dropdown-menu-right scrollable-menu">
                                            <div class="project-search-field">
                                                <input type="text" class="form-control" id="js-project-input" autocomplete="off" role="textbox" aria-label="${g.message(code:"header.projectSelection.search")}" title="${g.message(code:"header.projectSelection.search")}" placeholder="${g.message(code:"header.projectSelection.search")}">
                                            </div>
                                            <li><g:link controller="${controllerName}" action="${actionName}" params="${[projectGroup: "ALL"]}">${g.message(code: "header.projectSelection.allProjects")}</g:link>
                                            <g:each in="${availableProjectsInGroups.entrySet()}" var="subGroup">
                                                <li class="dropdown-submenu"><a href="#">${subGroup.key}</a>
                                                    <ul class="dropdown-menu">
                                                        <g:each in="${subGroup.value}" var="item" status="i">
                                                            <g:if test="${i==0}">
                                                                <li><g:link controller="${controllerName}" action="${actionName}" params="${[(projectParameter): selectedProject.name, projectGroup: subGroup.key]}">${item.displayName}</g:link></li>
                                                            </g:if>
                                                            <g:else>
                                                                <li><g:link controller="${controllerName}" action="${actionName}" params="${[(projectParameter): item.displayName, projectGroup: subGroup.key]}">${item.displayName}</g:link></li>
                                                            </g:else>
                                                        </g:each>
                                                    </ul>
                                                </li>
                                            </g:each>
                                            <g:if test="${!availableProjectsWithoutGroup.isEmpty()}">
                                                <li class="dropdown-submenu"><a href="#">${g.message(code: "header.projectSelection.other")}</a>
                                                    <ul class="dropdown-menu">
                                                        <g:each in="${availableProjectsWithoutGroup}" var="item">
                                                            <li><g:link controller="${controllerName}" action="${actionName}" params="${[(projectParameter): item.displayName]}">${item.displayName}</g:link></li>
                                                        </g:each>
                                                    </ul>
                                                </li>
                                            </g:if>
                                        </ul>
                                    </div>
                                </form>
                                <form class="navbar-form navbar-left">
                                    <div class="input-group input-group-sm">
                                        <input type="text" class="form-control" placeholder="${g.message(code:"header.identifierSearch")}">
                                        <span class="input-group-btn">
                                            <button type="button" class="btn btn-default">
                                                <i class="glyphicon glyphicon-search"><span class="sr-only">Search</span></i>
                                            </button>
                                        </span>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                    <g:if test="${!disableMenu}">
                    <div class="row">
                        <div class="col-xs-12">
                            <div class="collapse navbar-collapse menu-navbar">
                                <ul class="nav navbar-nav navbar-left">
                                    <li><a href="#">${g.message(code: "otp.menu.sample")}</a></li>
                                    <li><a href="#">${g.message(code: "otp.menu.run")}</a></li>
                                    <li><a href="#">${g.message(code: "otp.menu.lane")}</a></li>
                                    <sec:ifAnyGranted roles="ROLE_OPERATOR">
                                        <li class="dropdown">
                                            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">${g.message(code: "otp.menu.statistics")}<span class="caret"></span></a>
                                            <ul class="dropdown-menu">
                                                <li><g:link controller="clusterJobGeneral" action="index"><g:message code="otp.menu.jobstats.general"/></g:link></li>
                                                <li><g:link controller="clusterJobJobTypeSpecific" action="index">${g.message(code: "otp.menu.jobstats.jobTypeSpecific")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="processingTimeStatistics" action="index">${g.message(code: "otp.menu.processingTimeStatistics")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="statistics" action="kpi">${g.message(code: "otp.menu.kpi")}</g:link></li>
                                            </ul>
                                        </li>
                                        <li class="dropdown">
                                            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Operator <span class="caret"></span></a>
                                            <ul class="dropdown-menu">
                                                <li><g:link controller="metadataImport">${g.message(code: "otp.menu.importAndValidation")}</g:link></li>
                                                <li><g:link controller="sampleIdentifierOverview">${g.message(code: "otp.menu.sampleIdentifierOverview")}</g:link></li>
                                                <li><g:link controller="metadataImport" action="blacklistedIlseNumbers">${g.message(code: "otp.menu.blacklistedIlseNumbers")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="projectCreation">${g.message(code: "otp.menu.projectCreation")}</g:link></li>
                                                <li><g:link controller="projectConfig">${g.message(code: "otp.menu.projectConfig")}</g:link></li>
                                                <li><g:link controller="individual" action="insert">${g.message(code: "otp.menu.createIndividual")}</g:link></li>
                                                <li><g:link controller="bulkSampleCreation" action="index">${g.message(code: "otp.menu.createSample")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="processes" action="list">${g.message(code: "otp.menu.processes")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="mergingCriteria" action="defaultSeqPlatformGroupConfiguration">${g.message(code: "otp.menu.seqPlatformGroup")}</g:link></li>
                                                <li><g:link controller="metaDataFields">${g.message(code: "otp.menu.metaDataFields")}</g:link></li>
                                                <li><g:link controller="softwareTool" action="list">${g.message(code: "otp.menu.softwareTool")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="processingTimeStatistics">${g.message(code: "otp.menu.processingTimeStatistics")}</g:link></li>
                                            </ul>
                                        </li>
                                    </sec:ifAnyGranted>
                                    <sec:ifAnyGranted roles="ROLE_ADMIN">
                                        <li class="dropdown">
                                            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Admin <span class="caret"></span></a>
                                            <ul class="dropdown-menu">
                                                <li><g:link controller="userAdministration">${g.message(code: "otp.menu.userAdministration")}</g:link></li>
                                                <li><g:link controller="roles">${g.message(code: "otp.menu.roles")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="processingOption">${g.message(code: "otp.menu.processingOptions")}</g:link></li>
                                                <li role="separator" class="divider"></li>
                                                <li><g:link controller="crashRecovery">${g.message(code: "otp.menu.crashRecovery")}</g:link></li>
                                                <li><g:link controller="shutdown">${g.message(code: "otp.menu.planServerShutdown")}</g:link></li>
                                            </ul>
                                        </li>
                                    </sec:ifAnyGranted>
                                    <li><g:link controller="home" action="index">${g.message(code: "otp.menu.defaultLayout")}</g:link></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    </g:if>
                </div>
            </div>
        </div>
    </nav>
</sec:ifLoggedIn>
<sec:ifNotLoggedIn>
    <div class="container">
        <div class="row" style="z-index: 20;">
            <div class="col-xs-12 col-sm-4 logo">
                <div class="logo-container">
                    <div class="logo-img">
                        <g:link class="logo" uri="/">
                            <img class="img-responsive" src="${assetPath(src: 'v2/otp-logo.png')}" alt="OTP">
                        </g:link>
                    </div>
                    <div class="logo-slogan">
                        <h1>
                            One<br>Touch<br>Pipeline
                            <g:if test="${otp.environmentName() != 'production'}">
                                <br><span class="environment-name">${otp.environmentName()}</span>
                            </g:if>
                        </h1>
                    </div>
                </div>
            </div>
            <div class="col-xs-12 col-sm-8">
                <div id="login-container">
                    <form class="login-form navbar-form navbar-right" role="form" method="POST" action="${createLink(controller: 'j_spring_security_check')}">
                        <div class="input-group">
                            <span class="input-group-addon"><i class="glyphicon glyphicon-user"></i></span>
                            <input id="account" type="text" class="form-control js-account" name="j_username" value="${username}" placeholder="${g.message(code:"login.form.account")}">
                        </div>
                        <div class="input-group">
                            <span class="input-group-addon"><i class="glyphicon glyphicon-lock"></i></span>
                            <input id="password" type="password" class="form-control" name="j_password" value="" placeholder="${g.message(code:"login.form.password")}">
                        </div>
                        <button type="submit" class="btn btn-primary login-btn"><g:message code="login.form.login" /></button>
                        <a class="register-btn pull-left" data-toggle="modal" href="#register"><g:message code="login.form.request" /></a>
                    </form>
                </div>
            </div>
        </div>
    </div>
</sec:ifNotLoggedIn>

<g:layoutBody/>

<div class="container"><br></div>
<div class="container footer">
    <div class="row">
        <div class="col-lg-12 footer-copyright">
            <ul>
                <li><a data-toggle="modal" href="#contact">${g.message(code: "info.contact.title")}</a></li>
                <li><g:link controller="info" action="imprint"><g:message code="info.imprint.link"/></g:link></li>
                <li><g:link controller="privacyPolicy"><g:message code="info.privacyPolicy.link"/></g:link></li>
                <li>
                    &copy;2011-2020
                    <a href="https://www.dkfz.de" target="_blank">DKFZ</a>,
                    <a href="https://www.uni-heidelberg.de/" target="_blank">Universität Heidelberg</a>,
                    <a href="https://www.charite.de/" target="_blank">Charité</a>,
                    <a href="https://www.klinikum.uni-heidelberg.de/" target="_blank">Universitätsklinikum Heidelberg</a>
                </li>
                <li class="copyright-grey">${version}</li>
            </ul>
        </div>
    </div>
</div>

<a class="scroll-to-top js-scroll-to-top" href="#">
    <img class="img-responsive" src="${assetPath(src: 'v2/to-top.png')}" alt="">
</a>

<otp:modal modalId="contact" title="${g.message(code: "info.contact.title")}">
    <p>${g.message(code: "info.contact.operatedBy")}</p>
    <p class="keep-whitespace">${contactDataOperatedBy}</p>
    <h2>${g.message(code: "info.contact.support")}</h2>
    <p class="keep-whitespace"><a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a></p>
    <h2>${g.message(code: "info.contact.personInCharge")}</h2>
    <p class="keep-whitespace">${contactDataPersonInCharge}</p>
    <h2>${g.message(code: "info.contact.postalAddress")}</h2>
    <p class="keep-whitespace">${contactDataPostalAddress}</p>
</otp:modal>
<asset:deferredScripts/>
</body>
</html>
