%{--
  - Copyright 2011-2026 The OTP authors
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
    <title><g:message code="qcThreshold.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">

    <div>
        <h1>${g.message(code: "qcThreshold.title")}</h1>

        <g:each in="${classesWithProperties}" var="cl">

            <h2 class="mt-4">${cl.clasz.simpleName}</h2>

            <table class="threshold-table table table-sm table-striped table-bordered mt-3">
                <thead class="align-middle">
                    <tr>
                        <th>${g.message(code: "qcThreshold.property")}</th>
                        <th>${g.message(code: "qcThreshold.seqType")}</th>
                        <th>${g.message(code: "qcThreshold.condition")}</th>
                        <th>${g.message(code: "qcThreshold.lowerError")}</th>
                        <th>${g.message(code: "qcThreshold.lowerWarn")}</th>
                        <th>${g.message(code: "qcThreshold.upperWarn")}</th>
                        <th>${g.message(code: "qcThreshold.upperError")}</th>
                        <th>${g.message(code: "qcThreshold.property2")}</th>
                        <g:if test="${cl.existingThresholds}">
                            <th></th>
                            <th></th>
                        </g:if>
                    </tr>
                </thead>
                <tbody>
                    <g:each in="${cl.existingThresholds}" var="threshold">

                        <tr class="edit-table-buttons">
                            <td>${threshold.qcProperty1}</td>
                            <td>${threshold.seqType?.displayNameWithLibraryLayout ?: "All sequencing types"}</td>
                            <g:form action="update">
                                <input type="hidden" name="qcThreshold.id" value="${threshold.id}"/>
                                <td>
                                    <span class="edit-fields d-none">
                                        %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                        <g:select id="" name="condition" class="threshold use-select-2"
                                                  from="${compare}" value="${threshold.compare}" optionValue="displayName" noSelection="['': 'Select']"/>
                                    </span>
                                    <span class="show-fields">
                                        ${threshold.compare.displayName}
                                    </span>
                                </td>
                                <td>
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="errorThresholdLower" class="threshold form-control edit-fields d-none" value="${threshold.errorThresholdLower}">
                                    <span class="show-fields">
                                        ${threshold.errorThresholdLower}
                                    </span>
                                </td>
                                <td>
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="warningThresholdLower" class="threshold form-control edit-fields d-none" value="${threshold.warningThresholdLower}">
                                    <span class="show-fields">
                                        ${threshold.warningThresholdLower}
                                    </span>
                                </td>
                                <td>
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="warningThresholdUpper" class="threshold form-control edit-fields d-none" value="${threshold.warningThresholdUpper}">
                                    <span class="show-fields">
                                        ${threshold.warningThresholdUpper}
                                    </span>
                                </td>
                                <td>
                                    %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                    <input id="" name="errorThresholdUpper" class="threshold form-control edit-fields d-none" value="${threshold.errorThresholdUpper}">
                                    <span class="show-fields">
                                        ${threshold.errorThresholdUpper}
                                    </span>
                                </td>
                                <td>
                                    <span class="edit-fields d-none">
                                        %{-- explicitly unset ID, so it doesn't default to "name", which would lead to duplicate IDs, and thus javascript pain --}%
                                        <g:select id="" name="property2" class="threshold use-select-2"
                                                  from="${cl.availableThresholdProperties}" value="${threshold.qcProperty2}" noSelection="['': '']"/>
                                    </span>
                                    <span class="show-fields">
                                        ${threshold.qcProperty2}
                                    </span>
                                </td>

                                <td>
                                    <button class="button-edit btn btn-sm btn-outline-primary">Edit</button>
                                    <g:submitButton class="save btn btn-sm btn-outline-success d-none me-1" name="Save" value="Save"/>
                                    <button class="cancel btn btn-sm btn-outline-secondary d-none">Cancel</button>
                                </td>
                            </g:form>
                            <td>
                                <g:form action="delete">
                                    <input type="hidden" name="qcThreshold.id" value="${threshold.id}"/>
                                    <button type="submit" class="btn btn-sm btn-outline-danger">${g.message(code: "default.button.delete.label")}</button>
                                </g:form>
                            </td>
                        </tr>
                    </g:each>

                    <g:form action="create">
                        <input type="hidden" name="className" value="${cl.clasz.simpleName}"/>

                        <tr class="add-table-fields d-none">
                            <td>
                                <g:select id="" name="property" class="threshold use-select-2"
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
                            <td><input name="errorThresholdLower" class="threshold form-control"></td>
                            <td><input name="warningThresholdLower" class="threshold form-control"></td>
                            <td><input name="warningThresholdUpper" class="threshold form-control"></td>
                            <td><input name="errorThresholdUpper" class="threshold form-control"></td>
                            <td>
                                <g:select id="" name="property2" class="threshold use-select-2"
                                          from="${cl.availableThresholdProperties}" noSelection="['': '']"/>
                            </td>
                            <g:if test="${cl.existingThresholds}">
                                <td></td>
                                <td></td>
                            </g:if>
                        </tr>

                        <tr class="add-buttons-row">
                            <td class="add-table-buttons" colspan="100">
                                <button class="add btn btn-outline-success">+</button>
                                <g:submitButton class="save btn btn-outline-success d-none" name="Save" value="Save"/>
                                <button class="cancel btn btn-outline-secondary d-none">Cancel</button>
                            </td>
                        </tr>
                    </g:form>
                </tbody>
            </table>

        </g:each>
    </div>
</div>

</body>
</html>
