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
<g:applyLayout name="main">
<head>
    <title><g:layoutTitle default="OTP"/></title>
    <g:layoutHead/>
    <asset:stylesheet src="info.less"/>
    <asset:javascript src="common/FocusLogin.js"/>
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
            <a href="${g.createLink(action: "imprint")}">
                <div class="infoMenuButton ${(actionName == "imprint") ? "infoMenuButtonActive" : ""}">
                    <div class="infoMenuButtonText"><g:message code="info.imprint.link" /></div>
                </div>
            </a>
            <g:if test="${showPartners}">
                <a href="${g.createLink(action: "partners")}">
                    <div class="infoMenuButton ${(actionName == "partners") ? "infoMenuButtonActive" : ""}">
                        <div class="infoMenuButtonText"><g:message code="info.partners.link" /></div>
                    </div>
                </a>
            </g:if>

        </div>

        <sec:ifNotLoggedIn>
            <g:render template="/auth/login"/>
        </sec:ifNotLoggedIn>

        <div id="content">
            <g:layoutBody/>
        </div>
    </div>
</body>
</g:applyLayout>
