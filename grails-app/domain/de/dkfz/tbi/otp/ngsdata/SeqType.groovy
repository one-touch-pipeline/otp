package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*

/*
 * In the GUI and e-mails sent by OTP this shall be called "Sequencing Type" (or "Seq Type" where little space is
 * available), no matter if the library layout is included in the displayed value or not. (Decided together with the OTP
 * Product Owner on 2016-07-19.)
 */
class SeqType implements Entity {

    final static LIBRARYLAYOUT_PAIRED = LibraryLayout.PAIRED.name()

    final static LIBRARYLAYOUT_SINGLE = LibraryLayout.SINGLE.name()

    final static LIBRARYLAYOUT_MATE_PAIR = LibraryLayout.MATE_PAIR.name()

    public static final TAGMENTATION_SUFFIX = '_TAGMENTATION'

    public static final Collection<SeqTypeNames> WGBS_SEQ_TYPE_NAMES = [
            SeqTypeNames.WHOLE_GENOME_BISULFITE,
            SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION,
    ].asImmutable()

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
    String displayName
    /** name used in roddy config files */
    String roddyName


    static constraints = {
        name(blank: false)
        libraryLayout(blank: false, validator: { OtpPath.isValidPathComponent(it) })  // TODO: OTP-1123: unique constraint for (name, libraryLayout)
        dirName(blank: false, unique: 'libraryLayout', validator: { OtpPath.isValidPathComponent(it) })  // TODO: OTP-1124: unique constraint for (dirName, libraryLayoutDirName)
        alias(nullable: true, blank: false)
        //For unknown reason the object creation fail, if it is not set as nullable
        displayName(nullable: true, blank: false)
        roddyName(nullable: true, blank: false, validator: {
            !it?.contains('_')  // Roddy has problems with underscores
        })
    }

    static mapping = {
        displayName formula: '(CASE WHEN alias IS NOT NULL THEN alias ELSE name END)'
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

    String getProcessingOptionName() {
        return alias ?: name
    }

    SeqTypeNames getSeqTypeName() {
        return SeqTypeNames.fromSeqTypeName(name)
    }

    Class<? extends SeqTrack> getSeqTrackClass() {
        return seqTypeName?.seqTrackClass ?: SeqTrack
    }

    boolean isWgbs() {
        return WGBS_SEQ_TYPE_NAMES.contains(seqTypeName)
    }

    String toString() {
        "${displayName} ${libraryLayout}"
    }


    static SeqType getWholeGenomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME.seqTypeName, LIBRARYLAYOUT_PAIRED)
        )
    }

    static SeqType getExomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, LIBRARYLAYOUT_PAIRED)
        )
    }

    static SeqType getWholeGenomeBisulfitePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, LIBRARYLAYOUT_PAIRED)
        )
    }

    static SeqType getWholeGenomeBisulfiteTagmentationPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, LIBRARYLAYOUT_PAIRED)
        )
    }

    static List<SeqType> getDefaultOtpAlignableSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getPanCanAlignableSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
                getWholeGenomeBisulfitePairedSeqType(),
                getWholeGenomeBisulfiteTagmentationPairedSeqType(),
        ]
    }

    static List<SeqType> getAllAlignableSeqTypes() {
        return [
                getDefaultOtpAlignableSeqTypes(),
                getPanCanAlignableSeqTypes(),
        ].flatten().unique()
    }
}
