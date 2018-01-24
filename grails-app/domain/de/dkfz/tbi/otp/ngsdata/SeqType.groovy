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

    public static final Collection<SeqTypeNames> SEQTYPES_MUST_BE_COPIED = WGBS_SEQ_TYPE_NAMES + [
            SeqTypeNames.RNA,
            SeqTypeNames.CHIP_SEQ,
    ].asImmutable()

    /**
     * One of {@link SeqTypeNames#seqTypeName}.
     * Used in file system paths, for example by ProcessedMergedBamFileService.fileNameNoSuffix(ProcessedMergedBamFile).
     */
    String name
    String libraryLayout
    String dirName
    /**
     * Display name used in the GUI.
     */
    String displayName

    /** name used in roddy config files */
    String roddyName

    static hasMany = [alias : String]

    static constraints = {
        name(blank: false)
        libraryLayout(blank: false, unique: 'name', validator: { OtpPath.isValidPathComponent(it) })
        dirName(blank: false, unique: 'libraryLayout', validator: { OtpPath.isValidPathComponent(it) })  // TODO: OTP-1124: unique constraint for (dirName, libraryLayoutDirName)
        displayName(blank: false)
        roddyName(nullable: true, blank: false, validator: {
            !it?.contains('_')  // Roddy has problems with underscores
        })
    }

    /**
     * Retrieves the unique natural Id (human readable)
     * Should not be changed, since this is stored at the database.
     */
    String getNaturalId() {
        return "${name}_${libraryLayout}"
    }

    String getNameWithLibraryLayout() {
        return "${name} ${libraryLayout}"
    }

    String getDisplayNameWithLibraryLayout() {
        return "${displayName} ${libraryLayout}"
    }

    String getLibraryLayoutDirName() {
        return getLibraryLayoutDirName(libraryLayout)
    }

    static String getLibraryLayoutDirName(final String libraryLayout) {
        return libraryLayout.toLowerCase()
    }

    String getProcessingOptionName() {
        return displayName ?: name
    }

    SeqTypeNames getSeqTypeName() {
        return SeqTypeNames.fromSeqTypeName(name)
    }

    Class<? extends SeqTrack> getSeqTrackClass() {
        return seqTypeName?.seqTrackClass ?: SeqTrack
    }

    boolean isExome() {
        return name == SeqTypeNames.EXOME.seqTypeName
    }

    boolean isWgbs() {
        return WGBS_SEQ_TYPE_NAMES.contains(seqTypeName)
    }

    boolean isRna() {
        return name == SeqTypeNames.RNA.seqTypeName
    }

    boolean isChipSeq() {
        return name == SeqTypeNames.CHIP_SEQ.seqTypeName
    }

    boolean seqTypeAllowsLinking() {
        return !SEQTYPES_MUST_BE_COPIED.contains(seqTypeName)
    }

    String toString() {
        return displayNameWithLibraryLayout
    }


    static SeqType getWholeGenomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME.seqTypeName, LIBRARYLAYOUT_PAIRED), 'WGS PAIRED not found'
        )
    }

    static SeqType getExomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, LIBRARYLAYOUT_PAIRED), 'WES PAIRED not found'
        )
    }

    static SeqType getWholeGenomeBisulfitePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, LIBRARYLAYOUT_PAIRED), 'WGBS PAIRED not found'
        )
    }

    static SeqType getWholeGenomeBisulfiteTagmentationPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, LIBRARYLAYOUT_PAIRED), 'WGBS_TAG PAIRED not found'
        )
    }

    static SeqType getRnaPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.RNA.seqTypeName, LIBRARYLAYOUT_PAIRED), 'RNA PAIRED not found'
        )
    }

    static SeqType getChipSeqPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.CHIP_SEQ.seqTypeName, LIBRARYLAYOUT_PAIRED), 'CHIP_SEQ PAIRED not found'
        )
    }

    static SeqType getRnaSingleSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayout(SeqTypeNames.RNA.seqTypeName, LIBRARYLAYOUT_SINGLE), 'RNA SINGLE not found'
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
                getChipSeqPairedSeqType(),
        ]
    }

    static List<SeqType> getRnaAlignableSeqTypes() {
        return [
                getRnaPairedSeqType(),
                getRnaSingleSeqType(),
        ]
    }

    static List<SeqType> getRoddyAlignableSeqTypes() {
        return [
                getPanCanAlignableSeqTypes(),
                getRnaAlignableSeqTypes(),
        ].flatten()
    }

    static List<SeqType> getAllAlignableSeqTypes() {
        return [
                getDefaultOtpAlignableSeqTypes(),
                getRoddyAlignableSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesRequiredLibPrepKit() {
        return [
                exomePairedSeqType,
                wholeGenomeBisulfitePairedSeqType,
                wholeGenomeBisulfiteTagmentationPairedSeqType,
                chipSeqPairedSeqType,
                rnaPairedSeqType,
                rnaSingleSeqType,
        ]
    }

    static List<SeqType> getSnvPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getIndelPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getSophiaPipelineSeqTypes() {
        return [
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getAceseqPipelineSeqTypes() {
        return [
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getAllAnalysableSeqTypes() {
        return [
                getSnvPipelineSeqTypes(),
                getIndelPipelineSeqTypes(),
                getSophiaPipelineSeqTypes(),
                getAceseqPipelineSeqTypes(),
        ].flatten().unique()
    }


    static List<SeqType> getSeqTypesIgnoringLibraryPreparationKitForMerging() {
        return [
                getWholeGenomeBisulfitePairedSeqType(),
                getWholeGenomeBisulfiteTagmentationPairedSeqType(),
        ].flatten().unique()
    }
}
