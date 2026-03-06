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
    <title><g:message code="qcThreshold.title2" args="${[selectedProject.name]}"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/bootstrap/projectSelection"/>
        </div>
        <div class="grid-element comment-box">
            <g:render template="/templates/commentBox" model="[
                    commentable     : selectedProject,
                    targetController: 'projectConfig',
                    targetAction    : 'saveProjectComment',
            ]"/>
        </div>
    </div>
    <div class="mb-4">
        <g:render template="/projectConfig/tabMenu"/>
    </div>

    <div>
        <h1>${g.message(code: "qcThreshold.title2", args: [selectedProject.name])}</h1>

        <otp:annotation type="info">${g.message(code: "qcThreshold.noticeProject")}</otp:annotation>


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
                        <g:if test="${cl.existingThresholds.any{ it.projectExistingThresholds }}">
                            <th></th>
                            <th></th>
                        </g:if>
                    </tr>
                </thead>

                <tbody>
                    <g:each in="${cl.existingThresholds}" var="threshold">
                        <tr class="edit-table-buttons">
                            <td>${threshold.property}</td>
                            <td>${threshold.seqType?.displayNameWithLibraryLayout ?: "All sequencing types"}</td>
                            <g:form action="update">
                                <input type="hidden" name="qcThreshold.id" value="${threshold.projectExistingThresholds?.id}"/>
                                <input type="hidden" name="forProject" value="true"/>
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <span class="edit-fields d-none">
                                            <g:select id="" name="condition" class="threshold use-select-2"
                                                      from="${compare}" value="${threshold.projectExistingThresholds?.compare}"
                                                      optionValue="displayName" noSelection="['': 'Select']"/>
                                        </span>
                                        <span class="show-fields">
                                            ${threshold.projectExistingThresholds?.compare?.displayName}
                                        </span>
                                    </g:if>

                                    <span class="defaultThreshold">
                                        <g:if test="${threshold.defaultExistingThresholds && threshold.projectExistingThresholds}">
                                            (${threshold.defaultExistingThresholds?.compare?.displayName})
                                        </g:if>
                                        <g:else>
                                            ${threshold.defaultExistingThresholds?.compare?.displayName}
                                        </g:else>
                                    </span>
                                </td>
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <input id="" name="errorThresholdLower" class="threshold form-control edit-fields d-none" value="${threshold.projectExistingThresholds?.errorThresholdLower}">
                                        <span class="show-fields">
                                            ${threshold.projectExistingThresholds?.errorThresholdLower}
                                        </span>
                                    </g:if>

                                    <span class="defaultThreshold">
                                        <g:if test="${threshold.defaultExistingThresholds?.errorThresholdLower && threshold.projectExistingThresholds}">
                                            (${threshold.defaultExistingThresholds?.errorThresholdLower})
                                        </g:if>
                                        <g:else>
                                            ${threshold.defaultExistingThresholds?.errorThresholdLower}
                                        </g:else>
                                    </span>
                                </td>
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <input id="" name="warningThresholdLower" class="threshold form-control edit-fields d-none" value="${threshold.projectExistingThresholds?.warningThresholdLower}">
                                        <span class="show-fields">
                                            ${threshold.projectExistingThresholds?.warningThresholdLower}
                                        </span>
                                    </g:if>

                                    <span class="defaultThreshold">
                                        <g:if test="${threshold.defaultExistingThresholds?.warningThresholdLower && threshold.projectExistingThresholds}">
                                            (${threshold.defaultExistingThresholds?.warningThresholdLower})
                                        </g:if>
                                        <g:else>
                                            ${threshold.defaultExistingThresholds?.warningThresholdLower}
                                        </g:else>
                                    </span>
                                </td>
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <input id="" name="warningThresholdUpper" class="threshold form-control edit-fields d-none" value="${threshold.projectExistingThresholds?.warningThresholdUpper}">
                                        <span class="show-fields">
                                            ${threshold.projectExistingThresholds?.warningThresholdUpper}
                                        </span>
                                    </g:if>

                                    <span class="defaultThreshold">
                                        <g:if test="${threshold.defaultExistingThresholds?.warningThresholdUpper && threshold.projectExistingThresholds}">
                                            (${threshold.defaultExistingThresholds?.warningThresholdUpper})
                                        </g:if>
                                        <g:else>
                                            ${threshold.defaultExistingThresholds?.warningThresholdUpper}
                                        </g:else>
                                    </span>
                                </td>
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <input id="" name="errorThresholdUpper" class="threshold form-control edit-fields d-none" value="${threshold.projectExistingThresholds?.errorThresholdUpper}">
                                        <span class="show-fields">
                                            ${threshold.projectExistingThresholds?.errorThresholdUpper}
                                        </span>
                                    </g:if>

                                    <span class="defaultThreshold">
                                        <g:if test="${threshold.defaultExistingThresholds?.errorThresholdUpper && threshold.projectExistingThresholds}">
                                            (${threshold.defaultExistingThresholds?.errorThresholdUpper})
                                        </g:if>
                                        <g:else>
                                            ${threshold.defaultExistingThresholds?.errorThresholdUpper}
                                        </g:else>
                                    </span>
                                </td>
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <span class="edit-fields d-none">
                                            <g:select id="" name="property2" class="threshold use-select-2 edit-fields d-none"
                                                      from="${cl.availableThresholdProperties}"
                                                      value="${threshold.projectExistingThresholds?.qcProperty2}" noSelection="['': '']"/>
                                        </span>
                                        <span class="show-fields">
                                            ${threshold.projectExistingThresholds?.qcProperty2}
                                        </span>
                                    </g:if>

                                    <span class="defaultThreshold">
                                        <g:if test="${threshold.defaultExistingThresholds?.qcProperty2 && threshold.projectExistingThresholds}">
                                            (${threshold.defaultExistingThresholds?.qcProperty2})
                                        </g:if>
                                        <g:else>
                                            ${threshold.defaultExistingThresholds?.qcProperty2}
                                        </g:else>
                                    </span>
                                </td>
                                <g:if test="${cl.existingThresholds.any{ it.projectExistingThresholds }}">
                                    <td>
                                        <g:if test="${threshold.projectExistingThresholds}">
                                            <button class="button-edit btn btn-sm btn-outline-primary">Edit</button>
                                            <g:submitButton class="save btn btn-sm btn-outline-success d-none me-1" name="Save" value="Save"/>
                                            <button class="cancel btn btn-sm btn-outline-secondary d-none">Cancel</button>
                                        </g:if>
                                    </td>
                                </g:if>
                            </g:form>

                            <g:if test="${cl.existingThresholds.any{ it.projectExistingThresholds }}">
                                <td>
                                    <g:if test="${threshold.projectExistingThresholds}">
                                        <g:form action="delete">
                                            <input type="hidden" name="qcThreshold.id" value="${threshold.projectExistingThresholds?.id}"/>
                                            <input type="hidden" name="forProject" value="true"/>
                                            <button type="submit" class="btn btn-sm btn-outline-danger">${g.message(code: "default.button.delete.label")}</button>
                                        </g:form>
                                    </g:if>
                                </td>
                            </g:if>
                        </tr>
                    </g:each>

                    <g:form action="create">
                        <input type="hidden" name="className" value="${cl.clasz.simpleName}"/>
                        <input type="hidden" name="forProject" value="true"/>
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
                                <g:select id="" name="condition" class="threshold use-select-2" from="${compare}" optionValue="displayName"
                                          noSelection="['': 'Select']"/>
                            </td>
                            <td><input name="errorThresholdLower" class="threshold form-control"></td>
                            <td><input name="warningThresholdLower" class="threshold form-control"></td>
                            <td><input name="warningThresholdUpper" class="threshold form-control"></td>
                            <td><input name="errorThresholdUpper" class="threshold form-control"></td>
                            <td>
                                <g:select id="" name="property2" class="threshold use-select-2"
                                          from="${cl.availableThresholdProperties}" noSelection="['': '']"/>
                            </td>
                            <g:if test="${cl.existingThresholds.any{ it.projectExistingThresholds }}">
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
