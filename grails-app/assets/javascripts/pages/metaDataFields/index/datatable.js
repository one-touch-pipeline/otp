/*jslint browser: true */
/*global $ */
$.otp.libraryPreparationKitTable = {
        registerlistLibraryPreparationKit : function () {
            "use strict";
            var oTablelistLibraryPreparationKit = $.otp.createListView(
                    '#listLibraryPreparationKit',
                    $.otp.createLink({
                        controller: 'metaDataFields',
                        action: 'dataTableSourceListLibraryPreparationKit'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTablelistLibraryPreparationKit;
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
                        action: 'dataTableSourceListSeqPlatforms'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTableListPlatform;
        }
};

$.otp.SeqTypeTable = {
        registerListSeqTypeTable: function () {
            "use strict";
            var oTableListSeqType = $.otp.createListView(
                    '#listSeqType',
                    $.otp.createLink({
                        controller: 'metaDataFields',
                        action: 'dataTableSourceListSeqType'
                    }),
                    function (json) {
                        return json;
                    }
                );
            return oTableListSeqType;
        }
};