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

<div class="accordion" id="warnAreaAccordion">
    <div class="card d-none" id="withdrawnSeqTracksWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapseFive" aria-expanded="true"
                    aria-controls="collapseFive">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.withdrawnWarnings.title"/>
            </button>
        </div>

        <div id="collapseFive" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.withdrawnWarnings"/></p>
                </div>

                <table id="withdrawnWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.individual"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.sampleType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.effectedLaneCount"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.triggeringWontBeStarted"/></p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="missingWorkflowConfigWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true"
                    aria-controls="collapseOne">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.missingWorkflowConfig.title"/>
            </button>
        </div>

        <div id="collapseOne" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.missingWorkflowConfig"/>
                        <g:link controller="workflowSelection">${g.message(code: 'triggerWorkflows.warn.config.link')}</g:link></p>
                </div>

                <table id="missingWorkflowConfigsWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.workflow"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.effectedLaneCount"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.triggeringWontBeStarted"/></p>

                <p class="card-text">
                    <g:message code="triggerWorkflows.warn.configPageReference"/>
                    <g:link controller="workflowSelection" action="index">
                        <g:message code="triggerWorkflows.warn.configPageReference.page"/>
                    </g:link>
                    <g:message code="triggerWorkflows.warn.configPageReference.note"/>
                </p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="missingReferenceGenomeWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapseFour" aria-expanded="true"
                    aria-controls="collapseFour">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.missingReferenceGenomeConfig.title"/>
            </button>
        </div>

        <div id="collapseFour" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.missingReferenceGenomeConfig"/>
                        <g:link controller="workflowSelection">${g.message(code: 'triggerWorkflows.warn.config.link')}</g:link></p>
                </div>

                <table id="missingReferenceGenomeWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.species"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.effectedLaneCount"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.triggeringWontBeStarted"/></p>

                <p class="card-text">
                    <g:message code="triggerWorkflows.warn.configPageReference"/>
                    <g:link controller="workflowSelection" action="index">
                        <g:message code="triggerWorkflows.warn.configPageReference.page"/>
                    </g:link>
                    <g:message code="triggerWorkflows.warn.configPageReference.note"/>
                </p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="missingSeqPlatformGroupsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-1" aria-expanded="true"
                    aria-controls="collapse-1">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.missingSeqPlatformGroups.title"/>
            </button>
        </div>

        <div id="collapse-1" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.missingSeqPlatformGroups"/>
                        <g:link controller="workflowSelection">${g.message(code: 'triggerWorkflows.warn.config.link')}</g:link></p>
                </div>

                <table id="missingSeqPlatformGroupsTable" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.individual"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.sampleType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqPlatforms"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.link"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.seqPlatform.triggeringStartsOnlyWhenSeqPlatformGroupsIgnored"/></p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="seqPlatformWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapseTwo" aria-expanded="true"
                    aria-controls="collapseTwo">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.mismatchedSeqPlatform.title"/>
            </button>
        </div>

        <div id="collapseTwo" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.mismatchedSeqPlatform"/>
                        <g:link controller="workflowSelection">${g.message(code: 'triggerWorkflows.warn.config.link')}</g:link></p>
                </div>

                <table id="seqPlatformWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.individual"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.sampleType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqPlatformGroupSubTable"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.seqPlatform.triggeringStartsOnlyOneGroup"/></p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="libraryPrepKitWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapseThree"
                    aria-expanded="true" aria-controls="collapseThree">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.mismatchedPreparationKit.title"/>
            </button>
        </div>

        <div id="collapseThree" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.mismatchedPreparationKit"/></p>
                </div>

                <table id="libraryPrepKitWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.individual"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.sampleType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.libraryPreparationKitSubTable"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.libraryPrepKit.triggeringStartsOnlyOneGroup"/></p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="missingLibraryPrepKitWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapseSix"
                    aria-expanded="true" aria-controls="collapseSix">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.missingPreparationKit.title"/>
            </button>
        </div>

        <div id="collapseSix" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.missingPreparationKit"/></p>
                </div>

                <table id="missingLibraryPrepKitWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.individual"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.seqType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.sampleType"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.lane"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.run"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>
            </div>
        </div>
    </div>

    <div class="card d-none" id="warningsForMissingSampleTypePerProjectCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-2"
                    aria-expanded="true" aria-controls="collapse-2">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerWorkflows.warn.missingSampleTypePerProjectCard.title"/>
            </button>
        </div>

        <div id="collapse-2" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerWorkflows.warn.missingSampleTypePerProjectCard"/>
                        <g:link controller="sampleCategory">${g.message(code: 'triggerWorkflows.warn.config.link')}</g:link></p>
                </div>

                <table id="warningsForMissingSampleTypePerProject" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerWorkflows.warn.table.project"/></th>
                        <th><g:message code="triggerWorkflows.warn.table.sampleType"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerWorkflows.warn.analysisWontBeStarted"/></p>
            </div>
        </div>
    </div>
</div>
