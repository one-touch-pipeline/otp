<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="snv.processing.status" args="${[individual.mockPid]}"/></title>
</head>
<body>
    <div class="body">
            <h2><td class="myKey"><g:message code="snv.individual.details.pid"/>:</td>
                <td class="myValue">${individual.mockPid} <input id="mockPid" type="hidden" value="${individual.mockPid}"/></td>
            </h2>
        <p><g:message code="snv.samplePairsInMultipleLists" /></p>
        <h3 class="otpExtraTopMargin"><g:message code="snv.finishedSamples" /></h3>
        <div class="otpDataTables otpDataTableWithFreeSpaceTopLeft">
             <otp:dataTable
            codes="${[
                'snv.sampleType1',
                'snv.sampleType2',
                'snv.seqType',
                'snv.lastUpdated',
                'snv.samplePairPath',
            ] }"
                id="samplePairForSnvProcessingSnvFinished" />
        </div>
        <h3 class="otpExtraTopMargin"><g:message code="snv.inProgressSamples" /></h3>
        <div class="otpDataTables otpDataTableWithFreeSpaceTopLeft">
                     <otp:dataTable
                    codes="${[
                        'snv.sampleType1',
                        'snv.sampleType2',
                        'snv.seqType',
                        'snv.dateCreated',
                    ] }"
                        id="samplePairForSnvProcessingSNVInProgress" />
                </div>
        <h3 class="otpExtraTopMargin"><g:message code="snv.notstartedSamples" /></h3>
        <p><g:message code="snv.title.detailPages" /></p>
        <div class="otpDataTables otpDataTableWithFreeSpaceTopLeft">
                    <otp:dataTable
                    codes="${[
                        'snv.sampleType1',
                        'snv.index.availableRequestedLaneCount1',
                        'snv.index.availableRequestedCoverage1',
                        'snv.sampleType2',
                        'snv.index.availableRequestedLaneCount2',
                        'snv.index.availableRequestedCoverage2',
                        'snv.seqType',
                    ] }"
                        id="notStartedSamplePairs" />
        </div>
        <h3 class="otpExtraTopMargin"><g:message code="snv.diasablesSample" /></h3>
        <div class="otpDataTables otpDataTableWithFreeSpaceTopLeft">
                     <otp:dataTable
                    codes="${[
                        'snv.sampleType1',
                        'snv.sampleType2',
                        'snv.seqType',
                    ] }"
                        id="disabledSamplePairs" />
        </div>
        <p></p>
    </div>
    <r:script>
        $(function() {
            $.otp.samplePair.register();
            });
    </r:script>
</body>
</html>
