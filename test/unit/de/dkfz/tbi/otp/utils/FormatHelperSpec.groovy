package de.dkfz.tbi.otp.utils

import spock.lang.*

class FormatHelperSpec extends Specification {

    void "test formatNumber"() {
        expect:
        FormatHelper.formatNumber(input) == result

        where:
        input                || result
        new Double(2.67)     || "2.67"
        new Double(2)        || "2.00"
        new Double(2.678566) || "2.68"
        new Double(2.67234)  || "2.67"
        new Long(234)        || "234"
        new Long(1234)       || "1,234"
        new Long(1234567890) || "1,234,567,890"
        null                 || ""
    }
}
