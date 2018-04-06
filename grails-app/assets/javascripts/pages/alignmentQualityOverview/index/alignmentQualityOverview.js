/*jslint browser: true */
/*global $ */

$.otp.alignmentQualityOverview = {
    change : function (dropdownMenu, id, oldValue) {
        var comment = prompt("Please provide a comment for this change:");

        if (comment == null) {
            dropdownMenu.value = oldValue;
        } else {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": $.otp.createLink({
                    controller: 'alignmentQualityOverview',
                    action: 'changeQcStatus',
                }),
                "data": {
                    "abstractBamFile.id": id,
                    "newValue": dropdownMenu.value,
                    "comment": comment,
                },
                "success" : function (json) {
                    if (!json.success) {
                        alert("Failed to edit value.\n" + json.error);
                        dropdownMenu.value = oldValue;
                    } else {
                        $("#overviewTableProcessedMergedBMF").DataTable().destroy();
                        $.otp.alignmentQualityOverviewTable.register();
                    }
                }
            });
        }
    },
}
