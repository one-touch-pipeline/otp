/*jslint browser: true */
/*global $ */
$.otp.exomeEnrichmentKitTable = {
        registerlistExomeEnrichmentKit : function () {
            "use strict";
            var oTablelistExomeEnrichmentKit = $.otp.createListView(
                    '#listExomeEnrichmentKit',
                    $.otp.createLink({
                        controller: 'metaDataFields',
                        action: 'dataTableSourceListExomeEnrichmentKit'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTablelistExomeEnrichmentKit;
        }
};

$.otp.antibodyTargetTable = {
        registerListAntibodyTarget: function () {
            "use strict";
            var oTableListAntibodyTarget = $.otp.createListView(
                    '#listAntibodyTarget',
                    $.otp.createLink({
                        controller: 'metaDataFields',
                        action: 'dataTableSourceListAntibodyTarget'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTableListAntibodyTarget;
        }
};

$.otp.seqCenterTable = {
        registerListSeqCenter: function () {
            "use strict";
            var oTableListSeqCenter = $.otp.createListView(
                    '#listSeqCenter',
                    $.otp.createLink({
                        controller: 'metaDataFields',
                        action: 'dataTableSourceListSeqCenter'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTableListSeqCenter;
        }
};

$.otp.platformTable = {
        registerListPlatformTable: function () {
            "use strict";
            var oTableListPlatform = $.otp.createListView(
                    '#listPlatformAndIdentifier',
                    $.otp.createLink({
                        controller: 'metaDataFields',
                        action: 'dataTableSourceListSeqPlatformAndIdentifier'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTableListPlatform;
        }
};
