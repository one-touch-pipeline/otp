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
    <meta name="layout" content="main"/>
    <title><g:message code="qcThreshold.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <div>
        <h1>${g.message(code: "qcThreshold.title")}</h1>
        <table class="threshold-table">
            <g:set var="propFieldWidth" value="40ch"/>

            <g:each in="${classesWithProperties}" var="cl">
                <thead>
                <tr class="intermediateHeader"><td colspan="9"><h2>${cl.clasz.simpleName}</h2></td></tr>

                <tr>
                    <th>${g.message(code: "qcThreshold.property")}</th>
                    <th>${g.message(code: "qcThreshold.seqType")}</th>
                    <th>${g.message(code: "qcThreshold.condition")}</th>
                    <th>${g.message(code: "qcThreshold.lowerError")}</th>
                    <th>${g.message(code: "qcThreshold.lowerWarn")}</th>
                    <th>${g.message(code: "qcThreshold.upperWarn")}</th>
                    <th>${g.message(code: "qcThreshold.upperError")}</th>
                    <th>${g.message(code: "qcThreshold.property2")}</th>
                    <th></th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${cl.existingThresholds}" var="v">

                    <otp:editTable>

                        <td>${v.qcProperty1}</td>
                        <td>${v.seqType?.displayNameWithLibraryLayout ?: "All sequencing types"}</td>
                        <g:form action="update">
                            <input type="hidden" name="qcThreshold.id" value="${v.id}"/>
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <g:select id="" name="condition" class="threshold use-select-2"
                                              from="${compare}" value="${v.compare}" optionValue="displayName" noSelection="['': 'Select']"/>
                                </span>
                                <span class="show-fields">
                                    ${v.compare.displayName}
                                </span>
                            </td>
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="errorThresholdLower" class="threshold" value="${v.errorThresholdLower}">
                                </span>
                                <span class="show-fields">
                                    ${v.errorThresholdLower}
                                </span>
                            </td>
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="warningThresholdLower" class="threshold" value="${v.warningThresholdLower}">
                                </span>
                                <span class="show-fields">
                                    ${v.warningThresholdLower}
                                </span>
                            </td>
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="warningThresholdUpper" class="threshold" value="${v.warningThresholdUpper}">
                                </span>
                                <span class="show-fields">
                                    ${v.warningThresholdUpper}
                                </span>
                            </td>
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="errorThresholdUpper" class="threshold" value="${v.errorThresholdUpper}">
                                </span>
                                <span class="show-fields">
                                    ${v.errorThresholdUpper}
                                </span>
                            </td>
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <g:select id="" name="property2" class="threshold use-select-2" style="min-width: ${propFieldWidth}"
                                              from="${cl.availableThresholdProperties}" value="${v.qcProperty2}" noSelection="['': '']"/>
                                </span>
                                <span class="show-fields">
                                    ${v.qcProperty2}
                                </span>
                            </td>
                            <td>
                                <otp:editTableButtons/>
                            </td>
                        </g:form>
                        <td>
                            <g:form action="delete">
                                <input type="hidden" name="qcThreshold.id" value="${v.id}"/>
                                <g:submitButton name="Delete"/>
                            </g:form>
                        </td>
                    </otp:editTable>
                </g:each>


                <g:form action="create">
                    <input type="hidden" name="className" value="${cl.clasz.simpleName}"/>
                    <otp:tableAdd>
                        <td>
                            <g:select id="" name="property" class="threshold use-select-2" style="min-width: ${propFieldWidth}"
                                      from="${cl.availableThresholdProperties}" noSelection="['': 'Select']"/>
                        </td>
                        <td>
                            <g:select id="" name="seqType.id" class="threshold use-select-2"
                                      from="${seqTypes}" optionKey="id" noSelection="['': 'Select']"/>
                        </td>
                        <td>
                            <g:select id="" name="condition" class="threshold use-select-2"
                                      from="${compare}" optionValue="displayName" noSelection="['': 'Select']"/>
                        </td>
                        <td><input name="errorThresholdLower" class="threshold"></td>
                        <td><input name="warningThresholdLower" class="threshold"></td>
                        <td><input name="warningThresholdUpper" class="threshold"></td>
                        <td><input name="errorThresholdUpper" class="threshold"></td>
                        <td>
                            <g:select id="" name="property2" class="threshold use-select-2" style="min-width: ${propFieldWidth}"
                                      from="${cl.availableThresholdProperties}" noSelection="['': '']"/>
                        </td>
                        <td></td>
                        <td></td>
                    </otp:tableAdd>
                </g:form>
                </tbody>
            </g:each>
        </table>
    </div>
</div>

</body>
</html>
