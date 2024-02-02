%{--
  - Copyright 2011-2024 The OTP authors
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

<%@ page import="de.dkfz.tbi.otp.ngsdata.SampleType" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processingThresholds.title" args="${[selectedProject.name]}"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/processingThreshold/index/configureThresholds.js"/>
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

    <h1><g:message code="processingThresholds.title" args="${[selectedProject.name]}"/></h1>
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <g:if test="${!edit}">
            <g:link action="index" params="[edit: true]" class="btn">${g.message(code: "processingThresholds.edit")}</g:link>
        </g:if>
    </sec:ifAllGranted>
    <div>
        <g:form class="confirm" action="update">
            <table>
                <tr>
                    <th colspan="2"></th>
                    <g:each var="seqType" in="${seqTypes}">
                        <th colspan="2">${seqType}</th>
                    </g:each>
                </tr>
                <tr>
                    <th><g:message code="processingThresholds.sampleTypes"/></th>
                    <th><g:message code="processingThresholds.category"/></th>
                    <g:each var="seqType" in="${seqTypes}">
                        <th><g:message code="processingThresholds.minLanes"/></th>
                        <th><g:message code="processingThresholds.minCoverage"/></th>
                    </g:each>
                </tr>
                <g:each var="sampleType" in="${sampleTypes}" status="i">
                    <tr>
                        <td>${sampleType.name}</td>
                        <td>
                            <input type="hidden" name="sampleTypes[${i}].sampleType.id" value="${sampleType.id}"/>
                            <g:if test="${edit}">
                                <g:select name="sampleTypes[${i}].category" class="use-select-2" from='${categories}'
                                          value='${groupedCategories[sampleType] ?: de.dkfz.tbi.otp.ngsdata.SampleTypePerProject.Category.UNDEFINED}'
                                          autocomplete="off"/>
                            </g:if>
                            <g:else>${groupedCategories[sampleType] ?: de.dkfz.tbi.otp.ngsdata.SampleTypePerProject.Category.UNDEFINED}</g:else>
                        </td>
                        <g:each var="seqType" in="${seqTypes}" status="j">
                            <input type="hidden" name="sampleTypes[${i}].seqTypes[${j}].seqType.id" value="${seqType.id}"/>
                            <td>
                                <g:if test="${edit}">
                                    <input type="number" min="0" name="sampleTypes[${i}].seqTypes[${j}].minNumberOfLanes"
                                           value="${groupedThresholds[sampleType]?.get(seqType)?.numberOfLanes}"
                                           autocomplete="off"/>
                                </g:if>
                                <g:else>${groupedThresholds[sampleType]?.get(seqType)?.numberOfLanes}</g:else>
                            </td>
                            <td>
                                <g:if test="${edit}">
                                    <input type="text" pattern="([0-9]+(\.[0-9]+)?)?" inputmode="numeric" name="sampleTypes[${i}].seqTypes[${j}].minCoverage"
                                           value="${groupedThresholds[sampleType]?.get(seqType)?.coverage}"
                                           autocomplete="off"/>
                                </g:if>
                                <g:else>${groupedThresholds[sampleType]?.get(seqType)?.coverage}</g:else>
                            </td>
                        </g:each>
                    </tr>
                </g:each>
            </table>

            <g:if test="${edit}">
                <otp:annotation type="info">
                    <g:message code="processingThresholds.note"/>
                </otp:annotation>
                <g:submitButton name="submit" value="${g.message(code: "processingThresholds.submit")}"/>
                <g:link class="btn" action="index">${g.message(code: "processingThresholds.cancel")}</g:link>
            </g:if>
        </g:form>
    </div>
</div>
</body>
</html>
