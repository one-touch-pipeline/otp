package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.utils.StringUtils

/**
 * This bam file is produced by some Roddy alignment workflow.
 * The file is based on earlier created bam file (with the same workflow), if exists and
 * new SeqTracks which were not merged into the earlier created bam file (base bam file).
 */
class RoddyBamFile extends AbstractMergedBamFile implements RoddyResult, ProcessParameterObject {

    /*
     * the value is also used in a check in the bash script: 'bashScripts/OtherUnixUser/checkInputParameter.sh'.
     * If the value is changed here, it needs also to be changed there.
     */
    static final String WORK_DIR_PREFIX = ".merging"

    static final String QUALITY_CONTROL_DIR = "qualitycontrol"

    static final String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"
    static final String QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME = "qualitycontrol_targetExtract.json"

    static final String RODDY_EXECUTION_STORE_DIR = "roddyExecutionStore"

    static final String MERGED_DIR = "merged"

    static final String RUN_PREFIX = "run"

    static final String RODDY_EXECUTION_DIR_PATTERN = /exec_\d{6}_\d{8,9}_.+_.+/

    RoddyBamFile baseBamFile

    List<String> roddyExecutionDirectoryNames = []

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

    /**
     * Roddy version which was used to produce this RoddyBamFile
     */
    ProcessingOption roddyVersion


    String workDirectoryName

