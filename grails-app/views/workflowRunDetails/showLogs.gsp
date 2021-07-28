%{--
  - Copyright 2011-2020 The OTP authors
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
<html>
<head>
    <title>${g.message(code: "workflowRun.details.log")}</title>
    <asset:javascript src="pages/workflowRunList/common.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <nav class="navbar">
        <div class="navbar-brand">
            <div id="statusDot" title="${step.state}" data-status="${step.state}" class="d-inline-block"></div>
            <span class="d-inline-flex align-top">${g.message(code: "workflowRun.details.log")} for ${step.beanName}</span>
        </div>
    </nav>

    <div class="dropdown-divider"></div>

    ${raw(step.workflowRun.displayName.replace("\n", "<br>"))}
    <br>
    <g:link action="index" id="${step.workflowRun.id}">${g.message(code: "workflowRun.details.log.back.link")}</g:link>

    <g:each in="${messages}" var="message">
        <hr>
        <pre>${message}</pre>
    </g:each>
    <g:if test="${messages.empty}">
        <hr>
        <pre>${g.message(code: "workflowRun.details.log.none")}</pre>
    </g:if>
</div>
</body>
</html>
