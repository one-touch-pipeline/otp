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

<div class="sample-identifiers-field">
    <p class="sample-identifiers-field-editor" style="display: none">
        <input type="hidden" name="target" value="${link}"/>
    </p>
    <table class="newSample">
        <tr class="sample">
            <td valign="top" class="value ${hasErrors(bean:individual,field:'sampleIdentifier','errors')}">
                <input type="text" name="sampleIdentifier" />
            </td>
            <td>
                <div class="newSampleIdentifier">
                    <button class="buttons plus"><g:message code="individual.insert.newSampleIdentifier"/></button>
                    <button class="buttons minus" style="display: none"><g:message code="individual.insert.removeSampleIdentifier"/></button>
                </div>
            </td>
        </tr>
    </table>
    <p class="inline-element"><span class="wordBreak">${value}</span><button class="edit">&nbsp;</button></p>
</div>
