<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="alignment.quality.title" args="${[project, seqType]}"/></title>
    <asset:javascript src="pages/alignmentQualityOverview/index/dataTable.js"/>
</head>

<body>
    <div class="body">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <div class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="alignment.quality.seqType"/> :</span>
            <form style="display: inline">
                <g:select class="criteria" id="seqType" name='seqType'
                          from='${seqTypes}' value='${seqType.id}' optionKey='id' optionValue='displayNameWithLibraryLayout' onChange='submit();'/>
            </form>
        </div>
        <br><br><br>
        <div id="withdrawn_description">
            Withdrawn data is colored gray
        </div>
        <div class="otpDataTables alignmentQualityOverviewTable">
            <otp:dataTable
                codes="${header}"
                id="overviewTableProcessedMergedBMF"/>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.alignmentQualityOverviewTable.register();
        });
    </asset:script>
</body>
</html>
