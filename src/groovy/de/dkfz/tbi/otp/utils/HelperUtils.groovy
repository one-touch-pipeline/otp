package de.dkfz.tbi.otp.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class HelperUtils {
    static Random random = new Random()
    static DateTimeFormatter formatter = DateTimeFormat.forPattern('yyyy-MM-dd-HH-mm-ss-SSSZ')

    static String getUniqueString() {
        return "${formatter.print(new DateTime())}-${sprintf('%016X', random.nextLong())}"
    }

    static String getRandomMd5sum() {
        return sprintf('%016x', random.nextLong()) + sprintf('%016x', random.nextLong())
    }

    static String getRandomEmail() {
        return sprintf('%016x.%016x@%016x.com', random.nextLong(), random.nextLong(), random.nextLong())
    }


    static String byteArrayToHexString(byte[] bytes) {
        assert bytes
        return new BigInteger(1, bytes).toString(16).padLeft(bytes.length * 2, '0')
    }
}
