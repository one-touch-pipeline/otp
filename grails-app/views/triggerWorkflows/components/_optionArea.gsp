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
  - Option Area
  -
  - Required params:
  - deciders, a json object list with [{ name: "Fastqc", creationTypes : [ "Create missing", "Xxx", ...]}]
  -
  - Usage example: <g:render template="/templates/bootstrap/optionArea" model="[deciders: deciders]"/>
  --}%
<div class="card">
    <div class="card-header">
        <i class="bi bi-list-check"></i> <g:message code="triggerWorkflows.option.header"/>
    </div>
    <div class="card-body">
        <div class="form-check">
            <input class="form-check-input" type="checkbox" id="ignoreSeqPlatformGroup">
            <label class="form-check-label" for="ignoreSeqPlatformGroup"><g:message code="triggerWorkflows.input.checkbox.ignoreSeqPlatformGroup"/></label>
        </div>
        <div class="card">
            <div class="card-header">
                <i class="bi bi-check2-square"></i> <g:message code="triggerWorkflows.option.decider.header"/>
            </div>
            <div class="card-body row">
                <form id="deciderActionSelection" class="col-md-4">
                    <g:each in="${deciders}" var="decider">
                        <g:render template="/templates/bootstrap/deciderWorkflowCreationSelection" model="[decider: decider]"/>
                    </g:each>
                </form>
                <div class="col">
                    <div><g:message code="triggerWorkflows.option.decider.selection.header"/>
                        <ul class="list-group">
                            <g:each in="${deciderActions}" var="action">
                                <li class="list-group-item">
                                    <div class="fw-bold"><g:message code="deciderCreateWorkflowAction.${action}.name"/>: </div>
                                    <g:message code="deciderCreateWorkflowAction.${action}.description"/>
                                </li>
                            </g:each>
                        </ul>

                    </div>
                    <div><g:message code="triggerWorkflows.option.decider.notes.header"/>
                        <ul class="list-group">
                            <g:each in="${deciderNotes}" var="note">
                                <li class="list-group-item">
                                    <div class="fw-bold">${note.key}: </div>
                                    ${note.value}
                                </li>
                            </g:each>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
