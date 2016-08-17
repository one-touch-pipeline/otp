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