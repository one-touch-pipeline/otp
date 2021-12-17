%{--
  - Copyright 2011-2019 The OTP authors
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

<div class="edit-switch edit-switch-new-free-text-values" style="display:inline-block">
    <span class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:set var="i" value="${0}"/>
        <g:each var="field" in="${textFields}">
            <label>${labels[i++]}:
                <input type="text" name="${field}"/>
            </label>
        </g:each>
        <g:each var="checkBox" in="${checkBoxes}">
            <label>${labels[i++]}:
                <g:checkBox name="${checkBox.key}" checked="${checkBox.value}" value="true"/>
            </label>
        </g:each>
        <g:each var="dropDown" in="${dropDowns}">
            <label>${labels[i++]}:
                <g:select id="" name="${dropDown.key}" from="${dropDown.value}" class="use-select-2"/>
            </label>
        </g:each>
        <button class="buttons save"><g:message code="default.button.save.label"/></button>
        <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
    </span>
    <span class="edit-switch-label"><button class="add js-edit"><g:message code="default.new"/></button>
    </span>
</div>
