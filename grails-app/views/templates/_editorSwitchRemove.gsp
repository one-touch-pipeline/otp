%{--
  - Copyright 2011-2020 The OTP authors
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

<div class="edit-switch edit-switch-drop-down" name="${name}">
    <p class="edit-switch-editor" style="display: none">
        <input type="hidden" name="targetDelete" value="${link}"/>
        <input type="hidden" name="targetDeleteValue" value="${value}"/>
        <button class="delete" data-confirmation="${confirmation}"><g:message code="default.button.delete.label"/></button>
        <button class="cancel"><g:message code="default.button.cancel.label"/></button>
    </p>
    <p class="edit-switch-label">
        <span class="wordBreak">${value}</span>
        <button class="remove js-remove">&nbsp;</button>
    </p>
</div>