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

<div class="accordion" id="warnAreaAccordion">
    <div class="card d-none" id="withdrawnSeqTracksWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseFive" aria-expanded="true"
                    aria-controls="collapseFive">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.withdrawnWarnings.title"/>
            </button>
        </div>

        <div id="collapseFive" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerAlignment.warn.withdrawnWarnings"/></p>
                </div>

                <table id="withdrawnWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerAlignment.warn.table.project"/></th>
                        <th><g:message code="triggerAlignment.warn.table.individual"/></th>
                        <th><g:message code="triggerAlignment.warn.table.seqType"/></th>
                        <th><g:message code="triggerAlignment.warn.table.sampleTypeName"/></th>
                        <th><g:message code="triggerAlignment.warn.table.effectedLaneCount"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerAlignment.warn.triggeringWontBeStarted"/></p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="missingAlignmentConfigWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseOne" aria-expanded="true"
                    aria-controls="collapseOne">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.missingAlignmentConfig.title"/>
            </button>
        </div>

        <div id="collapseOne" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerAlignment.warn.missingAlignmentConfig"/></p>
                </div>

                <table id="missingAlignmentConfigsWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerAlignment.warn.table.project"/></th>
                        <th><g:message code="triggerAlignment.warn.table.seqType"/></th>
                        <th><g:message code="triggerAlignment.warn.table.effectedLaneCount"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerAlignment.warn.triggeringWontBeStarted"/></p>

                <p class="card-text">
                    <g:message code="triggerAlignment.warn.configPageReference"/>
                    <g:link controller="workflowSelection" action="index">
                        <g:message code="triggerAlignment.warn.configPageReference.page"/>
                    </g:link>
                    <g:message code="triggerAlignment.warn.configPageReference.note"/>
                </p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="missingReferenceGenomeWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseFour" aria-expanded="true"
                    aria-controls="collapseFour">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.missingReferenceGenomeConfig.title"/>
            </button>
        </div>

        <div id="collapseFour" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerAlignment.warn.missingReferenceGenomeConfig"/></p>
                </div>

                <table id="missingReferenceGenomeWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerAlignment.warn.table.project"/></th>
                        <th><g:message code="triggerAlignment.warn.table.seqType"/></th>
                        <th><g:message code="triggerAlignment.warn.table.species"/></th>
                        <th><g:message code="triggerAlignment.warn.table.effectedLaneCount"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerAlignment.warn.triggeringWontBeStarted"/></p>

                <p class="card-text">
                    <g:message code="triggerAlignment.warn.configPageReference"/>
                    <g:link controller="workflowSelection" action="index">
                        <g:message code="triggerAlignment.warn.configPageReference.page"/>
                    </g:link>
                    <g:message code="triggerAlignment.warn.configPageReference.note"/>
                </p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="seqPlatformWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="true"
                    aria-controls="collapseTwo">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.mismatchedSeqPlatform.title"/>
            </button>
        </div>

        <div id="collapseTwo" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerAlignment.warn.mismatchedSeqPlatform"/></p>
                </div>

                <table id="seqPlatformWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerAlignment.warn.table.project"/></th>
                        <th><g:message code="triggerAlignment.warn.table.individual"/></th>
                        <th><g:message code="triggerAlignment.warn.table.seqType"/></th>
                        <th><g:message code="triggerAlignment.warn.table.sampleTypeName"/></th>
                        <th><g:message code="triggerAlignment.warn.table.seqPlatformGroupSubTable"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerAlignment.warn.seqPlatform.triggeringStartsOnlyOneGroup"/></p>
            </div>
        </div>
    </div>

    <div class="card d-none" id="libraryPrepKitWarningsCard">
        <div class="card-header">
            <button class="btn btn-link w-100 text-left collapsed p-0" type="button" data-toggle="collapse" data-target="#collapseThree"
                    aria-expanded="true" aria-controls="collapseThree">
                <i class="bi bi-exclamation-triangle"></i> <g:message code="triggerAlignment.warn.mismatchedPreparationKit.title"/>
            </button>
        </div>

        <div id="collapseThree" class="collapse" data-parent="#warnAreaAccordion">
            <div class="card-body">
                <div class="alert alert-warning" role="alert">
                    <p class="card-text"><g:message code="triggerAlignment.warn.mismatchedPreparationKit"/></p>
                </div>

                <table id="libraryPrepKitWarnings" class="table table-sm table-striped table-hover table-bordered">
                    <thead>
                    <tr>
                        <th><g:message code="triggerAlignment.warn.table.project"/></th>
                        <th><g:message code="triggerAlignment.warn.table.individual"/></th>
                        <th><g:message code="triggerAlignment.warn.table.seqType"/></th>
                        <th><g:message code="triggerAlignment.warn.table.sampleTypeName"/></th>
                        <th><g:message code="triggerAlignment.warn.table.libraryPreparationKitSubTable"/></th>
                    </tr>
                    </thead>
                    <tbody/>
                </table>

                <p class="card-text"><g:message code="triggerAlignment.warn.libraryPrepKit.triggeringStartsOnlyOneGroup"/></p>
            </div>
        </div>
    </div>

</div>
