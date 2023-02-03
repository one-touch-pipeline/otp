%{--
  - Copyright 2011-2021 The OTP authors
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
    <title>${g.message(code: "workflowSystemConfig.title")}</title>
    <asset:javascript src="pages/workflowSystemConfig/index.js"/>
</head>

<body>
    <div class="container-fluid otp-main-container">
        <h1><g:message code="workflowSystemConfig.title"/></h1>

        <table id="workflowOverview" class="table table-striped table-bordered table-hover" style="width:100%">
            <thead>
            <tr>
                <th></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.name.title')}"><g:message code="workflowSystemConfig.th.name"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.priority.title')}"><g:message code="workflowSystemConfig.th.priority"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.enabled.title')}"><g:message code="workflowSystemConfig.th.enabled"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.maxParallel.title')}"><g:message code="workflowSystemConfig.th.maxParallel"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.default.version.title')}"><g:message code="workflowSystemConfig.th.default.version"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.supportedSeqTypes.title')}"><g:message code="workflowSystemConfig.th.supportedSeqTypes"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.allowedRefGen.title')}"><g:message code="workflowSystemConfig.th.allowedRefGen"/></th>
                <th title="${g.message(code: 'workflowSystemConfig.th.deprecated.title')}"><g:message code="workflowSystemConfig.th.deprecated"/></th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>

    <otp:otpModal modalId="editWorkflowModal" title="${g.message(code: 'workflowSystemConfig.modal.title')}" type="dialog"
                  closeText="${g.message(code: 'workflowSystemConfig.modal.cancel')}"
                  confirmText="${g.message(code: 'workflowSystemConfig.modal.confirm')}" closable="false">
        <form>
            <div class="form-group">
                <label for="modal-priority"><g:message code="workflowSystemConfig.modal.priority"/></label>
                <input type="number" class="form-control" id="modal-priority" aria-describedby="priority">
                <small id="priority" class="form-text text-muted"><g:message code="workflowSystemConfig.modal.priority.description"/></small>
            </div>
            <div class="form-group">
                <label for="modal-max-runs"><g:message code="workflowSystemConfig.modal.maxParallel"/></label>
                <input type="number" class="form-control" id="modal-max-runs" aria-describedby="priority">
                <small id="max-runs" class="form-text text-muted"><g:message code="workflowSystemConfig.modal.maxParallel.description"/></small>
            </div>
            <div class="form-group">
                <label for="modal-defaultVersion"><g:message code="workflowSystemConfig.modal.default.version"/></label>
                <select id="modal-defaultVersion"
                        name="modal-defaultVersion"
                        class="form-control use-select-2"
                        data-placeholder="${g.message(code: 'workflowSystemConfig.modal.default.version.placeholder')}">
                </select>
            </div>
            <div class="form-group">
                <label for="modal-seqTypes"><g:message code="workflowSystemConfig.modal.supportedSeqTypes"/></label>
                <g:select id="modal-seqTypes"
                          name="modal-seqTypes"
                          class="form-control use-select-2"
                          multiple="true"
                          value=""
                          from="${seqTypes}"
                          optionKey="id"
                          optionValue="displayNameWithLibraryLayout"
                          data-placeholder="${g.message(code: 'workflowSystemConfig.modal.supportedSeqTypes.placeholder')}"/>
            </div>
            <div class="form-group">
                <label for="modal-refGenomes"><g:message code="workflowSystemConfig.modal.allowedRefGen"/></label>
                <g:select id="modal-refGenomes"
                          name="modal-refGenomes"
                          class="form-control use-select-2"
                          multiple="true"
                          value=""
                          from="${refGenomes}"
                          optionKey="id"
                          optionValue="name"
                          data-placeholder="${g.message(code: 'workflowSystemConfig.modal.allowedRefGen.placeholder')}"/>
            </div>
            <div class="form-group form-check custom-control custom-switch">
                <input type="checkbox" class="custom-control-input" id="modal-enabled">
                <label class="custom-control-label" for="modal-enabled"><g:message code="workflowSystemConfig.modal.enabled"/></label>
            </div>
        </form>
    </otp:otpModal>
</body>
</html>
