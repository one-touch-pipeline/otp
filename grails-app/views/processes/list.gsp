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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.list.title"/></title>
    <asset:javascript src="pages/processes/common.js"/>
    <asset:javascript src="pages/processes/list/list.js"/>
</head>
<body>
    <div class="body">
        <otp:autoRefresh/>
        <h1><g:message code="processes.list.title"/></h1>
        <div id="workflowOverview">
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                    'workflow.list.table.headers.workflow',
                    'workflow.list.table.headers.count',
                    'workflow.list.table.headers.countFailed',
                    'workflow.list.table.headers.countRunning',
                    'workflow.list.table.headers.lastSuccess',
                    'workflow.list.table.headers.lastFailure',
                 ]}" id="workflowOverviewTable"/>
            </div>
        </div>
        <asset:script type="text/javascript">
            $(document).ready(function() {
                $.otp.workflows.registerJobExecutionPlan('#workflowOverviewTable');
            });
        </asset:script>
    </div>
</body>
</html>
