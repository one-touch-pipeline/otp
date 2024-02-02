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

%{--
Used for creating new seqPlatformGroups

parameter to set:
    mergingCriteria:                the current merging criteria to configure with this new SeqPlatformGroup
    allSeqPlatformsWithoutGroup:    all SeqPlatforms which have no group in the current merging criteria
    selectedProjectToCopyForm:      a search parameter to keep with every request
    selectedSeqTypeToCopyFrom:      a search parameter to keep with every request
--}%

<div class="card-header">
    <div class="row justify-content-between">
        <div class="col-8">
            <g:set var="disabled" value="${allSeqPlatformsWithoutGroup.empty}"/>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:form action="createNewGroupAndAddPlatform">
                    <g:hiddenField name="seqType.id" value="${mergingCriteria?.seqType?.id}"/>
                    <g:hiddenField name="mergingCriteria.id" value="${mergingCriteria?.id}"/>
                    <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
                    <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
                    <div class="input-group">
                        <g:select id="new_select_seqPlat"
                                  name="seqPlatform.id"
                                  class="use-select-2 form-control" autocomplete="off"
                                  from="${allSeqPlatformsWithoutGroup}" optionKey="id"
                                  noSelection="${[null: 'Select to add new group']}"
                                  disabled="${disabled}"/>
                        <div class="input-group-append">
                            <button class="btn btn-outline-primary select2-appended-btn" id="addSeqPlatformGroup" type="submit" disabled>
                                <i class="bi bi-plus"></i>
                            </button>
                        </div>
                    </div>
                </g:form>
            </sec:ifAllGranted>
        </div>
    </div>
</div>
