<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
</head>

<body>
    <div class="body">
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.exomeEnrichmentKit',
                    'dataFields.exomeEnrichmentKit.alias',
                ] }"
                id="listExomeEnrichmentKit"
            />
        </div>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.listAntibodyTarget',
                ] }"
                id="listAntibodyTarget"
            />
        </div>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.listSeqCenterName',
                    'dataFields.listSeqCenterDirName',
                ] }"
                id="listSeqCenter"
            />
        </div>
    </div>
    <r:script>
        $(function() {
            $.otp.exomeEnrichmentKitTable.registerlistExomeEnrichmentKit();
            $.otp.antibodyTargetTable.registerListAntibodyTarget();
            $.otp.seqCenterTable.registerListSeqCenter();
        });
    </r:script>
</body>
</html>