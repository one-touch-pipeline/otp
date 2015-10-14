package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.CollectionUtils


class SeqType {

    final static LIBRARYLAYOUT_PAIRED = "PAIRED"

    final static LIBRARYLAYOUT_SINGLE = "SINGLE"

    final static LIBRARYLAYOUT_MATE_PAIR = "MATE_PAIR"

    /**
     * One of {@link SeqTypeNames#seqTypeName}.
     * Used in file system paths, for example by ProcessedMergedBamFileService.fileNameNoSuffix(ProcessedMergedBamFile).
     * Used in the GUI unless {@link #alias} is set.
     */
    String name
    String libraryLayout
    String dirName
    /**
     * Can be set to override {@link #name} for display in the GUI.
     */
    String alias
    /**
     * Display name used in the GUI.
     */
    String aliasOrName

    static constraints = {
        name(blank: false)
        libraryLayout(blank: false, validator: { OtpPath.isValidPathComponent(it) })  // TODO: OTP-1123: unique constraint for (name, libraryLayout)
        dirName(blank: false, unique: 'libraryLayout', validator: { OtpPath.isValidPathComponent(it) })  // TODO: OTP-1124: unique constraint for (dirName, libraryLayoutDirName)
        alias(nullable: true, blank: false)
        //For unknown reason the object creation fail, if it is not set as nullable
        aliasOrName(nullable: true, blank: false)
    }

    static mapping = {
        aliasOrName formula: '(CASE WHEN alias IS NOT NULL THEN alias ELSE name END)'
    }

    /**
     * Retrieves the unique natural Id (human readable)
     * Should not be changed, since this is stored at the database.
     */
    String getNaturalId() {
        return "${name}_${libraryLayout}"
    }

    String getLibraryLayoutDirName() {
        return getLibraryLayoutDirName(libraryLayout)
    }

    static String getLibraryLayoutDirName(final String libraryLayout) {
        return libraryLayout.toLowerCase()
    }

    SeqTypeNames getSeqTypeName() {
        return SeqTypeNames.fromSeqTypeName(name)
    }

    Class<? extends SeqTrack> getSeqTrackClass() {
        return seqTypeName?.seqTrackClass ?: SeqTrack
    }

    String toString() {
        "${aliasOrName} ${libraryLayout}"
    }


    static SeqType getWholeGenomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                SeqType.findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
        )
    }

    static SeqType getExomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                SeqType.findAllByAliasAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
        )
    }
}
