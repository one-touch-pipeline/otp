package de.dkfz.tbi.otp.utils

class FormatHelperUnitTest {


    void testFormatToTwoDecimalsNullSave_hasAlreadyTwoDigits() {
        Double d = new Double(2.67)
        String expected = "2.67"
        String value = FormatHelper.formatToTwoDecimalsNullSave(d)
        assert expected == value
    }

    void testFormatToTwoDecimalsNullSave_addZeros() {
        Double d = new Double(2)
        String expected = "2.00"
        String value = FormatHelper.formatToTwoDecimalsNullSave(d)
        assert expected == value
    }

    void testFormatToTwoDecimalsNullSave_roundUp() {
        Double d = new Double(2.678566)
        String expected = "2.68"
        String value = FormatHelper.formatToTwoDecimalsNullSave(d)
        assert expected == value
    }

    void testFormatToTwoDecimalsNullSave_roundDown() {
        Double d = new Double(2.67234)
        String expected = "2.67"
        String value = FormatHelper.formatToTwoDecimalsNullSave(d)
        assert expected == value
    }

    void testFormatToTwoDecimalsNullSave_nullPointer() {
        Double d = null
        String expected = ""
        String value = FormatHelper.formatToTwoDecimalsNullSave(d)
        assert expected == value
    }



    void testFormatGroupsNullSave_noGroup() {
        Long l = new Long(234)
        String expected = "234"
        String value = FormatHelper.formatGroupsNullSave(l)

        assert expected == value
    }

    void testFormatGroupsNullSave_oneGroup() {
        Long l = new Long(1234)
        String expected = "1,234"
        String value = FormatHelper.formatGroupsNullSave(l)

        assert expected == value
    }

    void testFormatGroupsNullSave_manyGroups() {
        Long l = new Long(1234567890)
        String expected = "1,234,567,890"
        String value = FormatHelper.formatGroupsNullSave(l)

        assert expected == value
    }

    void testFormatGroupsNullSave_nullPointer() {
        Long l = null
        String expected = ""
        String value = FormatHelper.formatGroupsNullSave(l)

        assert expected == value
    }
}
