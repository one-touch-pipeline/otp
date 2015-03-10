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