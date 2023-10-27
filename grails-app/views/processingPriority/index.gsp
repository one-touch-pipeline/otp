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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <asset:stylesheet src="pages/processingPriority/styles.less"/>
    <asset:javascript src="pages/processingPriority/index/functions.js"/>
    <g:set var="entityName" value="${message(code: 'processingPriority.title', default: 'Processing Priority')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>
<div class="container-fluid otp-main-container">
    <h3><g:message code="processingPriority.title"/></h3>

    <p><g:message code="processingPriority.subtitle"/></p>

    <div class="alert alert-info" role="alert" style="width: 100%">
        ${g.message(code: "processingPriority.note.title")}
        <ul>
            <li>${g.message(code: "processingPriority.note.item1")}</li>
            <li>${g.message(code: "processingPriority.note.item2")}</li>
            <li>${g.message(code: "processingPriority.note.item3",
                    args: ["https://gitlab.com/one-touch-pipeline/otp-roddy-config/-/blob/master/cluster-queue-names.txt"])}
            </li>
            <li>${g.message(code: "processingPriority.note.item4")}</li>
        </ul>
    </div>

    <table id="processingPriority" class="table table-sm table-striped table-hover table-bordered" data-page-length='16'  style="width:100%">
        <thead>
        <tr>
            <th>Id</th>
            <th>Version</th>
            <th title="${g.message(code: "processingPriority.tooltip.name")}">
                ${g.message(code: "processingPriority.label.name")}
            </th>
            <th title="${g.message(code: "processingPriority.tooltip.priority")}">
                ${g.message(code: "processingPriority.label.priority")}
            </th>
            <th title="${g.message(code: "processingPriority.tooltip.allowedParallelWorkflowRuns")}">
                ${g.message(code: "processingPriority.label.allowedParallelWorkflowRuns")}
            </th>
            <th title="${g.message(code: "processingPriority.tooltip.queue")}">
                ${g.message(code: "processingPriority.label.queue")}
            </th>
            <th title="${g.message(code: "processingPriority.tooltip.roddyConfigSuffix")}">
                ${g.message(code: "processingPriority.label.roddyConfigSuffix")}
            </th>
            <th title="${g.message(code: "processingPriority.tooltip.errorMailPrefix")}">
                ${g.message(code: "processingPriority.label.errorMailPrefix")}
            </th>
            <th title="${g.message(code: "processingPriority.tooltip.action")}">
                ${g.message(code: "processingPriority.label.action")}
            </th>
        </tr>
        </thead>
        <tbody>
        <g:each status="rowIndex" in="${processingPriorityList}" var="processingPriority">
            <tr>
                <td>${processingPriority.id}</td>
                <td>${processingPriority.version}</td>
                <td>${processingPriority.name}</td>
                <td>${processingPriority.priority}</td>
                <td>${processingPriority.allowedParallelWorkflowRuns}</td>
                <td>${processingPriority.queue}</td>
                <td>${processingPriority.roddyConfigSuffix}</td>
                <td>${processingPriority.errorMailPrefix}</td>
                <td>
                    <button id="delete-row" class="btn btn-sm btn-outline-danger" type="button" title="${g.message(code: "processingPriority.tooltip.delete")}">
                        <i class="bi bi-trash"></i>
                    </button>
                    <button id="edit-row" class="btn btn-sm btn-outline-secondary" type="button" title="${g.message(code: "processingPriority.tooltip.edit")}">
                        <i class="bi bi-pencil"></i>
                    </button>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>

