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
<!doctype html>
<html lang="en" class="${otp.environmentName()}">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title><g:layoutTitle default="OTP"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="contextPath" content="${request.contextPath}">
    <meta name="uriWithParams" content="${uriWithParams}">
    <meta name="projectName" content="${selectedProject?.name}">
    <meta name="projectParameter" content="${projectParameter}">
    <asset:stylesheet src="mainmenu.less"/>
    <otp:favicon/>
    <g:layoutHead/>
</head>

<g:render template="/templates/stickyNewsBanner"/>

<body id="otp">
<div class="body_position">
    <div class="header">
        <g:if test="${logo}">
            <img class="institute-logo" src="${assetPath(src: logo)}" alt=""/>
        </g:if>
        <div></div>
        <img class="otp-logo" src="${assetPath(src: 'non-free/header-otp.png')}" alt="OTP"/>
        <g:if test="${otp.environmentName() != 'production'}">
            <p class="environmentName"><otp:environmentName/></p>
        </g:if>
    </div>

    <g:if test="${!disableMenu}">
        <div class="menu">
            <div class="menuContainer menuContainerL">
                <ul>
                    <sec:ifLoggedIn>
                        <li class="menuContainerItem"><g:link controller="sampleOverview"><g:message code="otp.menu.sampleOverview"/></g:link></li>
                        <li class="menuContainerItem"><g:link controller="individual" action="list"><g:message code="otp.menu.individuals"/></g:link></li>
                        <li class="menuContainerItem"><g:link controller="sequence" action="index"><g:message code="otp.menu.sequences"/></g:link></li>
                        <li class="navigation menuContainerItem projectMenu">
                            <ul>
                                <li class="nav_container"><a class="menuLinkContainer"><g:message code="otp.menu.project"/> &#9661;</a>
                                    <ul>
                                        <li><g:link controller="projectOverview" action="index"><g:message
                                                code="otp.menu.projectSpecificStatistics"/></g:link></li>
                                        <li><g:link controller="projectConfig" action="index"><g:message code="otp.menu.projectConfig"/></g:link></li>
                                        <li><g:link controller="workflowSelection" action="index"><g:message
                                                code="otp.menu.alignmenAndAnalysis"/></g:link></li>
                                        <li><g:link controller="projectUser" action="index"><g:message code="otp.menu.userManagement"/></g:link></li>
                                        <li><g:link controller="projectRequest" action="index"><g:message
                                                code="otp.menu.projectRequest"/></g:link></li>
                                        <li><g:link controller="sampleIdentifierOverview" action="index"><g:message
                                                code="otp.menu.sampleNameOverview"/></g:link></li>
                                        <sec:ifAnyGranted roles="ROLE_OPERATOR">
                                            <li class="allGranted"><g:link controller="projectInfo" action="list"><g:message
                                                    code="projectOverview.projectInfos" /></g:link></li>
                                            <li class="allGranted"><g:link controller="dataTransfer" action="index"><g:message
                                                    code="dataTransfer.menu.title" /></g:link></li>
                                        </sec:ifAnyGranted>
                                        <sec:ifAnyGranted roles="ROLE_OPERATOR">
                                            <li class="allGranted"><g:link controller="projectFields" action="index"><g:message
                                                    code="otp.menu.configureProjectInformation"/></g:link></li>
                                        </sec:ifAnyGranted>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <li class="navigation menuContainerItem resultsMenu">
                            <ul>
                                <li class="nav_container"><a class="menuLinkContainer"><g:message
                                        code="otp.menu.results"/> &#9661;</a>
                                    <ul>
                                        <li><g:link controller="alignmentQualityOverview" action="index"><g:message
                                                code="otp.menu.alignmentQuality"/></g:link></li><br>
                                        <li><g:link controller="cellRanger" action="finalRunSelection"><g:message
                                                code="otp.menu.cellRanger.finalRunSelection"/></g:link></li><br>
                                        <li><g:link controller="snv" action="results"><g:message code="otp.menu.snv.results"/></g:link></li>
                                        <li><g:link controller="indel" action="results"><g:message code="otp.menu.indel.results"/></g:link></li>
                                        <li><g:link controller="aceseq" action="results"><g:message code="otp.menu.cnv.results"/></g:link></li>
                                        <li><g:link controller="sophia" action="results"><g:message code="otp.menu.sophia.results"/></g:link></li>
                                        <li><g:link controller="runYapsa" action="results"><g:message code="otp.menu.runYapsa.results"/></g:link></li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <sec:ifAnyGranted roles="ROLE_OPERATOR">
                            <li class="menuContainerItem"><g:link controller="egaSubmission" action="overview"><g:message code="otp.menu.ega"/></g:link></li>
                            <li class="navigation menuContainerItem statisticMenu">
                                <ul>
                                    <li class="allGranted nav_container"><a class="menuLinkContainer"><g:message
                                            code="otp.menu.statistics"/> &#9661;</a>
                                        <ul>
                                            <li><g:link controller="clusterJobGeneral" action="index"><g:message
                                                    code="otp.menu.jobstats.general"/></g:link></li><br>
                                            <li><g:link controller="clusterJobJobTypeSpecific" action="index"><g:message
                                                    code="otp.menu.jobstats.jobTypeSpecific"/></g:link></li><br>
                                            <li><g:link controller="processingTimeStatistics" action="index"><g:message
                                                    code="otp.menu.processingTimeStatistics"/></g:link></li>
                                            <li><g:link controller="statistics" action="kpi">${g.message(code: "otp.menu.kpi")}</g:link></li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                            <li class="navigation menuContainerItem operatorMenu">
                                <ul>
                                    <li class="allGranted nav_container"><a class="menuLinkContainer"><g:message
                                            code="otp.menu.operatorSection"/> &#9661;</a>
                                        <ul>
                                            <li><g:link controller="metadataImport" action="index"><g:message
                                                    code="otp.menu.importAndValidation"/></g:link></li>
                                            <li><g:link controller="bamMetadataImport" action="index"><g:message
                                                    code="otp.menu.bamMetadataImport"/></g:link></li>
                                            <li><g:link controller="metadataImport" action="blacklistedIlseNumbers"><g:message
                                                    code="otp.menu.blacklistedIlseNumbers"/></g:link></li>
                                            <li><g:link controller="projectCreation" action="index"><g:message code="otp.menu.projectCreation"/></g:link></li>
                                            <li><g:link controller="individual" action="insert"><g:message code="otp.menu.createIndividual"/></g:link></li>
                                            <li><g:link controller="bulkSampleCreation" action="index"><g:message code="otp.menu.createSample"/></g:link></li>
                                            <li><g:link controller="processes" action="list"><g:message code="otp.menu.processes"/></g:link></li>
                                            <li><g:link controller="defaultSeqPlatformGroup" action="index"><g:message
                                                    code="otp.menu.seqPlatformGroup"/></g:link></li>
                                            <li><g:link controller="metaDataFields" action="index"><g:message code="otp.menu.metaDataFields"/></g:link></li>
                                            <li><g:link controller="qcThreshold" action="defaultConfiguration"><g:message
                                                    code="otp.menu.qcThreshold"/></g:link></li>
                                            <li><g:link controller="document" action="manage"><g:message code="otp.menu.documents"/></g:link></li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                            <li class="navigation menuContainerItem workflowMenu">
                                <ul>
                                    <li class="allGranted nav_container"><a class="menuLinkContainer"><g:message
                                            code="otp.menu.newWorkflow"/> &#9661;</a>
                                        <ul>
                                            <li><g:link controller="systemStatus" action="index"><g:message code="otp.menu.systemStatus"/></g:link></li>
                                            <li><g:link controller="workflowSystemConfig" action="index"><g:message code="otp.menu.systemConfig"/></g:link></li>
                                            <li><g:link controller="workflowSelection" action="index"><g:message code="otp.menu.workflowSelection"/></g:link></li>
                                            <li><g:link controller="workflowRunOverview" action="index"><g:message code="otp.menu.workflowRunOverview"/></g:link></li>
                                            <li><g:link controller="workflowRunList" action="index"><g:message code="otp.menu.workflowRunList"/></g:link></li>
                                            <li><g:link controller="workflowConfig" action="index"><g:message code="otp.menu.workflowConfig"/></g:link></li>
                                            <li><g:link controller="workflowConfigViewer" action="index"><g:message code="otp.menu.workflowConfigViewer"/></g:link></li>
                                            <li><g:link controller="crashRepair" action="index"><g:message code="otp.menu.workflowCrashRepair"/></g:link></li>
                                            <li><g:link controller="workflowJobErrorDefinition" action="index"><g:message code="otp.menu.workflowRestartHandler"/></g:link></li>
                                            <li><g:link controller="triggerAlignment" action="index"><g:message code="otp.menu.triggerAlignment"/></g:link></li>
                                            <li><g:link controller="processingPriority" action="index"><g:message code="otp.menu.processingPriority"/></g:link></li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </sec:ifAnyGranted>
                        <sec:ifAnyGranted roles="ROLE_ADMIN">
                            <li class="navigation menuContainerItem adminMenu">
                                <ul>
                                    <li class="allGranted nav_container"><a class="menuLinkContainer"><g:message
                                            code="otp.menu.adminSection"/> &#9661;</a>
                                        <ul>
                                            <li><g:link controller="userAdministration"><g:message code="otp.menu.userAdministration"/></g:link></li>
                                            <li><g:link controller="roles"><g:message code="otp.menu.roles"/></g:link></li>
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
                            <li class="navigation menuContainerItem">
                                <g:form controller="logout" action="impersonate" method="POST">
                                    <button class="menuLink">${g.message(code: "header.switchBack", args: [sec.switchedUserOriginalUsername()])}</button>
                                </g:form>
                            </li>
                        </sec:ifSwitched>
                    </sec:ifLoggedIn>
                </ul>
            </div>

            <div class="menuContainer_empty"></div>

            <div class="menuContainer menuContainerR">
                <ul>
                    <sec:ifLoggedIn>
                        <li class="menuContainerItem"><g:link controller="logout"><g:message code="otp.menu.logout"/></g:link></li>
                        <li class="navigation menuContainerItem infoMenu">
                            <ul>
                                <li class="nav_container"><a class="menuLinkContainer"><g:message code="info.info.link"/> &#9661;</a>
                                    <ul>
                                        <li><g:link controller="info" action="about"><g:message code="info.about.link"/></g:link></li>
                                        <li><g:link controller="info" action="numbers"><g:message code="info.numbers.link"/></g:link></li>
                                        <li><g:link controller="info" action="contact"><g:message code="info.contact.link"/></g:link></li>
                                        <li><g:link controller="info" action="imprint"><g:message code="info.imprint.link"/></g:link></li>
                                        <g:if test="${showPartners}">
                                            <li><g:link controller="info" action="partners"><g:message code="info.partners.link"/></g:link></li>
                                        </g:if>
                                        <li><g:link controller="info" action="templates"><g:message code="info.templates.link"/></g:link></li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <li class="menuContainerItem"><g:link controller="home" action="index"><g:message code="otp.menu.home"/></g:link></li>
                    </sec:ifLoggedIn>
                </ul>
            </div>
        </div>
    </g:if>
    <div id="infoBox" class="info-box"></div>
    <g:layoutBody/>
    <div class="footer" role="contentinfo">
        &copy;2011-2023
        <a href="https://www.dkfz.de" target="_blank">DKFZ</a>,
        <a href="https://www.uni-heidelberg.de/" target="_blank">Heidelberg University</a>,
        <a href="https://www.charite.de/" target="_blank">Charité</a>,
        <a href="https://www.klinikum.uni-heidelberg.de/" target="_blank">Heidelberg University Hospital</a> |
        <g:link controller="info" action="imprint"><g:message code="info.imprint.link"/></g:link> |
        <g:link controller="privacyPolicy"><g:message code="info.privacyPolicy.link"/></g:link> |
        <g:if test="${faqLink}">
            <a href="${faqLink}" target="_blank">FAQ</a> |
        </g:if>
        ${version}
    </div>
</div>
<asset:deferredScripts/>
</body>
</html>
