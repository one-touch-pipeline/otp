package de.dkfz.tbi.otp.security

import grails.plugin.springsecurity.SpringSecurityUtils

abstract class DicomAuditUtils {
    static String getRealUserName(String username) {
        if (SpringSecurityUtils.isSwitched()) {
            username = "${SpringSecurityUtils.getSwitchedUserOriginalUsername()} as ${username}"
        }
        return username
    }
}
