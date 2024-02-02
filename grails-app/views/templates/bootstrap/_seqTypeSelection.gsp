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
  - Generic seqType selection.
  -
  - Required params:
  - seqTypes, as list of SeqTypes
  -
  - Usage example: <g:render template="/templates/bootstrap/seqTypeSelection" model="[seqTypes: seqTypes]"/>
  --}%
<div class="input-group mb-3">
    <div class="input-group-prepend">
        <label class="input-group-text"><g:message code="seqTypeSelection.seqType"/></label>
    </div>
    <select class="custom-select use-select-2" multiple="multiple" id="${id}">
        <g:each in="${seqTypes}" var="seqType">
            <option value="${seqType.id}">${seqType.displayNameWithLibraryLayout}</option>
        </g:each>
    </select>
</div>
