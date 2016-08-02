/*jslint browser: true */
/*global $ */

// run functions specific to the controller or action automatically
$(function() {
    var controllerSpecific = OTP.getFunctionFromString("OTP.pages." + OTP.controllerName + ".init");
    if (typeof controllerSpecific === 'function') {
        controllerSpecific();
    }
    var actionSpecific = OTP.getFunctionFromString("OTP.pages." + OTP.controllerName + "." + OTP.actionName + ".init");
    if (typeof actionSpecific === 'function') {
        actionSpecific();
    }
});
