<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of Processing Steps for Process # ${id} for Workflow ${name}</title>
<jqDT:resources/>
<g:javascript library="jquery.dataTables" />
<g:javascript src="jquery.timeago.js"/>
</head>
<body>
  <div class="body">
    <h1>List of Processing Steps for Process # ${id} for <g:link action="plan" id="${planId}">Workflow ${name}</g:link></h1>
    <div id="processOverview">
      <table id="processOverviewTable">
        <thead>
            <tr>
                <th>&nbsp;</th>
                <th>&nbsp;</th>
                <th>Processing Step</th>
                <th>Job</th>
                <th>Creation Date</th>
                <th>Last Update</th>
                <th>Duration</th>
                <th>Status</th>
                <th>&nbsp;</th>
            </tr>
        </thead>
        <tbody>
        </tbody>
      </table>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.createProcessingStepListView("#processOverviewTable", ${id});
        });
    </g:javascript>
  </div>
</body>
</html>
