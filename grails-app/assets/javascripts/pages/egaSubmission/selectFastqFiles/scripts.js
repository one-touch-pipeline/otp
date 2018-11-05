/*jslint browser: true */
/*global $ */

$.otp.selectFastqFiles = {

    combineCheckBoxes: function () {
        "use strict";
        $(document).on('click', 'input[type="checkbox"][data-group]', function(event) {
            var actor = $(this);
            var checked = actor.prop('checked');
            var group = actor.data('group');
            var checkboxes = $('input[type="checkbox"][data-group="' + group + '"]');
            var otherCheckboxes = checkboxes.not(actor);
            otherCheckboxes.prop('checked', checked);
        });
    }
};