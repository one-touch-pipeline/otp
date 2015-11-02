/*jslint browser: true */
/*global $ */

$(function () {
    "use strict";

    $('#loginForm').find('#account').focus();

    $('.loginButtonBox').click(function () {
        $('#loginForm').submit();
    });
});
