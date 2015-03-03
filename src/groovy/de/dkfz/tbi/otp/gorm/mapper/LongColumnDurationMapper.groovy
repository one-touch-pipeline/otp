package de.dkfz.tbi.otp.gorm.mapper

import org.jadira.usertype.dateandtime.shared.spi.AbstractLongColumnMapper
import org.joda.time.Duration

/**
 * Converts joda-time {@link org.joda.time.Duration} objects to long and vice versa
 */
class LongColumnDurationMapper extends AbstractLongColumnMapper<Duration> {

    @Override
    Long toNonNullValue(Duration duration) {
        return duration.millis
    }

    @Override
    String toNonNullString(Duration duration) {
        return toNonNullValue(duration).toString()
    }

    @Override
    Duration fromNonNullValue(Long millis) {
        return new Duration(millis)
    }

    @Override
    Duration fromNonNullString(String millisAsString) {
        return fromNonNullValue(millisAsString as long)
    }
}