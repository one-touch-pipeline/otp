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

    public final static SINGLE_CELL_DNA = "Single-cell DNA"

    public final static SINGLE_CELL_RNA = "Single-cell RNA"

    public static final TAGMENTATION_SUFFIX = '_TAGMENTATION'

    public static final Collection<SeqTypeNames> WGBS_SEQ_TYPE_NAMES = [
            SeqTypeNames.WHOLE_GENOME_BISULFITE,
            SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION,
    ].asImmutable()

    // files with these seqTypes must be copied because the corresponding workflows don't support incremental merging
    // TODO OTP-2726
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

    boolean singleCell

    /**
     * Display name used in the GUI.
     */
    String displayName

    /** name used in roddy config files */
    String roddyName

    static hasMany = [importAlias : String]

    static constraints = {
        name(blank: false)
        libraryLayout(blank: false, unique: ['name', 'singleCell'], validator: { OtpPath.isValidPathComponent(it) })
        dirName(blank: false, unique: ['libraryLayout', 'singleCell'], validator: { String val, SeqType obj ->
            if (!OtpPath.isValidPathComponent(val)) {
                return "no valid path component"
            }
            if (SeqType.findByDirNameAndSingleCell(obj.dirName, !obj.singleCell)) {
                return "dir name constraint"
            }
            if (SeqType.findByNameAndSingleCell(obj.name, obj.singleCell)) {
                if (SeqType.findByNameAndSingleCell(obj.name, obj.singleCell).dirName != obj.dirName) {
                    return "for same name and single cell, the dir name should be the same"
                }
            }
        })
        // TODO: OTP-1124: unique constraint for (dirName, libraryLayoutDirName)
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
        return "${displayName} ${libraryLayout} ${singleCell ? 'singleCell' : 'bulk'}"
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

    @Override
    String toString() {
        return displayNameWithLibraryLayout
    }


    static SeqType getWholeGenomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.WHOLE_GENOME.seqTypeName, LIBRARYLAYOUT_PAIRED, false), 'WGS PAIRED not found'
        )
    }

    static SeqType getExomePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.EXOME.seqTypeName, LIBRARYLAYOUT_PAIRED, false), 'WES PAIRED not found'
        )
    }

    static SeqType getWholeGenomeBisulfitePairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, LIBRARYLAYOUT_PAIRED, false), 'WGBS PAIRED not found'
        )
    }

    static SeqType getWholeGenomeBisulfiteTagmentationPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, LIBRARYLAYOUT_PAIRED, false), 'WGBS_TAG PAIRED not found'
        )
    }

    static SeqType getRnaPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.RNA.seqTypeName, LIBRARYLAYOUT_PAIRED, false), 'RNA PAIRED not found'
        )
    }

    static SeqType getChipSeqPairedSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.CHIP_SEQ.seqTypeName, LIBRARYLAYOUT_PAIRED, false), 'CHIP_SEQ PAIRED not found'
        )
    }

    static SeqType getRnaSingleSeqType() {
        return CollectionUtils.exactlyOneElement(
                findAllByNameAndLibraryLayoutAndSingleCell(SeqTypeNames.RNA.seqTypeName, LIBRARYLAYOUT_SINGLE, false), 'RNA SINGLE not found'
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
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getAceseqPipelineSeqTypes() {
        return [
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getRunYapsaPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getAllAnalysableSeqTypes() {
        return [
                getSnvPipelineSeqTypes(),
                getIndelPipelineSeqTypes(),
                getSophiaPipelineSeqTypes(),
                getAceseqPipelineSeqTypes(),
                getRunYapsaPipelineSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> getAllProcessableSeqTypes() {
        return [
                getAllAlignableSeqTypes(),
                getAllAnalysableSeqTypes(),
        ].flatten().unique()
    }


    static List<SeqType> getSeqTypesIgnoringLibraryPreparationKitForMerging() {
        return [
                getWholeGenomeBisulfitePairedSeqType(),
                getWholeGenomeBisulfiteTagmentationPairedSeqType(),
        ].flatten().unique()
    }
}
