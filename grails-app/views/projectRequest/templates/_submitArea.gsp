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

<div class="submit-area">
    <g:if test="${buttonActions*.text.contains("Submit")}">
        <otp:annotation type="info">
            ${g.message(code: "projectRequest.explain.submitter")}
        </otp:annotation>
    </g:if>
    <div class="actions">
        <g:each var="buttonAction" in="${buttonActions}">
            <g:if test="${buttonAction.text == 'Delete'}">
                <a id="delete-request-btn" data-project-request-id="projectRequestId" class="btn btn-${buttonAction.styling}" data-req-id="${projectRequestId}">Delete</a>
            </g:if>
            <g:else>
                <g:actionSubmit id="${buttonAction.action}-request-btn" action="${buttonAction.action}" name="${buttonAction.action}" class="btn btn-${buttonAction.styling}" value="${buttonAction.text}"/>
            </g:else>
        </g:each>
    </div>
</div>

<otp:otpModal modalId="confirmationModal" title="Delete Project Request" type="dialog" closeText="Cancel" confirmText="Delete" closable="false">
</otp:otpModal>
