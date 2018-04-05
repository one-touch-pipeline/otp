package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.hibernate.*

/**
 * This bam file is produced by some Roddy alignment workflow.
 * The file is based on earlier created bam file (with the same workflow), if exists and
 * new SeqTracks which were not merged into the earlier created bam file (base bam file).
 */
class RoddyBamFile extends AbstractMergedBamFile implements RoddyResult, ProcessParameterObject {

    static final String WORK_DIR_PREFIX = ".merging"

    static final String QUALITY_CONTROL_DIR = "qualitycontrol"
    static final String METHYLATION_DIR = "methylation"

    static final String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"
    static final String QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME = "qualitycontrol_targetExtract.json"

    static final String MERGED_DIR = "merged"

    static final String METADATATABLE_FILE = "metadataTable.tsv"

    static final String INSERT_SIZE_FILE_SUFFIX = 'insertsize_plot.png_qcValues.txt'
    static final String INSERT_SIZE_FILE_DIRECTORY = 'insertsize_distribution'



    RoddyBamFile baseBamFile

    Set<SeqTrack> seqTracks

    static hasMany = [
            seqTracks: SeqTrack,
            roddyExecutionDirectoryNames: String,
    ]

    /**
     * config file used to create this bam file
     */
    RoddyWorkflowConfig config

    /**
     * unique identifier of this bam file in {@link RoddyBamFile#workPackage}
     */
    int identifier


    String workDirectoryName

    static constraints = {
        type validator: { true }
        seqTracks minSize: 1, validator: { val, obj, errors ->
            obj.isConsistentAndContainsNoWithdrawnData().each {
                errors.reject(null, it)
            }
        }
        baseBamFile nullable: true

        workPackage validator: { val, obj ->
            [Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Name.RODDY_RNA_ALIGNMENT].contains(val?.pipeline?.name) &&
                    MergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }

        config validator: { val, obj -> val?.pipeline?.id == obj.workPackage?.pipeline?.id }
        identifier unique: 'workPackage'
        roddyExecutionDirectoryNames nullable: true
        workDirectoryName nullable: true, unique: 'workPackage', validator: {
            it == null || OtpPath.isValidPathComponent(it)
        } //needs to be nullable for objects created before link structure was used
    }

    static mapping = {
        baseBamFile index: "roddy_bam_file_base_bam_file_idx"
        config index: "roddy_bam_file_config_idx"
    }

    List<String> isConsistentAndContainsNoWithdrawnData() {

        List<String> errors = []

        def assertAndTrackOnError = { def expression, String errorMessage ->
            if (!expression) {
                errors << errorMessage
            }
        }

        withNewSession { session ->
            if (baseBamFile) {
                assertAndTrackOnError !mergingWorkPackage || mergingWorkPackage.satisfiesCriteria(baseBamFile),
                        "the base bam file does not satisfy work package criteria"

                assertAndTrackOnError baseBamFile.md5sum != null,
                        "the base bam file is not finished"

                assertAndTrackOnError withdrawn || !baseBamFile.withdrawn,
                        "base bam file is withdrawn for not withdrawn bam file ${this}"

                List<Long> duplicatedSeqTracksIds = baseBamFile.containedSeqTracks*.id.intersect(seqTracks*.id)
                assertAndTrackOnError duplicatedSeqTracksIds.empty,
                        "the same seqTrack is going to be merged for the second time: ${seqTracks.findAll{duplicatedSeqTracksIds.contains(it.id)}}"
            }

            Set<SeqTrack>  allContainedSeqTracks = this.getContainedSeqTracks()

            assertAndTrackOnError withdrawn || !allContainedSeqTracks.any { it.withdrawn },
                    "not withdrawn bam file has withdrawn seq tracks"

            assertAndTrackOnError numberOfMergedLanes == allContainedSeqTracks.size(),
                    "total number of merged lanes is not equal to number of contained seq tracks: ${numberOfMergedLanes} vs ${allContainedSeqTracks.size()}"
        }


        return errors
    }

