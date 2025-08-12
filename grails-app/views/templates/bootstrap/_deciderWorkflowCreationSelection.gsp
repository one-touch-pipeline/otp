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

%{--
  - Workflow creation selection for a decider
  -
  - Required params:
  - decider, see @DeciderWithActions
  -
  - Usage example: <g:render template="/templates/bootstrap/deciderWorkflowCreationSelection" model="[decider: decider]"/>
  --}%
<div class="row mb-3">
    <label for="${decider.name}" class="col form-label">${decider.name}</label>
    <div class="col">
        <select id="${decider.name}" class="form-control use-select-2" name="${decider.name}">
            <g:each in="${decider.createActions}" var="createAction">
                <option value="${createAction.id}"><g:message code="deciderCreateWorkflowAction.${createAction}.name"/></option>
            </g:each>
        </select>
    </div>
</div>
