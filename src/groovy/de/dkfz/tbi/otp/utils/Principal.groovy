package de.dkfz.tbi.otp.utils

import groovy.transform.Immutable

@Immutable
class Principal {
    String username
    String displayName = "OTP Developer"

    @Override
    String toString() {
        return username
    }
}
