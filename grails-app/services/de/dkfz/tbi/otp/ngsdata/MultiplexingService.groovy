package de.dkfz.tbi.otp.ngsdata

import java.util.regex.Matcher

class MultiplexingService {

    public static final String BARCODE_DELIMITER = '_'

    /**
     * find and extract barcodes of the file. It uses the following patterns in the given order:
     * <ul>
     * <li><code>/_([GATC]{6,8})_/</code></li>
     * <li><code>/_[0-9]_([0-9]{3})[\._]/</code></li>
     * </ul>
     * The first matching pattern is used and the group 1 is returned as barcode.
     *
     * @param fileName the name of the file to check for
     * @return the extracted barcode or null
     */
    public static String barcode(String fileName) {
        final List<String> regExpression = [
            /_([GATC]{6,8})_/ ,
            /_[0-9]_([0-9]{3})[\._]/ ,
            ]
        for (String muxRegexp: regExpression) {
            Matcher matcher = (fileName =~ muxRegexp)
            if (matcher) {
                return matcher.group(1)
            }
        }
        return null
    }

    public static String combineLaneNumberAndBarcode(String laneNumber, String barcode) {
        assert laneNumber
        if (barcode != null) {
            return "${laneNumber}${BARCODE_DELIMITER}${barcode}"
        } else {
            return laneNumber
        }
    }
}
