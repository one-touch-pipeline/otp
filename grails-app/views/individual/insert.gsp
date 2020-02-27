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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="individual.insert.title"/></title>
    <asset:javascript src="pages/individual/insert/functions.js"/>
    <asset:javascript src="common/MultiInputField.js"/>

</head>

<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <h1><g:message code="individual.insert.title"/></h1>
        <g:form controller="individual" action="save">
            <div>
                <table class="key-value-table key-input">
                    <tbody>
                    <tr>
                        <td>
                            <label for="identifier"><g:message code="individual.insert.identifier"/></label>
                        </td>
                        <td>
                            <input type="text" id="identifier" name="identifier" value="${cmd?.identifier}"/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="project"><g:message code="individual.insert.project"/></label>
                        </td>
                        <td>
                            <g:select name="project.id" from="${projects}" optionKey="id" id="project" noSelection="[null: '']" value="${cmd?.project?.id}"/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="alias"><g:message code="individual.insert.alias"/></label>
                        </td>
                        <td>
                            <input type="text" id="alias" name="alias" value="${cmd?.alias}"/>
                        </td>
                    </tr>
                    <tr>
                        <td valign="top">
                            <label for="displayedIdentifier"><g:message code="individual.insert.displayedIdentifier"/></label>
                        </td>
                        <td>
                            <input type="text" id="displayedIdentifier" name="displayedIdentifier" value="${cmd?.displayedIdentifier}"/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="internIdentifier"><g:message code="individual.insert.internIdentifier"/></label>
                        </td>
                        <td>
                            <input type="text" id="internIdentifier" name="internIdentifier" value="${cmd?.internIdentifier}"/>
                            <i><g:message code="otp.optional"/></i>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="individualType"><g:message code="individual.insert.individualType"/></label>
                        </td>
                        <td>
                            <g:select name="type" from="${individualTypes}" id="individualType" value="${cmd?.type}"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <br>

                <h3><g:message code="individual.insert.addSample"/></h3>
                <table class="key-value-table key-input">
                    <thead>
                    <tr class="hidden">
                        <th colspan="2">
                            <g:message code="individual.insert.newSample"/>
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr class="sample hidden">
                        <td>
                            <label for="sampleType"><g:message code="individual.insert.sampleType"/></label>
                        </td>
                        <td>
                            <g:select name="sampleType" from="${sampleTypes}" class="dropDown" noSelection="[null: '']"/>
                        </td>
                    </tr>
                    <tr class="sampleIdentifier hidden">
                        <td>
                            <label for="sampleIdentifier"><g:message code="individual.insert.sampleIdentifier"/></label>
                        </td>
                        <td class="multi-input-field">
                            <div class="field">
                                <g:textField list="sampleIdentifiersList" name="sampleIdentifiers"/>
                                <button type="button" class="add-field">+</button>
                            </div>
                        </td>
                    </tr>

                    <g:each in="${cmd?.samples ?: []}" var="sample" status="sampleCounter">
                        <table class="key-value-table">
                            <thead>
                            <tr>
                                <th colspan="2">
                                    <g:message code="individual.insert.newSample"/>
                                </th>
                            </tr>
                            </thead>
                            <tr class="sample">
                                <td>
                                    <label for="samples[${sampleCounter}].sampleType"><g:message code="individual.insert.sampleType"/></label>
                                </td>
                                <td>
                                    <g:select name="samples[${sampleCounter}].sampleType" from="${sampleTypes}" class="dropDown" noSelection="[null: '']"
                                              value="${sample.sampleType}"/>
                                </td>
                            </tr>
                            <tr class="sampleIdentifier">
                                <td>
                                    <label for="samples[${sampleCounter}].sampleIdentifier]"><g:message code="individual.insert.sampleIdentifier"/></label>
                                </td>
                                <td class="multi-input-field">
                                    <g:each in="${sample.sampleIdentifiers ?: [""]}" var="sampleIdentifier" status="i">
                                        <div class="field">
                                            <g:textField list="sampleIdentifiersList" name="samples[${sampleCounter}].sampleIdentifiers"
                                                         value="${sampleIdentifier}" id="sampleIdentifier"/>
                                            <g:if test="${i == 0}">
                                                <button type="button" class="add-field">+</button>
                                            </g:if>
                                            <g:else>
                                                <button type="button" class="remove-field">-</button>
                                            </g:else>
                                        </div>
                                    </g:each>
                                </td>
                            </tr>
                        </table>
                    </g:each>
                    </tbody>
                </table>
                <button type="button" class="add-button"><g:message code="individual.insert.newSampleButton"/></button>
                <input type="submit" value="${g.message(code: 'individual.insert.save')}"/>
                <g:message code="individual.insert.redirect"/><input type="checkbox" name="checkRedirect">
            </div>
        </g:form>
    </div>
</body>
</html>
