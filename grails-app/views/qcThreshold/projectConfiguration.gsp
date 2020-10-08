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
    <title><g:message code="qcThreshold.title2" args="${[selectedProject.name]}"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/projectSelection"/>
        </div>
        <div class="grid-element comment-box">
            <g:render template="/templates/commentBox" model="[
                    commentable     : selectedProject,
                    targetController: 'projectConfig',
                    targetAction    : 'saveProjectComment',
            ]"/>
        </div>
    </div>
    <div>
        <g:render template="/projectConfig/tabMenu"/>
    </div>

    <div>
        <h1>${g.message(code: "qcThreshold.title2", args: [selectedProject.name])}</h1>
        <otp:annotation type="info">${g.message(code: "qcThreshold.noticeProject")}</otp:annotation>

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
                        <td>${v.property}</td>
                        <td>${v.seqType?.displayNameWithLibraryLayout ?: "All sequencing types"}</td>
                        <g:form action="update">
                            <input type="hidden" name="qcThreshold.id" value="${v.projectExistingThresholds?.id}"/>
                            <input type="hidden" name="forProject" value="true"/>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <g:select id="" name="condition" class="threshold use-select-2"
                                                  from="${compare}" value="${v.projectExistingThresholds?.compare}"
                                                  optionValue="displayName" noSelection="['': 'Select']"/>
                                    </span>
                                    <span class="show-fields">
                                        ${v.projectExistingThresholds?.compare?.displayName}
                                    </span>
                                </g:if>

                                <span class="defaultThreshold">
                                    <g:if test="${v.defaultExistingThresholds && v.projectExistingThresholds}">
                                        (${v.defaultExistingThresholds?.compare?.displayName})
                                    </g:if>
                                    <g:else>
                                        ${v.defaultExistingThresholds?.compare?.displayName}
                                    </g:else>
                                </span>
                            </td>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <input id="" name="errorThresholdLower" class="threshold" value="${v.projectExistingThresholds?.errorThresholdLower}">
                                    </span>
                                    <span class="show-fields">
                                        ${v.projectExistingThresholds?.errorThresholdLower}
                                    </span>
                                </g:if>

                                <span class="defaultThreshold">
                                    <g:if test="${v.defaultExistingThresholds?.errorThresholdLower && v.projectExistingThresholds}">
                                        (${v.defaultExistingThresholds?.errorThresholdLower})
                                    </g:if>
                                    <g:else>
                                        ${v.defaultExistingThresholds?.errorThresholdLower}
                                    </g:else>
                                </span>
                            </td>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <input id="" name="warningThresholdLower" class="threshold" value="${v.projectExistingThresholds?.warningThresholdLower}">
                                    </span>
                                    <span class="show-fields">
                                        ${v.projectExistingThresholds?.warningThresholdLower}
                                    </span>
                                </g:if>

                                <span class="defaultThreshold">
                                    <g:if test="${v.defaultExistingThresholds?.warningThresholdLower && v.projectExistingThresholds}">
                                        (${v.defaultExistingThresholds?.warningThresholdLower})
                                    </g:if>
                                    <g:else>
                                        ${v.defaultExistingThresholds?.warningThresholdLower}
                                    </g:else>
                                </span>
                            </td>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <input id="" name="warningThresholdUpper" class="threshold" value="${v.projectExistingThresholds?.warningThresholdUpper}">
                                    </span>
                                    <span class="show-fields">
                                        ${v.projectExistingThresholds?.warningThresholdUpper}
                                    </span>
                                </g:if>

                                <span class="defaultThreshold">
                                    <g:if test="${v.defaultExistingThresholds?.warningThresholdUpper && v.projectExistingThresholds}">
                                        (${v.defaultExistingThresholds?.warningThresholdUpper})
                                    </g:if>
                                    <g:else>
                                        ${v.defaultExistingThresholds?.warningThresholdUpper}
                                    </g:else>
                                </span>
                            </td>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <input id="" name="errorThresholdUpper" class="threshold" value="${v.projectExistingThresholds?.errorThresholdUpper}">
                                    </span>
                                    <span class="show-fields">
                                        ${v.projectExistingThresholds?.errorThresholdUpper}
                                    </span>
                                </g:if>

                                <span class="defaultThreshold">
                                    <g:if test="${v.defaultExistingThresholds?.errorThresholdUpper && v.projectExistingThresholds}">
                                        (${v.defaultExistingThresholds?.errorThresholdUpper})
                                    </g:if>
                                    <g:else>
                                        ${v.defaultExistingThresholds?.errorThresholdUpper}
                                    </g:else>
                                </span>
                            </td>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <g:select id="" name="property2" class="threshold use-select-2" style="min-width: ${propFieldWidth};"
                                                  from="${cl.availableThresholdProperties}"
                                                  value="${v.projectExistingThresholds?.qcProperty2}" noSelection="['': '']"/>
                                    </span>
                                    <span class="show-fields">
                                        ${v.projectExistingThresholds?.qcProperty2}
                                    </span>
                                </g:if>

                                <span class="defaultThreshold">
                                    <g:if test="${v.defaultExistingThresholds?.qcProperty2 && v.projectExistingThresholds}">
                                        (${v.defaultExistingThresholds?.qcProperty2})
                                    </g:if>
                                    <g:else>
                                        ${v.defaultExistingThresholds?.qcProperty2}
                                    </g:else>
                                </span>
                            </td>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <otp:editTableButtons/>
                                </g:if>
                            </td>
                        </g:form>

                        <td>
                            <g:if test="${v.projectExistingThresholds}">
                                <g:form action="delete">
                                    <input type="hidden" name="qcThreshold.id" value="${v.projectExistingThresholds?.id}"/>
                                    <input type="hidden" name="forProject" value="true"/>
                                    <g:submitButton name="Delete"/>
                                </g:form>
                            </g:if>
                        </td>
                    </otp:editTable>
                </g:each>

                <g:form action="create">
                    <input type="hidden" name="className" value="${cl.clasz.simpleName}"/>
                    <input type="hidden" name="forProject" value="true"/>
                    <otp:tableAdd>
                        <td>
                            <g:select id="" name="property" class="threshold use-select-2" style="min-width: ${propFieldWidth};"
                                      from="${cl.availableThresholdProperties}" noSelection="['': 'Select']"/>
                        </td>
                        <td>
                            <g:select id="" name="seqType.id" class="threshold use-select-2"
                                      from="${seqTypes}" optionKey="id" noSelection="['': 'Select']"/>
                        </td>
                        <td>
                            <g:select id="" name="condition" class="threshold use-select-2" from="${compare}" optionValue="displayName"
                                      noSelection="['': 'Select']"/>
                        </td>
                        <td><input id="" name="errorThresholdLower" class="threshold"></td>
                        <td><input id="" name="warningThresholdLower" class="threshold"></td>
                        <td><input id="" name="warningThresholdUpper" class="threshold"></td>
                        <td><input id="" name="errorThresholdUpper" class="threshold"></td>
                        <td>
                            <g:select id="" name="property2" class="threshold use-select-2" style="min-width: ${propFieldWidth};"
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
