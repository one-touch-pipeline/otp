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

<div class="edit-switch edit-switch-multi-drop-down">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
        <g:select id="" name="dropdowns"
                  multiple="true"
                  optionKey="${optionKey}"
                  noSelection="${noSelection}"
                  from="${values}"
                  value="${value}"
                  class="use-select-2"/>
        <button class="save" data-confirmation="${confirmation}" data-success-handler="${sucessHandler}"><g:message code="default.button.update.label"/></button>
        <button class="cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label">
        <span class="wordBreak">
            <otp:getValueWithFallbacks value="${value?.join(', ')}" optionValue="${optionValue}" noSelection="${noSelection}"/>
        </span>
        <button class="edit js-edit">&nbsp;</button>
    </p>
</div>
