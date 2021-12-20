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

<div class="card">
    <div class="card-header">
        <i class="bi bi-search"></i> <g:message code="triggerAlignment.input.selection"/>
    </div>
    <div class="card-body">
        <div class="alert alert-primary" role="alert">
            <p class="card-text"><g:message code="triggerAlignment.input.cardTitle"/></p>
        </div>

        <g:render template="/templates/bootstrap/seqTrackSelectionTabBar/seqTrackSelectionTabBar" tabs="$tabs" model="[seqTypes: seqTypes]"/>

        <p class="card-text"></p>

        <ul class="list-group">
            <li class="list-group-item"><input type="checkbox" id="withdrawn"> <label for="withdrawn"><g:message code="triggerAlignment.input.checkbox.withdrawn"/></label></li>
            <li class="list-group-item"><input type="checkbox" id="ignoreSeqPlatform"> <label for="ignoreSeqPlatform"><g:message code="triggerAlignment.input.checkbox.ignoreSeqPlatform"/></label></li>
        </ul>

        <p class="card-text"></p>

        <button class="btn btn-primary"><g:message code="triggerAlignment.input.searchButton"/></button>
    </div>
</div>
