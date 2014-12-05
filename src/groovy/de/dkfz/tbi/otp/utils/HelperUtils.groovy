package de.dkfz.tbi.otp.utils

class HelperUtils {
    static Random random = new Random()

    public static String getUniqueString() {
        return "${System.currentTimeMillis()}-${sprintf('%016X', random.nextLong())}"
    }
}
