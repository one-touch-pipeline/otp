<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="alignment.quality.title" args="${[project, seqType]}"/></title>
    <asset:javascript src="pages/alignmentQualityOverview/index/dataTable.js"/>
    <asset:javascript src="pages/alignmentQualityOverview/index/alignmentQualityOverview.js"/>
</head>

<body>
    <div class="body">
    <g:if test="${projects}">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <g:if test="${seqType}">
            <div class="blue_label" id="projectsGroupbox">
                <span class="blue_label"><g:message code="alignment.quality.seqType"/> :</span>
                <form style="display: inline">
                    <g:select class="criteria" id="seqType" name='seqType'
                              from='${seqTypes}' value='${seqType.id}' optionKey='id' optionValue='displayNameWithLibraryLayout' onChange='submit();'/>
                </form>
            </div>
        </g:if>
        <div class="otpDataTables alignmentQualityOverviewTable">
            <otp:dataTable
                codes="${header}"
                id="overviewTableProcessedMergedBMF"/>
        </div>
    <asset:script>
        $(function() {
            $.otp.alignmentQualityOverviewTable.register();
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
