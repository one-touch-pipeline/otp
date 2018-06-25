<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="qcThreshold.title2" args="${[project.name]}"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <div>
        <h2>${g.message(code: "qcThreshold.title2", args: [project.name])}</h2>
        ${g.message(code: "qcThreshold.notice")}<br>
        ${g.message(code: "qcThreshold.noticeProject")}
        <table>

            <g:each in="${classesWithProperties}" var="cl">
                <thead>
                <tr class="intermediateHeader"><td colspan="9"><h3>${cl.clasz.simpleName}</h3></td></tr>

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
                            <input type="hidden" name="project" value="true"/>
                            <td>
                                <g:if test="${v.projectExistingThresholds}">
                                    <span class="edit-fields" style="display: none;">
                                        <g:select class="threshold" name="condition" from="${compare}" value="${v.projectExistingThresholds?.compare}"
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
                                        <input class="threshold" name="errorThresholdLower" value="${v.projectExistingThresholds?.errorThresholdLower}" title="${g.message(code: "qcThreshold.lowerError")}">
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
                                        <input class="threshold" name="warningThresholdLower" value="${v.projectExistingThresholds?.warningThresholdLower}" title="${g.message(code: "qcThreshold.lowerWarn")}">
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
                                        <input class="threshold" name="warningThresholdUpper" value="${v.projectExistingThresholds?.warningThresholdUpper}" title="${g.message(code: "qcThreshold.upperWarn")}">
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
                                        <input class="threshold" name="errorThresholdUpper" value="${v.projectExistingThresholds?.errorThresholdUpper}" title="${g.message(code: "qcThreshold.upperError")}">
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
                                        <g:select class="threshold" name="property2" from="${cl.availableThresholdProperties}"
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
                                    <input type="hidden" name="project" value="true"/>
                                    <g:submitButton name="Delete"/>
                                </g:form>
                            </g:if>
                        </td>
                    </otp:editTable>
                </g:each>
                <g:form action="create">
                    <input type="hidden" name="className" value="${cl.clasz.simpleName}"/>
                    <input type="hidden" name="project.id" value="${project.id}"/>
                    <otp:tableAdd>
                        <td>
                            <g:select class="threshold" name="property" from="${cl.availableThresholdProperties}"
                                      noSelection="['': 'Select']"/>
                        </td>
                        <td>
                            <g:select class="threshold" name="seqType.id" from="${seqTypes}" optionKey="id" noSelection="['': 'All']"/>
                        </td>
                        <td>
                            <g:select class="threshold" name="condition" from="${compare}" optionValue="displayName"
                                      noSelection="['': 'Select']"/>
                        </td>
                        <td>
                            <input class="threshold" name="errorThresholdLower" title="${g.message(code: "qcThreshold.lowerError")}">
                        </td>
                        <td>
                            <input class="threshold" name="warningThresholdLower" title="${g.message(code: "qcThreshold.lowerWarn")}">
                        </td>
                        <td>
                            <input class="threshold" name="warningThresholdUpper" title="${g.message(code: "qcThreshold.upperWarn")}">
                        </td>
                        <td>
                            <input class="threshold" name="errorThresholdUpper" title="${g.message(code: "qcThreshold.upperError")}">
                        </td>
                        <td>
                            <g:select class="threshold" name="property2" from="${cl.availableThresholdProperties}"
                                      noSelection="['': '']"/>
                        </td>
                        <td>

                        </td>
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
