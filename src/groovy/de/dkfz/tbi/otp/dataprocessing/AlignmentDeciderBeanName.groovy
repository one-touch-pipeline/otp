package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.*

@TupleConstructor
enum AlignmentDeciderBeanName {
    NO_ALIGNMENT('noAlignmentDecider', 'No Alignment'),
    OTP_ALIGNMENT('defaultOtpAlignmentDecider', 'OTP Alignment'),
    PAN_CAN_ALIGNMENT('panCanAlignmentDecider', 'PanCan Alignment'),

    final String beanName
    final String displayName

    static String findByBeanName(String bean) {
        values().find { it.beanName == bean }
    }
}
