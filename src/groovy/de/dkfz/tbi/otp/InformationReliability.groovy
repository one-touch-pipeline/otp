package de.dkfz.tbi.otp

/**
 * The possible state for {@link InformationReliability}
 */
enum InformationReliability {
    /**
     * Indicates, that the {@link LibraryPreparationKit} is known and present
     */
    KNOWN,
    /**
     * Indicates, that the {@link LibraryPreparationKit} is verified to be unknown
     */
    UNKNOWN_VERIFIED("UNKNOWN"),
    /**
     * Indicates, that {@link LibraryPreparationKit} is not available yet and must be asked for.
     * The value is defined for migration of old data, where the {@link LibraryPreparationKit} was not asked for.
     */
    UNKNOWN_UNVERIFIED,
    /**
     * Indicates that OTP inferred this {@link LibraryPreparationKit} from a different lane, where it was explicitly {@link #KNOWN}.
     * This {@link LibraryPreparationKit} should not be relied upon (find the original instead, don't infer based on inferences!)
     */
    INFERRED


    /**
     * The raw value of the information reliability.
     * <p>
     * This defaults to the Enum {@link #name()}, but in case of {@link #UNKNOWN_VERIFIED}
     * it returns "UNKNOWN", which is the raw value as it appears in external input files
     */
    final String rawValue

    private InformationReliability() {
        this.rawValue = name()
    }

    private InformationReliability(String value) {
        this.rawValue = value
    }
}