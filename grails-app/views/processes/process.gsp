<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of Processing Steps for Process # ${id} for Workflow ${name}</title>
<r:require module="jqueryDatatables"/>
<g:javascript src="jquery.timeago.js"/>
<r:require module="graphDracula"/>
</head>
<body>
  <div class="body">
    <otp:autoRefresh/>
    <h1>List of Processing Steps for Process # ${id} for <g:link action="plan" id="${planId}">Workflow ${name}</g:link></h1>
    <g:if test="${parameter}">
    <p>Process operates on ${parameter}</p>
    </g:if>
    <div>
        <div id="process-visualization" style="display: none"></div>
        <button id="show-visualization">Show Process Visualization</button>
        <button id="hide-visualization" style="display: none">Hide Process Visualization</button>
    </div>
    <div id="processOverview">
        <otp:dataTable codes="${[
            'otp.blank',
            'otp.blank',
            'workflow.process.table.headers.processingStep',
            'workflow.process.table.headers.job',
            'workflow.process.table.headers.creationDate',
            'workflow.process.table.headers.lastUpdate',
            'workflow.process.table.headers.duration',
            'workflow.process.table.headers.status',
            'otp.blank'
            ]}" id="processOverviewTable"/>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.createProcessingStepListView("#processOverviewTable", ${id});
        });
    </g:javascript>
  </div>
</body>
</html>
