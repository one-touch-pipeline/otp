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

<%@ page import="de.dkfz.tbi.otp.dataprocessing.MergingCriteria" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "mergingCriteria.title", args: [selectedProject.name])}</title>
    <asset:javascript src="common/CommentBox.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h1>${g.message(code: "mergingCriteria.title", args: [selectedProject.name])}</h1>

    <g:form action="update">
        <table>
            <tr>
                <th>${g.message(code: 'projectOverview.mergingCriteria.seqType')}</th>
                <th>${g.message(code: 'projectOverview.mergingCriteria.libPrepKit')}</th>
                <th>${g.message(code: 'projectOverview.mergingCriteria.seqPlatformGroup')}</th>
                <th></th>
            </tr>
            <tr>
                <td>
                    ${seqType}
                </td>
                <td>
                    <g:if test="${seqType.isExome()}">
                        true
                        <g:hiddenField name="useLibPrepKit" value="on"/>
                    </g:if>
                    <g:elseif test="${seqType.isWgbs()}">
                        false
                        %{-- no hidden field needed --}%
                    </g:elseif>
                    <g:else>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:checkBox name="useLibPrepKit" checked="${mergingCriteria.useLibPrepKit}" value="true" id="libPrepKit"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${mergingCriteria.useLibPrepKit}
                        </sec:ifNotGranted>
                    </g:else>
                </td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <g:select name="useSeqPlatformGroup" class="use-select-2" value="${mergingCriteria.useSeqPlatformGroup}"
                              from="${MergingCriteria.SpecificSeqPlatformGroups}"/>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${mergingCriteria.useSeqPlatformGroup}
                    </sec:ifNotGranted>

                </td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <g:hiddenField name="seqType.id" value="${seqType.id}"/>
                        <g:submitButton name="Submit"/>
                        <g:link controller="alignmentConfigurationOverview" class="btn">${g.message(code: "default.button.cancel.label")}</g:link>
                    </sec:ifAllGranted>
                </td>
            </tr>
        </table>
    </g:form>
    <br>



    <h2>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</h2>

    <g:if test="${mergingCriteria.useSeqPlatformGroup in [MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
                                                          MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC]}">
        <div style="float:left; width:50%;">
            <h3>${g.message(code: "mergingCriteria.seqPlatformDefinition.default")}</h3>
            <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <g:form action="copyAllDefaultToSpecific">
                        <g:hiddenField name="mergingCriteria.id" value="${mergingCriteria.id}"/>
                        <g:submitButton name="Copy all →" disabled="${dontAllowCopyingAll}" title="${dontAllowCopyingAll ?
                                "At least one of these groups contains a sequencing platform which is already in a specific group" :
                                "Copy all groups to specific groups"}"/>
                    </g:form>
                </sec:ifAllGranted>
            </g:if>
            <br>
            <g:each in="${seqPlatformGroups}" var="seqPlatformGroup">
                <div class="small-frame-box">
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
                            <g:form action="copyDefaultToSpecific">
                                <g:hiddenField name="mergingCriteria.id" value="${mergingCriteria.id}"/>
                                <g:hiddenField name="seqPlatformGroup.id" value="${seqPlatformGroup.id}"/>
                                <g:set var="alreadyUsed" value="${allUsedSpecificSeqPlatforms.intersect(seqPlatformGroup.seqPlatforms) as java.lang.Boolean}" />
                                <g:submitButton name="Copy →" style="float: right;" disabled="${alreadyUsed}"
                                                title="${alreadyUsed ?
                                                        "This group contains a sequencing platform which is already in a specific group" :
                                                        "Copy this group to specific groups"}"/>
                            </g:form>
                        </g:if>
                    </sec:ifAllGranted>
                    <ul>
                        <g:each in="${seqPlatformGroup.seqPlatforms.sort {it.fullName()}}" var="seqPlatform">
                            <li>${seqPlatform}</li>
                        </g:each>
                    </ul>
                </div>
            </g:each>
        </div>

        <div style="float:right; width:50%;">
            <g:if test="${mergingCriteria.useSeqPlatformGroup in [MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC]}">
                <h3>${g.message(code: "mergingCriteria.seqPlatformDefinition.specific")}</h3>
                <g:set var="platformFieldWidth" value="50ch"/>

                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
                        <div class="small-frame-box">
                            New group
                            <ul>
                                <li>
                                    <g:form action="createNewSpecificGroupAndAddPlatform">
                                        <g:select id="select_seqPlat_newgroup" name="platform.id" class="use-select-2" style="min-width: ${platformFieldWidth};"
                                                  from="${allSeqPlatformsWithoutGroup}" optionKey="id"
                                                  noSelection="${[null: 'Select to create new group']}"/>
                                        <g:hiddenField name="mergingCriteria.id" value="${mergingCriteria.id}"/>
                                            <g:submitButton name="Save"/>
                                    </g:form>
                                </li>
                            </ul>
                        </div>
                    </g:if>
                </sec:ifAllGranted>

                <g:each in="${seqPlatformGroupsPerProjectAndSeqType}" var="seqPlatformGroup">
                    <div class="small-frame-box">
                        <ul>
                            <g:each in="${seqPlatformGroup.seqPlatforms.sort {it.fullName()}}" var="seqPlatform">
                                <li>${seqPlatform}
                                <sec:ifAllGranted roles="ROLE_OPERATOR">
                                    <g:form action="removePlatformFromSeqPlatformGroup" style="display: inline;">
                                        <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                        <g:hiddenField name="platform.id" value="${seqPlatform.id}"/>
                                        <g:submitButton name="Remove"/>
                                    </g:form>
                                </sec:ifAllGranted>
                                </li>
                            </g:each>
                            <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
                                <sec:ifAllGranted roles="ROLE_OPERATOR">
                                    <li>
                                        <g:form action="addPlatformToExistingSeqPlatformGroup">
                                            <g:select id="select_seqPlat_${seqPlatformGroup.id}" name="platform.id" style="min-width: ${platformFieldWidth};"
                                                      class="use-select-2"
                                                      from="${allSeqPlatformsWithoutGroup}" optionKey="id"
                                                      noSelection="${[null: 'Select to add to group']}"/>
                                            <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                            <g:submitButton name="Save"/>
                                        </g:form>
                                    </li>
                                </sec:ifAllGranted>
                            </g:if>
                        </ul>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:form action="emptySeqPlatformGroup">
                                <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                <g:submitButton name="${g.message(code: "mergingCriteria.seqPlatformDefinition.emptyGroup")}"/>
                            </g:form>
                        </sec:ifAllGranted>
                    </div>
                </g:each>

            </g:if>

        </div>
    </g:if>
    <g:else>
        ${g.message(code: "mergingCriteria.seqPlatformDefinition.ignoreSeqPlatform")}
    </g:else>

</div>
</body>
</html>
