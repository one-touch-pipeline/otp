%{--
  - Copyright 2011-2024 The OTP authors
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
<%@ page import="de.dkfz.tbi.otp.tracking.Ticket" %>

<g:hiddenField name="ticket.id" value="${ticket.id}"/>
<g:hiddenField name="fastqImportInstance.id" value="${fastqImportInstanceId}"/>

<div id="notification-selection-container">
    <h3><g:message code="notification.notificationSelection.notification.processing"/></h3>
    <g:each var="step" status="i" in="${Ticket.ProcessingStep.values() - Ticket.ProcessingStep.FASTQC}">
        <div class="form-check">
            <g:checkBox class="form-check-input" name="steps[${i}]" value="${step.name()}"
                        checked="${cmd ? ((step in cmd.steps) ? "true" : "false") : "true"}"/>
            <label class="form-check-label vertical-align-middle" for="steps[${i}]">
                ${step.displayName.capitalize()}
            </label>
        </div>
    </g:each>

    <h3><g:message code="notification.notificationSelection.notification.other"/></h3>

    <div class="form-check">
        <g:checkBox class="form-check-input" name="notifyQcThresholds" value="true" checked="${cmd ? cmd.notifyQcThresholds : "true"}"/>

        <label class="form-check-label vertical-align-middle" for="notifyQcThresholds">
            <g:message code="notification.notificationSelection.notification.other.qc"/>
        </label>
    </div>

</div>
