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
<%@ page import="de.dkfz.tbi.otp.tracking.OtrsTicket" %>

<g:hiddenField name="otrsTicket.id" value="${otrsTicket.id}"/>
<g:hiddenField name="fastqImportInstance.id" value="${fastqImportInstance.id}"/>

<div id="notification-selection-container">
    <h3><g:message code="notification.notificationSelection.notification.processing"/></h3>
    <g:each var="step" status="i" in="${OtrsTicket.ProcessingStep.values() - OtrsTicket.ProcessingStep.FASTQC}">
        <label class="vertical-align-middle">
            <g:checkBox name="steps[${i}]" value="${step.name()}" checked="${cmd ? ((step in cmd.steps) ? "true" : "false") : "true"}"/> ${step.displayName.capitalize()}
        </label>
        <br>
    </g:each>

    <h3><g:message code="notification.notificationSelection.notification.other"/></h3>
    <label class="vertical-align-middle">
        <g:checkBox name="notifyQcThresholds" value="true" checked="${cmd ? cmd.notifyQcThresholds : "true"}"/> <g:message code="notification.notificationSelection.notification.other.qc"/>
    </label>
</div>
