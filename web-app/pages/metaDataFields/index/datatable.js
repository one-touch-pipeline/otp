/*jslint browser: true */
/*global $ */
$.otp.metadataFieldsTable = {
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