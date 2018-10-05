package de.dkfz.tbi.otp.utils

class FormatHelper {

    /**
     * Method to format numbers.
     * If the number is a floating point number, is formatted with 2 decimal places
     * If the number is an integer, is formatted with group separators (groups of 3 digits are separated by a comma)
     * If the number is null, return empty string
     */
    static String formatNumber(Number number) {
        if (number == null) {
            return ""
        } else if (number instanceof Float || number instanceof Double || number instanceof BigDecimal) {
            return String.format(Locale.ENGLISH, '%.2f', number)
        } else {
            return String.format(Locale.ENGLISH, '%,d', number)
        }
    }
}