    Pipeline getPipeline() {
        return workPackage.pipeline
    }

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return MergingWorkPackage.get(workPackage?.id)
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        def tmpSet = baseBamFile?.containedSeqTracks ?: []
        tmpSet.addAll(seqTracks)
        return tmpSet as Set
    }

    QualityAssessmentMergedPass findOrSaveQaPass() {
        return QualityAssessmentMergedPass.findOrSaveWhere(
                abstractMergedBamFile: this,
                identifier: 0,
        )
    }

    boolean hasMultipleLibraries() {
        this.seqTracks*.libraryDirectoryName.unique().size() > 1
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        return CollectionUtils.exactlyOneElement(RoddyMergedBamQa.createCriteria().list {
            eq 'chromosome', RoddyQualityAssessment.ALL
            qualityAssessmentMergedPass {
                abstractMergedBamFile {
                    eq 'id', this.id
                }
            }
        })
    }

    @Override
    public boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    public static int nextIdentifier(final MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        final Integer maxIdentifier = maxIdentifier(mergingWorkPackage)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    public static Integer maxIdentifier(MergingWorkPackage workPackage) {
        return RoddyBamFile.createCriteria().get {
            eq("workPackage", workPackage)
            projections {
                max("identifier")
            }
        }
    }

    @Override
    String toString() {
        String latest = isMostRecentBamFile() ? ' (latest)' : ''
        String withdrawn = withdrawn ? ' (withdrawn)' : ''
        return "RBF ${id}: ${identifier}${latest}${withdrawn} ${mergingWorkPackage.toStringWithoutIdAndPipeline()}"
    }

    // Example: blood_somePid_merged.mdup.bam
    @Override
    public String getBamFileName() {
        String antiBodyTarget = seqType.isChipSeq() ? "-${((MergingWorkPackage)mergingWorkPackage).antibodyTarget.name}" : ''
        return "${sampleType.dirName}${antiBodyTarget}_${individual.pid}_merged.mdup.bam"
    }

    // Example: blood_somePid_merged.mdup.bam.bai
    public String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    // Example: blood_somePid_merged.mdup.bam.md5
    public String getMd5sumFileName() {
        return "${bamFileName}.md5"
    }

    // Example: ${otp.root.path}/${project}/sequencing/whole_genome_sequencing/view-by-pid/somePid/control/paired/merged-alignment/.merging_3
    File getWorkDirectory() {
        return new File(baseDirectory, workDirectoryName)
    }

    File getFinalQADirectory() {
        return new File(baseDirectory, QUALITY_CONTROL_DIR)
    }

    File getWorkQADirectory() {
        return new File(workDirectory, QUALITY_CONTROL_DIR)
    }

    File getWorkMethylationDirectory() {
        return new File(workDirectory, METHYLATION_DIR)
    }

    File getFinalMethylationDirectory() {
        return new File(baseDirectory, METHYLATION_DIR)
    }

    File getFinalMergedQADirectory() {
        return new File(this.finalQADirectory, MERGED_DIR)
    }

    File getWorkMergedQADirectory() {
        return new File(this.workQADirectory, MERGED_DIR)
    }

    Map<String, File> getFinalLibraryQADirectories() {
        return getLibraryDirectories(this.finalQADirectory)
    }

    Map<String, File> getWorkLibraryQADirectories() {
        return getLibraryDirectories(this.workQADirectory)
    }

    File getFinalMergedMethylationDirectory() {
        return new File(this.finalMethylationDirectory, MERGED_DIR)
    }

    File getWorkMergedMethylationDirectory() {
        return new File(this.workMethylationDirectory, MERGED_DIR)
    }

    Map<String, File> getFinalLibraryMethylationDirectories() {
        return getLibraryDirectories(this.finalMethylationDirectory)
    }

    Map<String, File> getWorkLibraryMethylationDirectories() {
        return getLibraryDirectories(this.workMethylationDirectory)
    }

    File getFinalMergedQAJsonFile() {
        return new File(finalMergedQADirectory, QUALITY_CONTROL_JSON_FILE_NAME)
    }

    File getWorkMergedQAJsonFile() {
        return new File(workMergedQADirectory, QUALITY_CONTROL_JSON_FILE_NAME)
    }

    File getWorkMergedQATargetExtractJsonFile() {
        return new File(workMergedQADirectory, QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME)
    }

    Map<SeqTrack, File> getFinalSingleLaneQADirectories() {
        return getSingleLaneQADirectoriesHelper(this.finalQADirectory)
    }

    Map<SeqTrack, File> getWorkSingleLaneQADirectories() {
        return getSingleLaneQADirectoriesHelper(this.workQADirectory)
    }

    Map<SeqTrack, File> getFinalSingleLaneQAJsonFiles() {
        return getSingleLaneQAJsonFiles('Final')
    }

    Map<SeqTrack, File> getWorkSingleLaneQAJsonFiles() {
        return getSingleLaneQAJsonFiles('Work')
    }

    private Map<String, File> getLibraryDirectories(File baseDirectory) {
        return seqTracks.collectEntries {
            [(it.libraryDirectoryName): new File(baseDirectory, it.libraryDirectoryName)]
        }
    }

    private Map<SeqTrack, File> getSingleLaneQAJsonFiles(String workOrFinal) {
        return "get${workOrFinal}SingleLaneQADirectories"().collectEntries { SeqTrack seqTrack, File directory ->
            [(seqTrack): new File(directory, QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    Map<String, File> getFinalLibraryQAJsonFiles() {
        return getLibraryQAJsonFiles('Final')
    }

    Map<String, File> getWorkLibraryQAJsonFiles() {
        return getLibraryQAJsonFiles('Work')
    }

    private Map<String, File> getLibraryQAJsonFiles(String workOrFinal) {
        return "get${workOrFinal}LibraryQADirectories"().collectEntries { String lib, File directory ->
            [(lib): new File(directory, QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    // Example: run140801_SN751_0197_AC4HUVACXX_D2059_AGTCAA_L001
    Map<SeqTrack, File> getSingleLaneQADirectoriesHelper(File baseDirectory) {
        Map<SeqTrack, File> directoriesPerSeqTrack = new HashMap<SeqTrack, File>()
        seqTracks.each { SeqTrack seqTrack ->
            String readGroupName = seqTrack.getReadGroupName()
            directoriesPerSeqTrack.put(seqTrack, new File(baseDirectory, readGroupName))
        }
        return directoriesPerSeqTrack
    }

    File getFinalExecutionStoreDirectory() {
        return new File(baseDirectory, RODDY_EXECUTION_STORE_DIR)
    }

    List<File> getFinalExecutionDirectories() {
        this.roddyExecutionDirectoryNames.collect {
            new File(this.finalExecutionStoreDirectory, it)
        }
    }

    File getFinalBamFile() {
        return new File(baseDirectory, this.bamFileName)
    }

    File getWorkBamFile() {
        return new File(workDirectory, this.bamFileName)
    }

    File getFinalBaiFile() {
        return new File(baseDirectory, this.baiFileName)
    }

    File getWorkBaiFile() {
        return new File(workDirectory, this.baiFileName)
    }

    File getFinalMd5sumFile() {
        return new File(baseDirectory, this.md5sumFileName)
    }

    File getWorkMd5sumFile() {
        return new File(workDirectory, this.md5sumFileName)
    }

    File getFinalMetadataTableFile() {
        return new File(baseDirectory, METADATATABLE_FILE)
    }

    File getWorkMetadataTableFile() {
        return new File(workDirectory, METADATATABLE_FILE)
    }

    File getFinalInsertSizeDirectory() {
        return new File(finalMergedQADirectory, INSERT_SIZE_FILE_DIRECTORY)
    }

    @Override
    File getFinalInsertSizeFile() {
        return new File(finalInsertSizeDirectory, "${this.sampleType.dirName}_${this.individual.pid}_${INSERT_SIZE_FILE_SUFFIX}")
    }

    /**
     * returns whether the old or the new file structure is used.
     * <ul>
     *     <li>old file structure: in the old structure the processing was done in a temporary work directory. After
     *     finishing the processing the files to keep were moved to there final place. At the end the temporary directory
     *     was deleted.</li>
     *     <li>new structure: The new structure is designed to not move files. Therefore a permanent work directory is
     *     used to create files. After creation the files and directories are linked to the final place.</li>
     * </ul>
     *
     * @return true if the old structure is used.
     */
    boolean isOldStructureUsed() {
        return !workDirectoryName
    }



    /**
     * return for old structure the final bam file and for the new structure the work bam file
     */
    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return isOldStructureUsed() ? finalBamFile : workBamFile
    }



    void withdraw() {
        withTransaction {
            //get later bam files
            RoddyBamFile.findAllByBaseBamFile(this).each {
                it.withdraw()
            }

            super.withdraw()
        }
    }


    Long getNumberOfReadsFromQa() {
        AbstractQualityAssessment qa = getOverallQualityAssessment()
        return qa.pairedRead1 + qa.pairedRead2
    }

    Long getNumberOfReadsFromFastQc() {
        Set<SeqTrack> seqTracks = containedSeqTracks
        assert seqTracks.size() == numberOfMergedLanes : "Found ${seqTracks.size()} SeqTracks, but expected ${numberOfMergedLanes}"

        assert seqTracks.every { SeqTrack seqTrack ->
            seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
        } : "Not all Fastqc workflows of all seqtracks are finished"

        List<Integer> numberOfReads = seqTracks.collect { SeqTrack seqTrack ->
            seqTrack.NReads
        }

        assert !numberOfReads.contains(null) : 'At least one seqTrack has no value for number of reads'

        return numberOfReads.sum()
    }

    @Override
    AlignmentConfig getAlignmentConfig() {
        return config
    }

    Integer getMaximalReadLength() {
        return getSeqTracks()*.dataFiles.flatten().max { it.meanSequenceLength }.meanSequenceLength
    }
}
