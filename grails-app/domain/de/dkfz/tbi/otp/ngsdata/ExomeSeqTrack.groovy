package de.dkfz.tbi.otp.ngsdata

class ExomeSeqTrack extends SeqTrack {

    /**
     * The possible state for {@link ExomeSeqTrack#kitInfoState}
     *
     *
     */
    enum KitInfoState {
        /**
         * Indicates, that the {@link ExomeEnrichmentKit} is known and present
         */
        KNOWN,
        /**
         * Indicates, that the {@link ExomeEnrichmentKit} is verified to be unknown
         */
        UNKNOWN,
        /**
         * Indicates, that {@link ExomeEnrichmentKit} is not available yet and must be asked for.
         * The value is defined for migration of old data, where the {@link ExomeEnrichmentKit} was not asked for.
         */
        LATER_TO_CHECK
    }

    /**
     * Holds the information about the state of {@link #exomeEnrichmentKit}.
     * If the value is {@link KitInfoState#KNOWN}, the {@link #exomeEnrichmentKit} needs to be there,
     * in any other case the {@link #exomeEnrichmentKit} have to be <code>null</code>.
     * The default value is {@link KitInfoState#LATER_TO_CHECK}
     */
    KitInfoState kitInfoState = KitInfoState.LATER_TO_CHECK

    static belongsTo = [
        /**
         * Reference to the used {@link ExomeEnrichmentKit}.
         * If present, the value of {link #kitInfoState} has to be {@link KitInfoState#KNOWN}
         */
        exomeEnrichmentKit: ExomeEnrichmentKit
    ]

    static constraints = {
        kitInfoState(nullable: false)
        exomeEnrichmentKit(nullable: true, validator: { ExomeEnrichmentKit val, ExomeSeqTrack obj ->
            if (obj.kitInfoState == KitInfoState.KNOWN) {
                return val != null
            } else {
                return val == null
            }
        })
    }

    public String toString() {
        return "${super.toString()} ${kitInfoState} ${exomeEnrichmentKit}"
    }
}
