<%@ page contentType="text/html;charset=UTF-8" %>
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

    <h1>${g.message(code: "mergingCriteria.title", args: [project.name])}</h1>

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
                    <g:else>
                        <g:checkBox name="libPrepKit" value="${mergingCriteria.libPrepKit}" id="libPrepKit"/>
                    </g:else>
                </td>
                <td>
                    <g:select name="seqPlatformGroup" value="${mergingCriteria.seqPlatformGroup}"
                              from="${de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups}"/>
                </td>
                <td>
                    <g:hiddenField name="project.id" value="${project.id}"/>
                    <g:hiddenField name="seqType.id" value="${seqType.id}"/>
                    <g:submitButton name="Submit"/>
                </td>
            </tr>
        </table>
    </g:form>
</div>
</body>
</html>
