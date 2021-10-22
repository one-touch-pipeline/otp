/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import org.hibernate.Hibernate

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.HasIdentifier
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

/**
 * This bam file is produced by some Roddy alignment workflow.
 * The file is based on earlier created bam file (with the same workflow), if exists and
 * new SeqTracks which were not merged into the earlier created bam file (base bam file).
 */
class RoddyBamFile extends AbstractMergedBamFile implements Artefact, HasIdentifier , ProcessParameterObject, RoddyResult {

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
            seqTracks                   : SeqTrack,
            roddyExecutionDirectoryNames: String,
    ]

    /**
     * config file used to create this bam file
     */
    RoddyWorkflowConfig config

    String workDirectoryName

    static constraints = {
        seqTracks minSize: 1, validator: { val, obj, errors ->
            obj.isConsistentAndContainsNoWithdrawnData().each {
                errors.reject(null, it)
            }
        }
        baseBamFile nullable: true
        workflowArtefact nullable: true

        workPackage validator: { val, obj ->
            [Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Name.RODDY_RNA_ALIGNMENT].contains(val?.pipeline?.name) &&
                    MergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }

        config validator: { val, obj -> val?.pipeline?.id == obj.workPackage?.pipeline?.id }
        identifier validator: { val, obj ->
            !RoddyBamFile.findAllByWorkPackageAndIdentifierAndIdNotEqual(obj.workPackage, val, obj.id)
        }
        roddyExecutionDirectoryNames nullable: true
        workDirectoryName nullable: true, validator: { val, obj ->
            (val == null || (OtpPathValidator.isValidPathComponent(val) &&
                    !RoddyBamFile.findAllByWorkPackageAndWorkDirectoryNameAndIdNotEqual(obj.workPackage, val, obj.id)))
        } //needs to be nullable for objects created before link structure was used
        md5sum validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED && obj.fileSize > 0))
        }
        fileOperationStatus validator: { val, obj ->
            return (val == FileOperationStatus.PROCESSED) == (obj.md5sum != null)
        }
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

        SessionUtils.withNewSession { session ->
            if (baseBamFile) {
                assertAndTrackOnError !mergingWorkPackage || mergingWorkPackage.satisfiesCriteria(baseBamFile),
                        "the base bam file does not satisfy work package criteria"

                assertAndTrackOnError baseBamFile.md5sum != null,
                        "the base bam file is not finished"

                assertAndTrackOnError withdrawn || !baseBamFile.withdrawn,
                        "base bam file is withdrawn for not withdrawn bam file ${this}"

                List<Long> duplicatedSeqTracksIds = baseBamFile.containedSeqTracks*.id.intersect(seqTracks*.id)
                assertAndTrackOnError duplicatedSeqTracksIds.empty,
                        "the same seqTrack is going to be merged for the second time: ${seqTracks.findAll { duplicatedSeqTracksIds.contains(it.id) }}"
            }

            Set<SeqTrack> allContainedSeqTracks = this.getContainedSeqTracks()

            assertAndTrackOnError withdrawn || !allContainedSeqTracks.any { it.withdrawn },
                    "not withdrawn bam file has withdrawn seq tracks"

            assertAndTrackOnError numberOfMergedLanes == allContainedSeqTracks.size(),
                    "total number of merged lanes is not equal to number of contained seq tracks: ${numberOfMergedLanes} vs ${allContainedSeqTracks.size()}"
        }

        return errors
    }

    @Override
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
        QualityAssessmentMergedPass assessmentMergedPass = QualityAssessmentMergedPass.findWhere(
                abstractMergedBamFile: this,
                identifier: 0,
        )
        if (!assessmentMergedPass) {
            assessmentMergedPass = new QualityAssessmentMergedPass(
                    abstractMergedBamFile: this,
                    identifier: 0,
            )
            assessmentMergedPass.save(flush: true)
        }
        return assessmentMergedPass
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
    boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    @Override
    String toString() {
        String latest = mergingWorkPackage ? (isMostRecentBamFile() ? ' (latest)' : '') : '?'
        String withdrawn = withdrawn ? ' (withdrawn)' : ''
        return "RBF ${id}: ${identifier}${latest}${withdrawn} ${qcTrafficLightStatus} ${mergingWorkPackage.toStringWithoutIdAndPipeline()}"
    }

    // Example: blood_somePid_merged.mdup.bam
    @Override
    String getBamFileName() {
        String antiBodyTarget = seqType.hasAntibodyTarget ? "-${((MergingWorkPackage) mergingWorkPackage).antibodyTarget.name}" : ''
        return "${sampleType.dirName}${antiBodyTarget}_${individual.pid}_merged.mdup.bam"
    }

    // Example: blood_somePid_merged.mdup.bam.bai
    @Override
    String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    // Example: blood_somePid_merged.mdup.bam.md5
    String getMd5sumFileName() {
        return "${bamFileName}.md5"
    }

    // Example: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/whole_genome_sequencing/view-by-pid/somePid/control/paired/merged-alignment/.merging_3
    @Override
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
        Map<SeqTrack, File> directoriesPerSeqTrack = [:]
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

    @Override
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
        assert seqTracks.size() == numberOfMergedLanes: "Found ${seqTracks.size()} SeqTracks, but expected ${numberOfMergedLanes}"

        assert seqTracks.every { SeqTrack seqTrack ->
            seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
        }: "Not all Fastqc workflows of all seqtracks are finished"

        List<Integer> numberOfReads = seqTracks.collect { SeqTrack seqTrack ->
            seqTrack.NReads
        }

        assert !numberOfReads.contains(null): 'At least one seqTrack has no value for number of reads'

        return numberOfReads.sum()
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
     */
    @Override
    @Deprecated
    AlignmentConfig getAlignmentConfig() {
        return config
    }

    @Override
    Integer getMaximalReadLength() {
        return getSeqTracks()*.dataFilesWhereIndexFileIsFalse.flatten().max { it.meanSequenceLength }.meanSequenceLength
    }
}
