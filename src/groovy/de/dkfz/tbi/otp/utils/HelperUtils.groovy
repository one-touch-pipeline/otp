package de.dkfz.tbi.otp.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class HelperUtils {
    static Random random = new Random()
    static DateTimeFormatter formatter = DateTimeFormat.forPattern('yyyy-MM-dd-HH-mm-ss-SSSZ')

    public static String getUniqueString() {
        return "${formatter.print(new DateTime())}-${sprintf('%016X', random.nextLong())}"
    }

    public static String getRandomMd5sum() {
        return sprintf('%016x', random.nextLong()) + sprintf('%016x', random.nextLong())
    }
}
