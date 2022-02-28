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

<g:if test="${warnings.seqTypes.empty && warnings.seqPlatformGroups.empty && warnings.libraryPreparationKits.empty}">
    <div class="alert alert-primary" role="alert">
        No warnings are found.
    </div>
</g:if>

<div class="accordion" id="warnAreaAccordeon">
    <g:if test="${warnings.seqTypes}">
        <div class="card">
            <div class="card-header">
                <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseOne" aria-expanded="true"
                        aria-controls="collapseOne">
                    <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.missingConfig.title"/>
                </button>
            </div>

            <div id="collapseOne" class="collapse" data-parent="#warnAreaAccordeon">
                <div class="card-body">
                    <div class="alert alert-warning" role="alert">
                        <p class="card-text"><g:message code="triggerAlignment.warn.missingConfig"/></p>
                    </div>

                    <table id="seqTypeWarnings" class="table table-sm table-striped table-hover table-bordered">
                        <thead>
                        <tr>
                            <th><g:message code="triggerAlignment.warn.table.project"/></th>
                            <th><g:message code="triggerAlignment.warn.table.seqType"/></th>
                            <th><g:message code="triggerAlignment.warn.table.effectedLaneCount"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each status="rowIndex" in="${warnings.seqTypes}" var="row">
                            <tr>
                                <td>${row.project.name}</td>
                                <td>${row.seqType.name}</td>
                                <td>${row.laneCount}</td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>

                    <p class="card-text"><g:message code="triggerAlignment.warn.seqType.triggeringWontBeStarted"/></p>
                </div>
            </div>
        </div>
    </g:if>

    <g:if test="${warnings.seqPlatformGroups}">
        <div class="card">
            <div class="card-header">
                <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="true"
                        aria-controls="collapseTwo">
                    <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.mismatchedSeqPlatform.title"/>
                </button>
            </div>

            <div id="collapseTwo" class="collapse" data-parent="#warnAreaAccordeon">
                <div class="card-body">
                    <div class="alert alert-warning" role="alert">
                        <p class="card-text"><g:message code="triggerAlignment.warn.mismatchedSeqPlatform"/></p>
                    </div>

                    <table id="seqPlatformWarnings" class="table table-sm table-striped table-hover table-bordered">
                        <thead>
                        <tr>
                            <th><g:message code="triggerAlignment.warn.table.project"/></th>
                            <th><g:message code="triggerAlignment.warn.table.mockPid"/></th>
                            <th><g:message code="triggerAlignment.warn.table.sampleTypeName"/></th>
                            <th><g:message code="triggerAlignment.warn.table.seqTypeName"/></th>
                            <th><g:message code="triggerAlignment.warn.table.sequencingReadType"/></th>
                            <th><g:message code="triggerAlignment.warn.table.singleCell"/></th>
                            <th><g:message code="triggerAlignment.warn.table.seqPlatformGroupIds"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each status="rowIndex" in="${warnings.seqPlatformGroups}" var="row">
                            <tr>
                                <td>${row.project.name}</td>
                                <td>${row.mockPid}</td>
                                <td>${row.sampleTypeName}</td>
                                <td>${row.seqTypeName}</td>
                                <td>${row.seqReadType}</td>
                                <td>${row.singleCell}</td>
                                <td>
                                    <ul class="list-group">
                                        <g:each in="${row.seqPlatformGroups}" var="group">
                                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                                id: ${group.id}
                                                <span class="badge bg-primary rounded-pill text-light">${group.count}</span>
                                            </li>
                                        </g:each>
                                    </ul>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>

                    <p class="card-text"><g:message code="triggerAlignment.warn.seqPlatform.triggeringStartsOnlyOneGroup"/></p>
                </div>
            </div>
        </div>
    </g:if>

    <g:if test="${warnings.libraryPreparationKits}">
        <div class="card">
            <div class="card-header">
                <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseThree"
                        aria-expanded="true" aria-controls="collapseThree">
                    <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.mismatchedPreparationKit.title"/>
                </button>
            </div>

            <div id="collapseThree" class="collapse" data-parent="#warnAreaAccordeon">
                <div class="card-body">
                    <div class="alert alert-warning" role="alert">
                        <p class="card-text"><g:message code="triggerAlignment.warn.mismatchedPreparationKit"/></p>
                    </div>

                    <table id="libraryPrepKitWarnings" class="table table-sm table-striped table-hover table-bordered">
                        <thead>
                        <tr>
                            <th><g:message code="triggerAlignment.warn.table.project"/></th>
                            <th><g:message code="triggerAlignment.warn.table.mockPid"/></th>
                            <th><g:message code="triggerAlignment.warn.table.sampleTypeName"/></th>
                            <th><g:message code="triggerAlignment.warn.table.seqTypeName"/></th>
                            <th><g:message code="triggerAlignment.warn.table.sequencingReadType"/></th>
                            <th><g:message code="triggerAlignment.warn.table.singleCell"/></th>
                            <th><g:message code="triggerAlignment.warn.table.libraryPreparationKits"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each status="rowIndex" in="${warnings.libraryPreparationKits}" var="row">
                            <tr>
                                <td>${row.project.name}</td>
                                <td>${row.mockPid}</td>
                                <td>${row.sampleTypeName}</td>
                                <td>${row.seqTypeName}</td>
                                <td>${row.seqReadType}</td>
                                <td>${row.singleCell}</td>
                                <td>
                                    <ul class="list-group">
                                        <g:each in="${row.libraryPrepKits}" var="group">
                                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                                id: ${group.id}
                                                <span class="badge bg-primary rounded-pill text-light">${group.count}</span>
                                            </li>
                                        </g:each>
                                    </ul>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>

                    <p class="card-text"><g:message code="triggerAlignment.warn.libraryPrepKit.triggeringStartsOnlyOneGroup"/></p>
                </div>
            </div>
        </div>
    </g:if>
</div>
