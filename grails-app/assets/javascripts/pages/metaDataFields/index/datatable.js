/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
