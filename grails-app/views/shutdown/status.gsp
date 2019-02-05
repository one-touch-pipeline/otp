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
<title>Server Shutdown</title>
</head>
<body>
    <div class="body">
    <!-- TODO: global info messages -->
        <g:render template="/templates/messages"/>
        <h2>A Shutdown is currently in process.</h2>
        <table>
            <tbody>
                <tr>
                    <th>Initiated by:</th>
                    <td>${shutdown.initiatedBy.username}</td>
                </tr>
                <tr>
                    <th>Initiated at:</th>
                    <td>${shutdown.initiated}</td>
                </tr>
                <tr>
                    <th>Reason for Shutdown:</th>
                    <td>${shutdown.reason}</td>
                </tr>
            </tbody>
        </table>
        <h2>List of running Jobs which cannot be resumed</h2>
        <table>
            <thead>
                <tr>
                    <th>Workflow</th>
                    <th>Process Id</th>
                    <th>Processing Step</th>
                    <th>Job</th>
                </tr>
            </thead>
            <tbody>
                <g:each var="step" in="${notResumableJobs}">
                    <tr>
                        <td><g:link controller="processes" action="plan" id="${step.process.jobExecutionPlan.id}">${step.process.jobExecutionPlan.name}</g:link></td>
                        <td><g:link controller="processes" action="process" id="${step.process.id}">${step.process.id}</g:link></td>
                        <td><g:link controller="processes" action="processingStep" id="${step.id}">${step.jobDefinition.name}</g:link></td>
                    </tr>
                </g:each>
            </tbody>
        </table>
        <h2>List of running Jobs which can be resumed</h2>
        <table>
            <thead>
                <tr>
                    <th>Workflow</th>
                    <th>Process Id</th>
                    <th>Processing Step</th>
                    <th>Job</th>
                    </tr>
            </thead>
            <tbody>
                <g:each var="step" in="${resumableJobs}">
                    <tr>
                        <td><g:link controller="processes" action="plan" id="${step.process.jobExecutionPlan.id}">${step.process.jobExecutionPlan.name}</g:link></td>
                        <td><g:link controller="processes" action="process" id="${step.process.id}">${step.process.id}</g:link></td>
                        <td><g:link controller="processes" action="processingStep" id="${step.id}">${step.jobDefinition.name}</g:link></td>
                    </tr>
                </g:each>
            </tbody>
        </table>
        <hr/>
        <div>
            <g:form action="cancelShutdown" style="display: inline">
                <g:submitButton name="Cancel Shutdown"/>
            </g:form>
            <g:form action="closeApplication" style="display: inline">
                <g:submitButton name="Stop Application Context"/>
            </g:form>
        </div>
    </div>
</body>
</html>
