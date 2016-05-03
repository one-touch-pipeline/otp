package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

/**
 * {@link SeqTrack}s from {@link SeqPlatform}s in the same {@link SeqPlatformGroup} can be merged.
 */
class SeqPlatformGroup implements Entity {

    String name

    static constraints = {
        name blank: false, unique: true
    }

    @Override
    String toString() {
        return name
    }
}
