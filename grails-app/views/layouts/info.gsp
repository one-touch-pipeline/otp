<%@ page import="de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName; de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService" %>
<g:applyLayout name="main">
<head>
<title><g:layoutTitle default="OTP"/></title>
<g:layoutHead/>
    <asset:javascript src="modules/info.js"/>
    <asset:stylesheet src="modules/info.css"/>
</head>

<body>
<div class="body">
    <div id="infoMenu">
        <a href="${g.createLink(action: "about")}">
            <div class="infoMenuButton ${(actionName == "about") ? "infoMenuButtonActive" : ""}">
                <div class="infoMenuButtonText"><g:message code="info.about.link" /></div>
            </div>
        </a>
        <a href="${g.createLink(action: "numbers")}">
            <div class="infoMenuButton ${(actionName == "numbers") ? "infoMenuButtonActive" : ""}">
                <div class="infoMenuButtonText"><g:message code="info.numbers.link" /></div>
            </div>
        </a>
        <a href="${g.createLink(action: "contact")}">
            <div class="infoMenuButton ${(actionName == "contact") ? "infoMenuButtonActive" : ""}">
                <div class="infoMenuButtonText"><g:message code="info.contact.link" /></div>
            </div>
        </a>
        <g:if test="${ProcessingOptionService.findOptionSafe(OptionName.GUI_SHOW_PARTNERS, null, null) == "true"}">
            <a href="${g.createLink(action: "partners")}">
                <div class="infoMenuButton ${(actionName == "partners") ? "infoMenuButtonActive" : ""}">
                    <div class="infoMenuButtonText"><g:message code="info.partners.link" /></div>
                </div>
            </a>
        </g:if>
    </div>

    <sec:ifNotLoggedIn>
        <g:render template="/login/login"/>
    </sec:ifNotLoggedIn>

    <div id="content" style="position: absolute; top: 80px; left: 0px;" >
        <g:layoutBody/>
    </div>
</div>
</body>
</g:applyLayout>
