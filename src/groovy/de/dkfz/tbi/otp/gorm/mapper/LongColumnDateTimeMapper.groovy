package de.dkfz.tbi.otp.gorm.mapper

import org.jadira.usertype.dateandtime.joda.util.ZoneHelper
import org.jadira.usertype.dateandtime.shared.spi.AbstractLongColumnMapper
import org.joda.time.DateTime

/**
 * Converts joda-time {@link org.joda.time.DateTime} objects to long and vice versa
 */
class LongColumnDateTimeMapper extends AbstractLongColumnMapper<DateTime> {

    @Override
    Long toNonNullValue(DateTime dateTime) {
        return dateTime.millis
    }

    @Override
    String toNonNullString(DateTime dateTime) {
        return toNonNullValue(dateTime).toString()
    }

    @Override
    DateTime fromNonNullValue(Long millis) {
        return new DateTime(millis).withZone(ZoneHelper.getDefault())
    }

    @Override
    DateTime fromNonNullString(String millisAsString) {
        return fromNonNullValue(millisAsString as long)
    }
}
