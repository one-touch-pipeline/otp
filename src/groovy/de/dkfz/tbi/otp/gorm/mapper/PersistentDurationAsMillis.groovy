package de.dkfz.tbi.otp.gorm.mapper

import org.jadira.usertype.dateandtime.shared.spi.AbstractSingleColumnUserType
import org.joda.time.Duration

/**
 * Converts joda-time {@link org.joda.time.Duration} objects to long and vice versa
 * using {@link LongColumnDurationMapper}
 */
class PersistentDurationAsMillis extends AbstractSingleColumnUserType<Duration, Long, LongColumnDurationMapper> {
}
