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
parameter to set:
    mergingCriteria (optional):                the current merging criteria to configure with this SeqPlatformGroup, only need if editable=true
    seqPlatformGroup:                          the current SeqPlatformGroup to configure
    editable (optional):                       indicates if this seqPlatformGroup is editable, if not set its false
    selectedProjectToCopyForm (optional):      a search parameter to keep with every request
    selectedSeqTypeToCopyFrom (optional):      a search parameter to keep with every request
--}%

<div class="list-group list-group-flush">
    <g:each in="${seqPlatformGroup.seqPlatforms.sort { it.fullName }}" var="seqPlatform">
        <div class="list-group-item">
            <div class="row">
                <div class="col-10">
                    ${seqPlatform}
                </div>

                <div class="col-2">
                    <g:if test="${editable}">
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:form action="removePlatformFromSeqPlatformGroup">
                                <g:hiddenField name="seqType.id" value="${mergingCriteria?.seqType?.id}"/>
                                <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
                                <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
                                <g:hiddenField name="seqPlatformGroup.id" value="${seqPlatformGroup?.id}"/>
                                <g:hiddenField name="seqPlatform.id" value="${seqPlatform?.id}"/>

                                <button type="submit" class="close" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>

                            </g:form>
                        </sec:ifAllGranted>
                    </g:if>
                </div>
            </div>
        </div>
    </g:each>
</div>
