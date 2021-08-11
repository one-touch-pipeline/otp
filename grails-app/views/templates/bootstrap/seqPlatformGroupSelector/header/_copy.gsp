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
Used to display search results and copy them to the current merging criteria

parameter to set:
    mergingCriteria:                the current merging criteria to configure with this SeqPlatformGroup
    seqPlatformGroup:               the current SeqPlatformGroup to configure
    seqPlatformGroupAlreadyInUse:   a flag whether its allowed to copy this SeqPlatformGroup due to intersection
    selectedProjectToCopyForm:      a search parameter to keep with every request
    selectedSeqTypeToCopyFrom:      a search parameter to keep with every request
--}%

<div class="card-header">
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <g:form action="copySeqPlatformGroup">
            <g:hiddenField name="seqType.id" value="${mergingCriteria?.seqType?.id}"/>
            <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
            <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
            <g:hiddenField name="mergingCriteria.id" value="${mergingCriteria?.id}"/>
            <g:hiddenField name="seqPlatformGroup.id" value="${seqPlatformGroup?.id}"/>
            <div class="row justify-content-start pl-3">
                <button class="btn btn-outline-secondary" type="submit" ${(seqPlatformGroupAlreadyInUse) ? "disabled" : ""}>
                    <i class="bi bi-arrow-left"></i> <i class="bi bi-clipboard"></i>
                </button>
            </div>
        </g:form>
    </sec:ifAllGranted>
</div>