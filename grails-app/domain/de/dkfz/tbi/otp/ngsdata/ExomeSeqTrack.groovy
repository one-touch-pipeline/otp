package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.InformationReliability

class ExomeSeqTrack extends SeqTrack {
    /**
     * Holds the information about the state of {@link #exomeEnrichmentKit}.
     * If the value is {@link InformationReliability#KNOWN}, the {@link #exomeEnrichmentKit} needs to be there,
     * in any other case the {@link #exomeEnrichmentKit} have to be <code>null</code>.
     * The default value is {@link InformationReliability#UNKNOWN_UNVERIFIED}
     */
    InformationReliability kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED

    static belongsTo = [
        /**
         * Reference to the used {@link ExomeEnrichmentKit}.
         * If present, the value of {link #kitInfoReliability} has to be {@link kitInfoReliability#KNOWN}
         */
        exomeEnrichmentKit: ExomeEnrichmentKit
    ]

    static constraints = {
        kitInfoReliability(nullable: false)
        exomeEnrichmentKit(nullable: true, validator: { ExomeEnrichmentKit val, ExomeSeqTrack obj ->
            if (obj.kitInfoReliability == InformationReliability.KNOWN || obj.kitInfoReliability == InformationReliability.INFERRED) {
                return val != null
            } else {
                return val == null
            }
        })
    }

    public String toString() {
        return "${super.toString()} {kitInfoReliability} ${exomeEnrichmentKit}"
    }
}
