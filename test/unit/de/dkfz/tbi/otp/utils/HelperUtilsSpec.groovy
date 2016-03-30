package de.dkfz.tbi.otp.utils

import spock.lang.*

import static de.dkfz.tbi.otp.utils.HelperUtils.*

class HelperUtilsSpec extends Specification {

    void 'byteArrayToHexString returns expected string'(byte[] bytes, String hexString) {
        expect:
        byteArrayToHexString(bytes) == hexString

        where:
        bytes   || hexString
        [0]     || '00'
        [1]     || '01'
        [10]    || '0a'
        [16]    || '10'
        [255]   || 'ff'
        [0, 1]  || '0001'
        [1, 0]  || '0100'
        [16, 0] || '1000'
    }
}