<!-- Modal dialog to modify/create -->
<div class="modal fade" id="processingPriorityModal" tabindex="-1" role="dialog" aria-labelledby="processingPriorityModal" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="exampleModalLabel"><g:message code="processingPriority.title"/></h5>
                <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>

            <div class="modal-body">
                <input type="number" class="d-none" id="pp-row" name="row" value="-1">

                <div id="confirm-delete" class="d-none container-fluid">
                    <i class="bi bi-question-octagon p-3 text-danger" style="font-size: 1.5em;"></i>
                    <g:message code="processingPriority.delete.confirmation"/>
                </div>

                <div id="edit-processingPriority">
                    <form id="processingPriorityForm" class="needs-validation" novalidate>
                        <div class="form-group d-none">
                            <input type="number" class="form-control" id="pp-id" name="id">
                        </div>

                        <div class="form-group d-none">
                            <input type="number" class="form-control" id="pp-version" name="version">
                        </div>

                        <div class="form-group">
                            <label for="pp-name" class="col-form-label"><g:message code="processingPriority.label.name"/>:</label>
                            <input type="text" class="form-control" id="pp-name" name="name" required
                                   title="<g:message code="processingPriority.tooltip.name"/>"
                                   placeholder="<g:message code="processingPriority.tooltip.name"/>"
                                   oninput="$.otp.processingPriority.validateName(this)">
                            <div class="invalid-feedback d-none" id="vf-name">
                                <g:message code="processingPriority.validation.notUnique"/>
                            </div>
                        </div>

                        <div class="form-group has-validation">
                            <label for="pp-priority" class="col-form-label"><g:message code="processingPriority.label.priority"/>:</label>
                            <input type="number" class="form-control" id="pp-priority" name="priority" required
                                   title="<g:message code="processingPriority.tooltip.priority"/>"
                                   placeholder="<g:message code="processingPriority.tooltip.priority"/>"
                                   oninput="$.otp.processingPriority.validatePriority(this)">
                            <div class="invalid-feedback d-none" id="vf-priority">
                                <g:message code="processingPriority.validation.notUnique"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="pp-allowedParallelWorkflowRuns" class="col-form-label"><g:message code="processingPriority.label.allowedParallelWorkflowRuns"/>:</label>
                            <input type="number" class="form-control" id="pp-allowedParallelWorkflowRuns" name="allowedParallelWorkflowRuns" required
                                   title="<g:message code="processingPriority.tooltip.allowedParallelWorkflowRuns"/>"
                                   placeholder="<g:message code="processingPriority.tooltip.allowedParallelWorkflowRuns"/>"
                                   min="1"
                                   oninput="$.otp.processingPriority.validateAllowedParallelWorkflowRuns(this)">
                            <div class="invalid-feedback d-none" id="vf-allowedParallelWorkflowRuns">
                                <g:message code="processingPriority.tooltip.allowedParallelWorkflowRuns"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="pp-queue" class="col-form-label"><g:message code="processingPriority.label.queue"/>:</label>
                            <input type="text" class="form-control" id="pp-queue" name="queue" required
                                   title="<g:message code="processingPriority.tooltip.queue"/>"
                                   placeholder="<g:message code="processingPriority.tooltip.queue"/>"
                                   oninput="$.otp.processingPriority.validateString(this)">
                            <div class="invalid-feedback d-none" id="vf-queue">
                                <g:message code="processingPriority.tooltip.queue"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="pp-roddyConfigSuffix" class="col-form-label"><g:message code="processingPriority.label.roddyConfigSuffix"/>:</label>
                            <input type="text" class="form-control" id="pp-roddyConfigSuffix" name="roddyConfigSuffix" required
                                   title="<g:message code="processingPriority.tooltip.roddyConfigSuffix"/>"
                                   placeholder="<g:message code="processingPriority.tooltip.roddyConfigSuffix"/>"
                                   oninput="$.otp.processingPriority.validateString(this)">
                            <div class="invalid-feedback d-none" id="vf-roddyConfigSuffix">
                                <g:message code="processingPriority.tooltip.roddyConfigSuffix"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="pp-errorMailPrefix" class="col-form-label"><g:message code="processingPriority.label.errorMailPrefix"/>:</label>
                            <input type="text" class="form-control" id="pp-errorMailPrefix" name="errorMailPrefix" required
                                   title="<g:message code="processingPriority.tooltip.errorMailPrefix"/>"
                                   placeholder="<g:message code="processingPriority.tooltip.errorMailPrefix"/>"
                                   oninput="$.otp.processingPriority.validateString(this)">
                            <div class="invalid-feedback d-none" id="vf-errorMailPrefix">
                                <g:message code="processingPriority.tooltip.errorMailPrefix"/>
                            </div>
                        </div>
                    </form>
                </div>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><g:message code="default.button.cancel.label"/></button>
                <button type="button" class="btn btn-primary" id="bt-save" onclick="$.otp.processingPriority.save()" disabled>
                    <g:message code="default.button.save.label"/>
                </button>
                <button type="button" class="btn btn-primary d-none" id="bt-delete" onclick="$.otp.processingPriority.remove()">
                    <g:message code="default.button.delete.label"/>
                </button>
            </div>
        </div>
    </div>
</div>
</body>
</html>
