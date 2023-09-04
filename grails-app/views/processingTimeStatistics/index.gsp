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
<%@ page import="de.dkfz.tbi.util.TimeFormats" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processingTimeStatistics.title"/></title>
    <asset:javascript src="pages/processingTimeStatistics/index/datatable.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <div class="processing-time-statistics-top-bar rounded-page-header-box">
            <div class="error-container" hidden>
                <button class="close-error">close errors</button>
                <div class="error-list"></div>
            </div>
            <div class="date-pickers">
                <label for="dpFrom">from:</label> <input type="date" max="${dateTo}" class="datePicker" id="dpFrom" value="${dateFrom}" required="required">
                <label for="dpTo">to:</label> <input type="date" max="${dateTo}" class="datePicker" id="dpTo" value="${dateTo}" required="required">
            </div>
        </div>
        <div class="temp-processing-time-statistics-table-wrapper">
            <otp:dataTable codes="${[
                    'processingTimeStatistics.tableHeader.ticketNumber',
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
</body>
</html>
