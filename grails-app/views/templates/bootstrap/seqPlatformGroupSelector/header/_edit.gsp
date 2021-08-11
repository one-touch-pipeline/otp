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

%{--
Used for configurable seqPlatformGroups of a merging criteria

parameter to set:
    mergingCriteria:                the current merging criteria to configure with this SeqPlatformGroup
    seqPlatformGroup:               the current SeqPlatformGroup to configure
    allSeqPlatformsWithoutGroup:    all SeqPlatforms which have no group in the current merging criteria
    selectedProjectToCopyForm:      a search parameter to keep with every request
    selectedSeqTypeToCopyFrom:      a search parameter to keep with every request
--}%

<div class="card-header">
    <div class="row justify-content-between">
        <div class="col-8">
            <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <g:form action="addPlatformToExistingSeqPlatformGroup">
                        <g:hiddenField name="seqType.id" value="${mergingCriteria?.seqType?.id}"/>
                        <g:hiddenField name="seqPlatformGroup.id" value="${seqPlatformGroup?.id}"/>
                        <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
                        <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
                        <div class="input-group">
                            <g:select id="current_select_seqPlat_${seqPlatformGroup.id}"
                                      name="seqPlatform.id"
                                      class="use-select-2 form-control" autocomplete="off"
                                      from="${allSeqPlatformsWithoutGroup}" optionKey="id"
                                      noSelection="${[null: 'Select to add to group']}"/>
                            <div class="input-group-append">
                                <button class="btn btn-outline-primary select2-appended-btn" type="submit">
                                    <i class="bi bi-plus"></i>
                                </button>
                            </div>
                        </div>
                    </g:form>
                </sec:ifAllGranted>
            </g:if>
        </div>

        <div class="col-1">
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:form action="emptySeqPlatformGroup">
                    <g:hiddenField name="seqType.id" value="${mergingCriteria?.seqType?.id}"/>
                    <g:hiddenField name="useDefaultToCopyFrom" value="${useDefaultToCopyFrom}"/>
                    <g:if test="${!useDefaultToCopyFrom}">
                        <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
                        <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
                    </g:if>
                    <g:hiddenField name="seqPlatformGroup.id" value="${seqPlatformGroup.id}"/>
                    <div class="row justify-content-end pr-3">
                        <button class="btn btn-danger" type="submit">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </g:form>
            </sec:ifAllGranted>
        </div>
    </div>
</div>