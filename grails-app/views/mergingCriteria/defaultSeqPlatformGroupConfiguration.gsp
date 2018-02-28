<%@ page import="de.dkfz.tbi.otp.dataprocessing.MergingCriteria" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</title>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h2>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</h2>

    <h3>${g.message(code: "mergingCriteria.seqPlatformDefinition.default")}</h3>

    <div>
        <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
            <div class="small-frame-box">
                New group
                <ul>
                    <li>
                        <g:form action="createNewDefaultGroupAndAddPlatform">
                            <g:select name="platform.id" from="${allSeqPlatformsWithoutGroup}" optionKey="id" noSelection="${[null: 'Select to create new group']}"/>
                            <g:submitButton name="Save"/>
                        </g:form>
                    </li>
                </ul>
            </div>
        </g:if>
        <g:each in="${seqPlatformGroups}" var="seqPlatformGroup">
            <div class="small-frame-box">
                <ul>
                    <g:each in="${seqPlatformGroup.seqPlatforms}" var="seqPlatform">
                        <li>${seqPlatform}
                        <g:form action="removePlatformFromSeqPlatformGroup" style="display: inline;">
                            <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                            <g:hiddenField name="platform.id" value="${seqPlatform.id}"/>
                            <g:submitButton name="Remove"/>
                        </g:form>
                        </li>
                    </g:each>
                    <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
                        <li>
                            <g:form action="addPlatformToExistingSeqPlatformGroup">
                                <g:select name="platform.id" from="${allSeqPlatformsWithoutGroup}" optionKey="id" noSelection="${[null: 'Select to add to group']}"/>
                                <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                <g:submitButton name="Save"/>
                            </g:form>
                        </li>
                    </g:if>
                </ul>
                <g:form action="deleteSeqPlatformGroup">
                    <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                    <g:submitButton name="Delete group"/>
                </g:form>
            </div>
        </g:each>
    </div>

</div>
</body>
</html>
