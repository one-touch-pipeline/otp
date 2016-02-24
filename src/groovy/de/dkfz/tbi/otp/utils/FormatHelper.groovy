package de.dkfz.tbi.otp.utils

class FormatHelper {

    /**
     * null pointer save formatter for double.
     * If argument is null, return empty string, otherwise format the number
     * to two digits and return the formated string.
     */
    public static String formatToTwoDecimalsNullSave(Double number) {
        if (number == null) {
            return ""
        }
        return String.format(Locale.ENGLISH, '%.2f', number)
    }



    /**
     * null pointer save formatter for display number with group digits.
     * If argument is null, return empty string, otherwise format the number
     * using groups of three digest.
     */
    public static String formatGroupsNullSave(Long longValue) {
        if (longValue == null) {
            return ""
        }
        return String.format(Locale.ENGLISH, '%,d', longValue)
    }

}
