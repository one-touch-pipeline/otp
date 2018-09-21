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
        <div class="searchCriteriaTableSequences">
            <table id="searchCriteriaTable" style="display: inline-block">
                <tr>
                    <td id="projectName">${project.name}</td>
                    <td>
                        <span class="blue_label"><g:message code="egaSubmission.selectSamples.seqType"/>:</span>
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
        <g:form controller="egaSubmission" action="selectSamples">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                            '',
                            'egaSubmission.selectSamples.individual',
                            'egaSubmission.selectSamples.sampleType',
                            'egaSubmission.selectSamples.seqType'
                    ]}"
                    id="selectSamplesTable" />
            </div>
            <g:hiddenField name="submission.id" value="${submissionId}" />
            <g:submitButton name="next" value="Next"/>
        </g:form>
        <asset:script>
            $(function() {
                $.otp.selectSamplesTable.selectSamples();
            });
        </asset:script>
    </div>
</body>
</html>