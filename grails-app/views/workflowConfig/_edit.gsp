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

<!-- Modal dialog to modify/create/deprecate selector -->
<div class="modal fade" id="workflowConfigModal" data-backdrop="static" tabindex="-1" role="dialog" aria-labelledby="workflowConfigModal" aria-hidden="true">
    <div class="modal-dialog modal-xl" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="workflowConfigModalLabel"><g:message code="workflowConfig.dialog.title"/></h5>
                <button type="button" class="btn btn-primary close" data-bs-dismiss="modal" aria-label="Close">
                    <i class="bi bi-x" aria-hidden="true"></i>
                </button>
            </div>

            <div class="modal-body container-fluid">
                <p id="confirmContent" class="d-none">
                    <em class="fragmentName"></em>
                    <g:message code="workflowConfig.fragment.deprecate.info"/>
                </p>

                <form id="workflowConfigModalForm" class="needs-validation" novalidate>
                    <div class="form-group d-none">
                        <input type="number" class="form-control" id="pp-id" name="selector.id"/>
                        <input type="text" class="form-control" id="pp-operation"/>
                        <input type="text" class="form-control" id="pp-fragmentName" name="fragmentName"/>
                    </div>

                    <div class="row">
                        <div class="col">
                            <div class="input-group mb-3">
                                <div class="input-group-prepend">
                                    <label class="input-group-text"><g:message code="workflowConfig.selector.name"/></label>
                                </div>
                                <input id="pp-selector" type="text" class="form-control" name="selectorName"
                                       oninput="$.otp.workflowConfig.validateName(this)"/>
                            </div>
                        </div>
                    </div>

                    <g:render template="search" model="[isEdit: true]"/>

                    <div class="row">
                        <div class="col">
                            <div class="input-group mb-3">
                                <div class="input-group-prepend">
                                    <label class="input-group-text"><g:message code="workflowConfig.selector.type"/></label>
                                </div>
                                <select name="type" class="custom-select use-select-2">
                                    <g:each in="${selectorTypes}" var="selectorType">
                                        <option value="${selectorType}">${selectorType}</option>
                                    </g:each>
                                </select>
                            </div>
                        </div>

                        <div class="col">
                            <div class="input-group mb-3">
                                <div class="input-group-prepend">
                                    <label class="input-group-text"><g:message code="workflowConfig.selector.priority"/></label>
                                </div>
                                <input type="number" class="form-control"  name="priority" required
                                       title='<g:message code="workflowConfig.selector.priority.tooltip"/>'
                                       placeholder='<g:message code="workflowConfig.selector.priority.tooltip"/>'
                                       oninput="$.otp.workflowConfig.validatePriority(this)"/>

                                <div class="invalid-feedback d-none" id="vf-priority">
                                    <g:message code="workflowConfig.validation.notValid"/>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-sm">
                            <div class="input-group mb-3">
                                <div class="input-group-prepend">
                                    <label class="input-group-text"><g:message code="workflowConfig.fragment.version"/></label>
                                </div>
                                <select id="pp-fragments" name="fragment.id" class="form-control use-select-2"></select>

                                <div class="input-group-append">
                                    <button id="format-button" class="btn btn-outline-primary format"
                                            type="button"><g:message code="workflowConfig.button.format"/></button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="row">
                        <div class="col-sm">
                            <div class="input-group mb-3">
                                <div class="input-group-prepend">
                                    <label class="input-group-text"><g:message code="workflowConfig.fragment.value"/></label>
                                </div>
                                <textarea name="value" class="md-textarea form-control" rows="10"></textarea>
                            </div>
                        </div>
                    </div>
                </form>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    <g:message code="default.button.cancel.label"/>
                </button>
                <button class="btn btn-warning check" title="${g.message(code: "workflowConfig.button.check.title")}" type="button">
                    <g:message code="workflowConfig.button.check"/>
                </button>
                <button type="button" class="btn btn-primary" id="save-button">
                    <g:message code="default.button.ok.label"/>
                </button>
            </div>
        </div>
    </div>
</div>
