<%@ page import="de.dkfz.tbi.otp.dataprocessing.MergingCriteria" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "mergingCriteria.title", args: [project.name])}</title>
</head>

<body>
<div class="body">
    <g:if test="${flash.message}">
        <div id="infoBox"><div class="message">
            <p>
                ${flash.message}<br>
                ${flash.errors}
            </p>
            <div class="close"><button onclick="$(this).parent().parent().remove();"></button></div>
            <div style="clear: both;"></div>
        </div></div>
    </g:if>

    <g:link controller="projectConfig"> Back to project configuration</g:link>
    <h2>${g.message(code: "mergingCriteria.title", args: [project.name])}</h2>

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
                        <g:hiddenField name="libPrepKit" value="on"/>
                    </g:if>
                    <g:elseif test="${seqType.isWgbs()}">
                        false
                        %{-- no hidden field needed --}%
                    </g:elseif>
                    <g:else>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:checkBox name="libPrepKit" value="${mergingCriteria.libPrepKit}" id="libPrepKit"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${mergingCriteria.libPrepKit}
                        </sec:ifNotGranted>
                    </g:else>
                </td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <g:select name="seqPlatformGroup" value="${mergingCriteria.seqPlatformGroup}"
                              from="${MergingCriteria.SpecificSeqPlatformGroups}"/>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${mergingCriteria.seqPlatformGroup}
                    </sec:ifNotGranted>

                </td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <g:hiddenField name="project.id" value="${project.id}"/>
                        <g:hiddenField name="seqType.id" value="${seqType.id}"/>
                        <g:submitButton name="Submit"/>
                    </sec:ifAllGranted>
                </td>
            </tr>
        </table>
    </g:form>
    <br>



    <h3>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</h3>

    <g:if test="${mergingCriteria.seqPlatformGroup in [MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
                                                       MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC]}">
        <div style="float:left; width:50%;">
            <h4>${g.message(code: "mergingCriteria.seqPlatformDefinition.default")}</h4>
            <g:if test="${mergingCriteria.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
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
                        <g:if test="${mergingCriteria.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
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
            <g:if test="${mergingCriteria.seqPlatformGroup in [MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC]}">
                <h4>${g.message(code: "mergingCriteria.seqPlatformDefinition.specific")}</h4>

                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
                        <div class="small-frame-box">
                            New group
                            <ul>
                                <li>
                                    <g:form action="createNewSpecificGroupAndAddPlatform">
                                        <g:select name="platform.id" from="${allSeqPlatformsWithoutGroup}" optionKey="id" noSelection="${[null: 'Select to create new group']}"/>
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
                                            <g:select name="platform.id" from="${allSeqPlatformsWithoutGroup}" optionKey="id" noSelection="${[null: 'Select to add to group']}"/>
                                            <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                            <g:submitButton name="Save"/>
                                        </g:form>
                                    </li>
                                </sec:ifAllGranted>
                            </g:if>
                        </ul>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:form action="deleteSeqPlatformGroup">
                                <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                <g:submitButton name="Delete group"/>
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