    static constraints = {
        type validator: { true }
        seqTracks minSize: 1, validator: { val, obj, errors ->
            obj.isConsistentAndContainsNoWithdrawnData().each {
                errors.reject(null, it)
            }
        }
        baseBamFile nullable: true
        workPackage validator: { val, obj -> val?.workflow?.name == Workflow.Name.PANCAN_ALIGNMENT }
        config validator: { val, obj -> val?.workflow?.id == obj.workPackage?.workflow?.id }
        identifier unique: 'workPackage'
        roddyExecutionDirectoryNames nullable: true
        workDirectoryName nullable: true, unique: 'workPackage', validator: { it == null || OtpPath.isValidPathComponent(it) } //needs to be nullable for objects created before link structure was used
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

        seqTracks.each {
            assertAndTrackOnError !mergingWorkPackage || mergingWorkPackage.satisfiesCriteria(it),
                    "seqTrack ${it} does not satisfy merging criteria for ${mergingWorkPackage}"
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

    Workflow getWorkflow() {
        return workPackage.workflow
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
        return identifier == maxIdentifier(workPackage)
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
        return "RBF ${id}: ${identifier}${latest}${withdrawn} ${mergingWorkPackage.toStringWithoutIdAndWorkflow()}"
    }

    // Example: blood_somePid_merged.mdup.bam
    @Override
    public String getBamFileName() {
        return "${sampleType.dirName}_${individual.pid}_merged.mdup.bam"
    }

    // Example: blood_somePid_merged.mdup.bam.bai
    public String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    // Example: blood_somePid_merged.mdup.bam.md5
    public String getMd5sumFileName() {
        return "${bamFileName}.md5"
    }

    // Example: $OTP_ROOT_PATH/${project}/sequencing/whole_genome_sequencing/view-by-pid/somePid/control/paired/merged-alignment/.merging_3
    File getWorkDirectory() {
        return new File(baseDirectory, workDirectoryName)
    }

    File getFinalQADirectory() {
        return new File(baseDirectory, QUALITY_CONTROL_DIR)
    }

    File getWorkQADirectory() {
        return new File(workDirectory, QUALITY_CONTROL_DIR)
    }

    File getFinalMergedQADirectory() {
        return new File(this.finalQADirectory, MERGED_DIR)
    }

    File getWorkMergedQADirectory() {
        return new File(this.workQADirectory, MERGED_DIR)
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

    private Map<SeqTrack, File> getSingleLaneQAJsonFiles(String workOrFinal) {
        return "get${workOrFinal}SingleLaneQADirectories"().collectEntries { SeqTrack seqTrack, File directory ->
            [(seqTrack): new File(directory, QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    // Example: run140801_SN751_0197_AC4HUVACXX_D2059_AGTCAA_L001
    Map<SeqTrack, File> getSingleLaneQADirectoriesHelper(File baseDirectory) {
        Map<SeqTrack, File> directoriesPerSeqTrack = new HashMap<SeqTrack, File>()
        seqTracks.each { SeqTrack seqTrack ->
            String readGroupName = getReadGroupName(seqTrack)
            directoriesPerSeqTrack.put(seqTrack, new File(baseDirectory, readGroupName))
        }
        return directoriesPerSeqTrack
    }

    File getFinalExecutionStoreDirectory() {
        return new File(baseDirectory, RODDY_EXECUTION_STORE_DIR)
    }

    File getWorkExecutionStoreDirectory() {
        return new File(workDirectory, RODDY_EXECUTION_STORE_DIR)
    }

    List<File> getFinalExecutionDirectories() {
        this.roddyExecutionDirectoryNames.collect {
            new File(this.finalExecutionStoreDirectory, it)
        }
    }

    List<File> getWorkExecutionDirectories() {
        this.roddyExecutionDirectoryNames.collect {
            new File(this.workExecutionStoreDirectory, it)
        }
    }

    /**
     @returns subdirectory of {@link #getWorkExecutionStoreDirectory} corresponding to the latest roddy call
     */
    // Example:
    // exec_150625_102449388_SOMEUSER_WGS
    // exec_yyMMdd_HHmmssSSS_user_analysis
    File getLatestWorkExecutionDirectory() {
        if (!roddyExecutionDirectoryNames) {
            throw new RuntimeException("No roddyExecutionDirectoryNames have been stored in the database for ${this}.")
        }

        String latestDirectoryName = roddyExecutionDirectoryNames.last()
        assert latestDirectoryName == roddyExecutionDirectoryNames.max()
        assert latestDirectoryName ==~ RODDY_EXECUTION_DIR_PATTERN

        File latestWorkDirectory = new File(workExecutionStoreDirectory, latestDirectoryName)
        assert WaitingFileUtils.waitUntilExists(latestWorkDirectory)
        assert latestWorkDirectory.isDirectory()

        return latestWorkDirectory
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
    File getPathForFurtherProcessing() {
        if (this.id == mergingWorkPackage.processableBamFileInProjectFolder?.id) {
            return isOldStructureUsed() ? finalBamFile : workBamFile
        } else {
            throw new IllegalStateException()
        }
    }



    void withdraw() {
        withTransaction {
            //find snv and make them withdrawn
            super.withdrawCorrespondingSnvResults()

            //get later bam files
            RoddyBamFile.findAllByBaseBamFile(this).each {
                it.withdraw()
            }

            LogThreadLocal.threadLog.info "Execute WithdrawnFilesRename.groovy script afterwards"
            LogThreadLocal.threadLog.info "Withdrawing ${this}"
            withdrawn = true
            assert save(flush: true)
        }
    }

    static String getReadGroupName(SeqTrack seqTrack) {
        assert seqTrack : "The input seqTrack must not be null"
        Run run = seqTrack.run
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        assert dataFiles.size() == 2
        //if the names of datafile1 and datafile2 of one seqtrack are the same something strange happened -> should fail
        assert !dataFiles[0].fileName.equals(dataFiles[1].fileName)
        String commonFastQFilePrefix = getLongestCommonPrefixBeforeLastUnderscore(dataFiles[0].fileName, dataFiles[1].fileName)
        return "${RUN_PREFIX}${run.name}_${commonFastQFilePrefix}"
    }

    static String getLongestCommonPrefixBeforeLastUnderscore(String filename1, String filename2) {
        assert filename1 : "The input filename1 must not be null"
        assert filename2 : "The input filename2 must not be null"
        String commonFastqFilePrefix = StringUtils.longestCommonPrefix(filename1, filename2)
        String pattern = /^(.*)_([^_]*)$/
        def matcher = commonFastqFilePrefix =~ pattern
        if (matcher.matches()) {
            return matcher.group(1)
        } else {
            return commonFastqFilePrefix

        }
    }
}
