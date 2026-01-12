%{--
  - Copyright 2011-2025 The OTP authors
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
    <title><g:message code="triggerWorkflows.title"/></title>
    <asset:javascript src="pages/triggerWorkflows/index/functions.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <nav class="navbar navbar-light bg-light">
        <span class="navbar-brand mb-0 h1"><g:message code="triggerWorkflows.title"/></span>
    </nav>

    <div class="mt-3">
        <g:render template="/triggerWorkflows/components/inputArea" model="[tabs: ['project', 'pid', 'seqTrackId', 'ilse', 'multiInput', 'bam'], seqTypes: seqTypes]"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerWorkflows/components/warnArea" model="[warnings: warnings]"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerWorkflows/components/checkArea"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerWorkflows/components/infoArea" model="[info: info]"/>
    </div>

    <div class="mt-3">
        <g:render template="/triggerWorkflows/components/optionArea" model="[deciders: deciders]"/>
    </div>
    <nav class="navbar navbar-light bg-light mt-3">
        <div class="nav-item">
            <button id="triggerWorkflowsButton" class="btn btn-primary nav-item mr-2" onclick="$.otp.triggerWorkflows.trigger(this)">
                <g:message code="triggerWorkflows.triggerButton"/>
            </button>
        </div>
    </nav>

    <div class="mt-3">
        <g:render template="/triggerWorkflows/components/resultArea"/>
    </div>
</div>
</body>
</html>
