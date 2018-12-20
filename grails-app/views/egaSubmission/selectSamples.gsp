<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.selectSamples.title"/></title>
    <asset:javascript src="pages/egaSubmission/selectSamples/datatable.js"/>
</head>
<body>
    <div class="body">
        <g:link style="float: right" action="helpPage" fragment="selectSamples" target="_blank">
            <g:img file="info.png"/>
        </g:link>
        <h3><g:message code="egaSubmission.selectSamples.title"/></h3>
        <div class="searchCriteriaTableSequences">
            <table id="searchCriteriaTable" style="display: inline-block">
                <tr>
                    <td id="projectName">${project.name}</td>
                    <td>
                        <span class="blue_label">Filter by <g:message code="egaSubmission.seqType"/>:</span>
                    </td>
                    <td>
                        <table id="searchCriteriaTableSeqType">
                            <tr>
                                <td class="attribute">
                                    <g:select class="criteria"
                                              name="criteria"
                                              from="${seqTypes}"
                                              option="${seqTypes}"
                                              noSelection="${["none": message(code:"otp.filter.seqType")]}"/>
                                </td>
                                <td class="value">
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </div>
        <g:form controller="egaSubmission" action="selectSamplesForm">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                            '',
                            'egaSubmission.individual',
                            'egaSubmission.sampleType',
                            'egaSubmission.seqType'
                    ]}"
                    id="selectSamplesTable" />
            </div>
            <g:hiddenField name="submission.id" value="${submissionId}" />
            <g:submitButton name="next" value="Confirm"/>
        </g:form>
        <asset:script>
            $(function() {
                $.otp.selectSamplesTable.selectSamples(${sampleIds});
            });
        </asset:script>
    </div>
</body>
</html>