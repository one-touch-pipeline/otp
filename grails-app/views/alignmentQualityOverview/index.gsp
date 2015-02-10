<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectStatistic.title" /></title>
</head>

<body>
    <div class="body">
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="alignment.quality.project"/> :</span>
            <g:select class="criteria" id="project" name='project'
                from='${projects}' value='${project}' onChange='submit();'></g:select>
            <span class="blue_label"><g:message code="alignment.quality.seqType"/> :</span>
            <g:if test="${seqTypes.size>1}">
                <g:select class="criteria" id="seqType" name='seqType'
                from='${seqTypes}' value='${seqType}' onChange='submit();'></g:select>
            </g:if>
            <g:else>
                <span class="blue_label">
                ${seqType}
                </span>
                <input type='hidden' id='seqType' name='seqType' value='${seqType}'/>
            </g:else>
        </form>
        <div class="otpDataTables alignmentQualityOverviewTable">
            <otp:dataTable
                codes="${header}"
                id="overviewTableProcessedMergedBMF"/>
        </div>
    </div>
    <r:script>
        $(function() {
            $.otp.alignmentQualityOverviewTable.register();
        });
    </r:script>
</body>
</html>
