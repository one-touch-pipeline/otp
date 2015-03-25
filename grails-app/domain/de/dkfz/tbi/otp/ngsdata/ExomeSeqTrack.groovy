package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.InformationReliability

class ExomeSeqTrack extends SeqTrack {
    /**
     * Holds the information about the state of {@link #libraryPreparationKit}.
     * If the value is {@link InformationReliability#KNOWN}, the {@link #libraryPreparationKit} needs to be there,
     * in any other case the {@link #libraryPreparationKit} have to be <code>null</code>.
     * The default value is {@link InformationReliability#UNKNOWN_UNVERIFIED}
     */
    InformationReliability kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED

    static belongsTo = [
        /**
         * Reference to the used {@link LibraryPreparationKit}.
         * If present, the value of {link #kitInfoReliability} has to be {@link kitInfoReliability#KNOWN}
         */
        libraryPreparationKit: LibraryPreparationKit
    ]

    static constraints = {
        kitInfoReliability(nullable: false)
        libraryPreparationKit(nullable: true, validator: { LibraryPreparationKit val, ExomeSeqTrack obj ->
            if (obj.kitInfoReliability == InformationReliability.KNOWN || obj.kitInfoReliability == InformationReliability.INFERRED) {
                return val != null
            } else {
                return val == null
            }
        })
    }

    public String toString() {
        return "${super.toString()} ${kitInfoReliability} ${libraryPreparationKit}"
    }

    static mapping = {
        libraryPreparationKit index: "seq_track_library_preparation_kit_idx"
    }
}
