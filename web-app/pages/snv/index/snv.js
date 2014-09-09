/*jslint browser: true */
/*global $ */

/**
 */
$.otp.submitSNV = {
    isNumeric: function(e) {
        try {
            if (window.event) {
                var charCode = window.event.keyCode;
            } else if (e) {
                var charCode = e.which;
            } else {
                return true;
            }
            if ((charCode >= 48 && charCode <= 57)|| charCode == 08 || charCode == 13 || charCode == 9 || charCode == 0 || charCode == 46) {
                return true;
            } else {
                alert("Input fields should contain a numeric value");
                return false;
            }
        } catch (err) {
            alert("Input fields should contain a numeric value");
        }
    },
    submitAlert: function() {
        var r = confirm("Are you sure you want to confirm this registration?");
return r;
    },
}

