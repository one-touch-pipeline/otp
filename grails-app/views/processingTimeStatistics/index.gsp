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
    <meta name="layout" content="processingTimeStatisticsLayout"/>
    <title><g:message code="processingTimeStatistics.title"/></title>
</head>
<body>
<div><a href="${createLink(controller: 'home', action: 'index')}">back to OTP</a></div>
<div class="errorContainer">
    <button class="closeError">close errors</button>
</div>
<div class="periodContainer">
    <p>from: </p><input type="text" class="datePicker" id="dpFrom" value="${new Date().minus(6).format('yyyy-MM-dd')}">
    <p>to: </p><input type="text" class="datePicker" id="dpTo" value="${new Date().format('yyyy-MM-dd')}">
</div>
<div class="processingStatisticsBody">
    <div class="otpDataTables">
    <otp:dataTable codes="${[
            'processingTimeStatistics.tableHeader.otrsTicketNumber',
            'processingTimeStatistics.tableHeader.ilseId',
            'processingTimeStatistics.tableHeader.project',
            'processingTimeStatistics.tableHeader.numberOfRuns',
            'processingTimeStatistics.tableHeader.numberOfSamples',
            'processingTimeStatistics.tableHeader.numberOfLanes',
            'processingTimeStatistics.tableHeader.submissionReceivedNotice',
            'processingTimeStatistics.tableHeader.delay',
            'processingTimeStatistics.tableHeader.ticketCreated',
            'processingTimeStatistics.tableHeader.delay',
            'processingTimeStatistics.tableHeader.installationStarted',
            'processingTimeStatistics.tableHeader.diff',
            'processingTimeStatistics.tableHeader.installationFinished',
            'processingTimeStatistics.tableHeader.delay',
            'processingTimeStatistics.tableHeader.fastqcStarted',
            'processingTimeStatistics.tableHeader.diff',
            'processingTimeStatistics.tableHeader.fastqcFinished',
            'processingTimeStatistics.tableHeader.delay',
            'processingTimeStatistics.tableHeader.alignmentStarted',
            'processingTimeStatistics.tableHeader.diff',
            'processingTimeStatistics.tableHeader.alignmentFinished',
            'processingTimeStatistics.tableHeader.delay',
            'processingTimeStatistics.tableHeader.snvStarted',
            'processingTimeStatistics.tableHeader.diff',
            'processingTimeStatistics.tableHeader.snvFinished',
            'processingTimeStatistics.tableHeader.total',
            'processingTimeStatistics.tableHeader.comment',
    ]}" id="processingTimeStatisticsTable"/>
        </div>
</div>
<asset:script>
    $(function() {
        $.otp.processingTimeStatistics.registerDataTable();
        $.otp.processingTimeStatistics.registerDatePicker();
    });
</asset:script>
</body>
</html>
