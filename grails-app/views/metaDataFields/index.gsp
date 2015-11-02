<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <asset:javascript src="pages/metaDataFields/index/datatable.js"/>
</head>

<body>
    <div class="body">
        <h3>
            <g:message code="dataFields.title.libraryPreparationKitTable" />
        </h3>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.libraryPreparationKit',
                    'dataFields.libraryPreparationKit.alias',
                ] }"
                id="listLibraryPreparationKit"
            />
        </div>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.antibodyTargetTable" />
        </h3>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.listAntibodyTarget',
                ] }"
                id="listAntibodyTarget"
            />
        </div>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.centersTable" />
        </h3>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.listSeqCenterName',
                    'dataFields.listSeqCenterDirName',
                ] }"
                id="listSeqCenter"
            />
        </div>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.listPlatformNameTable" />
        </h3>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.listPlatformGroup',
                    'dataFields.listPlatformName',
                    'dataFields.listPlatformModelLabel',
                    'dataFields.listPlatformModelLabelAlias',
                    'dataFields.listSequeningKitLabel',
                    'dataFields.listSequeningKitLabelAlias',
                ] }"
                id="listPlatformAndIdentifier"
            />
        </div>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.titlelistSeqTypeTable" />
        </h3>
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'dataFields.listSeqType',
                ] }"
                id="listSeqType"
            />
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.libraryPreparationKitTable.registerlistLibraryPreparationKit();
            $.otp.antibodyTargetTable.registerListAntibodyTarget();
            $.otp.seqCenterTable.registerListSeqCenter();
            $.otp.platformTable.registerListPlatformTable();
            $.otp.SeqTypeTable.registerListSeqTypeTable();
        });
    </asset:script>
</body>
</html>
