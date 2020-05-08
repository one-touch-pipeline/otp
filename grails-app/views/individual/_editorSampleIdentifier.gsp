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
    <g:form controller="individual" action="editNewSampleIdentifier" class="sample-identifier-edit-form">
        <input type="hidden" name="sample.id" value="${sample.id}"/>
        <table>
            <tr>
                <td>
                    <strong><g:message code="individual.show.existingSamples"/></strong>
                </td>
                <td>
                    <strong><g:message code="document.delete"/></strong>
                </td>
            </tr>
        <g:each in="${sample.sampleIdentifierObjects}" var="sampleIdentifier" status="i">
            <tr>
                <td>
                    <input type="text" name="editedSampleIdentifiers[${i}].name" value="${sampleIdentifier.name}"/>
                    <input type="hidden" name="editedSampleIdentifiers[${i}].sampleIdentifier" value="${sampleIdentifier.id}"/>
                </td>
                <td>
                    <input type="checkbox" name="editedSampleIdentifiers[${i}].delete"/>
                </td>
            </tr>
        </g:each>
            %{-- workaround:
             The data binding does not work properly with single entry lists and setters.
             For a single entry it expects a setter with a String parameter, while it expects a setter with a List<String> parameter
             for more entries. You can not add both setters.
             With this workaround we always add a field to have at least two entries. We filter out empty entries anyways.
             --}%
            <input type="hidden" name="newIdentifiersNames" value=""/>
            <tr style="background-color: white">
                <td colspan="2"><hr></td>
                <td></td>
            </tr>
            <tr>
                <td>
                    <strong><g:message code="individual.show.createSample"/></strong>
                </td>
            </tr>
            <tr>
                <td class="multi-input-field">
                    <g:each in="${cmd?.samples ?: [""]}" var="sampleIdentifiers" status="i">
                        <div class="field">
                            <g:textField name="newIdentifiersNames" value="${sampleIdentifiers}"/>
                            <g:if test="${i == 0}">
                                <button type="button" class="add-field">+</button>
                            </g:if>
                            <g:else>
                                <button type="button" class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                </td>
                <td></td>
            </tr>
            <tr>
                <td>
                    <g:submitButton name="update" value="Update"/>
                    <button type="reset" id="cancel-button">Reset</button>
                </td>
                <td></td>
            </tr>
        </table>
    </g:form>
</div>