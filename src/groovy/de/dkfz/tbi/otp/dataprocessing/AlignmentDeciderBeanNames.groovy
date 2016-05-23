package de.dkfz.tbi.otp.dataprocessing

enum AlignmentDeciderBeanNames {
    NO_ALIGNMENT('noAlignmentDecider', 'No Alignment'),
    OTP_ALIGNMENT('defaultOtpAlignmentDecider', 'OTP Alignment'),
    PAN_CAN_ALIGNMENT('panCanAlignmentDecider', 'PanCan Alignment');

    final String bean
    final String displayName

    private AlignmentDeciderBeanNames(String bean, String displayName) {
        this.bean = bean
        this.displayName = displayName
    }

    static String findByBeanName(String bean) {
        values().find {it.bean == bean}
    }

}
