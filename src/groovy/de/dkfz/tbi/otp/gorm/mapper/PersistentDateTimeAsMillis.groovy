package de.dkfz.tbi.otp.gorm.mapper

import org.jadira.usertype.dateandtime.shared.spi.AbstractSingleColumnUserType
import org.joda.time.DateTime

/**
 * Converts joda-time {@link org.joda.time.DateTime} objects to long and vice versa
 * using {@link LongColumnDateTimeMapper}
 */
class PersistentDateTimeAsMillis extends AbstractSingleColumnUserType<DateTime, Long, LongColumnDateTimeMapper> {
}
