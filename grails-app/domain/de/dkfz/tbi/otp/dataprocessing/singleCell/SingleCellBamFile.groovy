package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

@SuppressWarnings('JavaIoPackageAccess')
class SingleCellBamFile extends AbstractMergedBamFile implements HasIdentifier, ProcessParameterObject {

    static final String INPUT_DIRECTORY_NAME = 'cell-ranger-input'

    static final String OUTPUT_DIRECTORY_NAME = 'out'

    static final String ORIGINAL_BAM_FILE_NAME = 'possorted_genome_bam.bam'

    static final String ORIGINAL_BAI_FILE_NAME = 'possorted_genome_bam.bam.bai'

    //is created manually
    static final String ORIGINAL_BAM_MD5SUM_FILE_NAME = 'possorted_genome_bam.md5sum'

    static final String METRICS_SUMMARY_CSV_FILE_NAME = "metrics_summary.csv"


    static final List<String> CREATED_RESULT_FILES = [
            'web_summary.html',
            METRICS_SUMMARY_CSV_FILE_NAME,
            ORIGINAL_BAM_FILE_NAME,
            ORIGINAL_BAI_FILE_NAME,
            ORIGINAL_BAM_MD5SUM_FILE_NAME,
            'filtered_gene_bc_matrices_h5.h5',
            'raw_gene_bc_matrices_h5.h5',
            'molecule_info.h5',
            'cloupe.cloupe',
    ].asImmutable()

    static final List<String> CREATED_RESULT_DIRS = [
            'filtered_gene_bc_matrices',
            'raw_gene_bc_matrices',
            'analysis',
    ].asImmutable()

    static final List<String> CREATED_RESULT_FILES_AND_DIRS = [
            CREATED_RESULT_FILES,
            CREATED_RESULT_DIRS,
    ].flatten().asImmutable()


    Set<SeqTrack> seqTracks

    String workDirectoryName

    static hasMany = [
            seqTracks: SeqTrack,
    ]

    static constraints = {
        workDirectoryName validator: { val, obj ->
            OtpPath.isValidRelativePath(val) &&
                    !SingleCellBamFile.findAllByWorkDirectoryName(val).any {
                        it != obj && it.workPackage == obj.workPackage
                    }
        }
        seqTracks minSize: 1
        identifier validator: { val, obj ->
            !SingleCellBamFile.findAllByIdentifier(val).any {
                it != obj && it.workPackage == obj.workPackage
            }
        }
    }

    @Override
    boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    @Override
    String getBamFileName() {
        String antiBodyTarget = seqType.isChipSeq() ? "-${((MergingWorkPackage) mergingWorkPackage).antibodyTarget.name}" : ''
        return "${sampleType.dirName}${antiBodyTarget}_${individual.pid}_merged.mdup.bam"
    }

    @Override
    String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    String getMd5SumFileName() {
        return "${bamFileName}.md5"
    }

    @Override
    AlignmentConfig getAlignmentConfig() {
        return mergingWorkPackage.config
    }

    File getWorkDirectory() {
        return new File(baseDirectory, workDirectoryName)
    }

    String getSingleCellSampleName() {
        return "${individual.pid}_${sampleType.dirName}"
    }

    File getSampleDirectory() {
        return new File(new File(workDirectory, INPUT_DIRECTORY_NAME), singleCellSampleName)
    }

    File getOutputDirectory() {
        return new File(workDirectory, singleCellSampleName)
    }

    File getResultDirectory() {
        return new File(outputDirectory, OUTPUT_DIRECTORY_NAME)
    }

    /**
     * Map of names to use for link and name used by CellRanger
     */
    Map<String, String> getFileMappingForLinks() {
        CREATED_RESULT_FILES_AND_DIRS.collectEntries {
            [(getLinkNameForFile(it)): it]
        }
    }

    /**
     * list of linked files
     */
    List<File> getLinkedResultFiles() {
        File result = workDirectory
        return CREATED_RESULT_FILES_AND_DIRS.collect {
            new File(result, getLinkNameForFile(it))
        }
    }

    /**
     * return the name to use for the links of the result file, because the bam file should be named differently
     */
    private String getLinkNameForFile(String name) {
        switch (name) {
            case ORIGINAL_BAM_FILE_NAME:
                return bamFileName
            case ORIGINAL_BAI_FILE_NAME:
                return baiFileName
            case ORIGINAL_BAM_MD5SUM_FILE_NAME:
                return md5SumFileName
            default:
                return name
        }
    }


    @Override
    File getFinalInsertSizeFile() {
        throw new MissingPropertyException("Final insert size file is not implemented for single cell BAM files")
    }

    @Override
    Integer getMaximalReadLength() {
        throw new MissingPropertyException("Maximal read length is not implemented for single cell BAM files")
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(workDirectory, bamFileName)
    }

    File getQualityAssessmentCsvFile() {
        return new File(resultDirectory, METRICS_SUMMARY_CSV_FILE_NAME)
    }

    @Override
    CellRangerMergingWorkPackage getMergingWorkPackage() {
        return CellRangerMergingWorkPackage.get(workPackage?.id)
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return seqTracks
    }

    QualityAssessmentMergedPass findOrSaveQaPass() {
        return QualityAssessmentMergedPass.findOrSaveWhere(
                abstractMergedBamFile: this,
                identifier: 0,
        )
    }

    @Override
    CellRangerQualityAssessment getOverallQualityAssessment() {
        CellRangerQualityAssessment.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'abstractMergedBamFile', this
                eq 'identifier', 0
            }
        } as CellRangerQualityAssessment
    }
}
