/*jslint browser: true */
/*global $ */

$.otp.projectUser = {

    toggle: function (controlElement, linkElement) {
        var control = document.getElementById(controlElement);
        var link = document.getElementById(linkElement);

        if (control.style.display == "none") {
            control.style.display = "";
            link.innerHTML = "Hide list";
        } else {
            control.style.display = "none";
            link.innerHTML = "Show list";
        }
    }
};
