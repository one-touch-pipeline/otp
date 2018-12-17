package de.dkfz.tbi.otp.config

import spock.lang.Specification

class OtpPropertySpec extends Specification {

    void 'ensure that the keys are unique'() {
        expect:
        OtpProperty.values()*.key == OtpProperty.values()*.key.unique()
    }
}
