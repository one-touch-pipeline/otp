<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.process.title.listOfProcessingSteps" args="${ [id] }"/> <g:message code="processes.process.title.workflow" args="${ [name] }"/></title>
    <r:require module="workflows"/>
</head>
<body>
    <div class="body">
        <div id="processInfoBox">
            <otp:autoRefresh/>
            <h1><g:message code="processes.process.title.listOfProcessingSteps" args="${ [id] }"/>  <g:link action="plan" id="${planId}"><g:message code="processes.process.title.workflow" args="${ [name] }"/></g:link></h1>
            <g:if test="${parameter}">
                <p><g:message code="processes.process.operatesOn" args="${ [parameter] }"/></p>
            </g:if>
            <div>
                <div id="process-visualization" style="display: none"></div>
                <button id="show-visualization"><g:message code="processes.process.showProcessVisualization"/></button>
                <button id="hide-visualization" style="display: none"><g:message code="processes.process.hideProcessVisualization"/></button>
            </div>
        </div>
        <div id="processCommentBox" class="commentBoxContainer">
            <div id="commentLabel">Comment:</div>
            <sec:ifNotGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox" readonly>${comment?.comment?.encodeAsHTML()}</textarea>
            </sec:ifNotGranted>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox">${comment?.comment?.encodeAsHTML()}</textarea>
                <div id="commentButtonArea">
                    <button id="saveComment" disabled>&nbsp;&nbsp;&nbsp;<g:message code="commentBox.save" /></button>
                    <button id="cancelComment" disabled><g:message code="commentBox.cancel" /></button>
                </div>
            </sec:ifAllGranted>
            <div id="commentDateLabel">${comment?.modificationDate?.format('EEE, d MMM yyyy HH:mm')}</div>
            <div id="commentAuthorLabel">${comment?.author}</div>
        </div>
        <div id="processOverview">
            <div class="otpDataTables">
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
        </div>
        <g:javascript>
            $(document).ready(function() {
                $.otp.workflows.registerProcessingStep("#processOverviewTable", ${id});
                $.otp.initCommentBox(${id}, "#processCommentBox");
            });
        </g:javascript>
    </div>
</body>
</html>
