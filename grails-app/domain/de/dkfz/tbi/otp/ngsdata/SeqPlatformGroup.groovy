package de.dkfz.tbi.otp.ngsdata

/**
 * {@link SeqTrack}s from {@link SeqPlatform}s in the same {@link SeqPlatformGroup} can be merged.
 */
class SeqPlatformGroup {

    String name

    static constraints = {
        name blank: false, unique: true
    }

    @Override
    String toString() {
        return name
    }
}
