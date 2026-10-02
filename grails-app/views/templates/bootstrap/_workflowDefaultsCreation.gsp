%{--
  - Copyright 2011-2026 The OTP authors
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

<ul class="nav nav-tabs" id="myTab" role="tablist">
    <li class="nav-item" role="presentation">
        <a class="nav-link active" id="analysis-wf-tab" data-bs-toggle="tab" href="#analysis-wf" role="tab" aria-controls="analysis-wf" aria-selected="true">
            ${g.message(code: "workflowSelection.analysis")}
        </a>
    </li>
</ul>


<div class="tab-content" id="inputTabsContent">
    <div class="tab-pane fade show active mt-2" id="analysis-wf" role="tabpanel" aria-labelledby="analysis-wf-tab">
        <div class="container analysis-wf-form">
            <div class="row">
                <h3>${g.message(code: "workflowSystemConfig.defaults")}</h3>

                <div class="mb-2">
                    <button type="button" class="btn btn-primary" id="add-workflow-default-btn">
                        <i class="bi bi-plus-lg"></i> ${g.message(code: "workflowSystemConfig.default.add")}
                    </button>
                </div>

                <table id="analysisDefaultsTable" class="table table-sm table-striped table-bordered table-hover"
                       data-empty-message="${g.message(code: "workflowSystemConfig.default.none")}"
                       data-edit-label="${g.message(code: "workflowSystemConfig.default.edit")}">
                    <thead>
                    <tr>
                        <th>${g.message(code: "workflowSystemConfig.default.name")}</th>
                        <th>${g.message(code: "workflowSelection.workflow")}</th>
                        <th>${g.message(code: "workflowSystemConfig.default.seqType")}</th>
                        <th>${g.message(code: "workflowSelection.version")}</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<select id="workflow-default-workflow-template" hidden class="form-select">
    <option value="">${g.message(code: "workflowSelection.notConfigured")}</option>
    <g:each in="${analysisWorkflows}" var="wf">
        <option value="${wf.id}">${wf.displayName}</option>
    </g:each>
</select>

<otp:otpModal modalId="workflowDefaultModal" title="${g.message(code: 'workflowSystemConfig.default.modal.title.add')}" type="dialog" classes="modal-lg"
              closeText="${g.message(code: 'workflowSystemConfig.modal.cancel')}"
              confirmText="${g.message(code: 'workflowSystemConfig.modal.confirm')}" closable="false">
    <form>
        <div class="form-group">
            <label for="default-modal-name">${g.message(code: "workflowSystemConfig.default.modal.name")}</label>
            <input type="text" class="form-control" id="default-modal-name">
        </div>

        <div class="form-group">
            <label for="default-modal-seqType">${g.message(code: "workflowSystemConfig.default.modal.seqType")}</label>
            <g:select id="default-modal-seqType"
                      name="default-modal-seqType"
                      class="form-control"
                      value=""
                      from="${seqTypes}"
                      optionKey="id"
                      optionValue="displayNameWithLibraryLayout"/>
        </div>

        <table class="table table-sm">
            <thead>
                <tr>
                    <th>${g.message(code: "workflowSystemConfig.default.modal.workflow")}</th>
                    <th>${g.message(code: "workflowSystemConfig.default.modal.version")}</th>
                    <th></th>
                </tr>
            </thead>
            <tbody id="default-modal-entries-body">
            </tbody>
        </table>
        <button type="button" class="btn btn-outline-primary btn-sm" id="default-modal-add-row-btn">
            <i class="bi bi-plus-lg"></i> ${g.message(code: "workflowSystemConfig.default.modal.addRow")}
        </button>

        <button type="button" class="btn btn-outline-danger btn-sm float-end d-none" id="default-modal-delete-btn"
                data-confirm-message="${g.message(code: "workflowSystemConfig.default.modal.deleteConfirm")}">
            <i class="bi bi-trash"></i> ${g.message(code: "workflowSystemConfig.default.delete")}
        </button>
    </form>
</otp:otpModal>
