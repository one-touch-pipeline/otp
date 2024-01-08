/*
 * Copyright 2011-2023 The OTP authors
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

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.TupleConstructor
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Represents a single generation of one merged BAM file (whereas a {@link AbstractMergingWorkPackage} represents all
 * generations).
 */
@ManagedEntity
abstract class AbstractBamFile implements CommentableWithProject, Entity {

    enum QaProcessingStatus {
        UNKNOWN,
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED
    }

    @TupleConstructor
    static enum QcTrafficLightStatus {
        // status is set by OTP when the file is still in processing
        NOT_RUN_YET(JobNotifyCase.SHOULD_NOT_OCCUR),
        // status is set by OTP when QC thresholds were met
        QC_PASSED(JobNotifyCase.NO_NOTIFY),
        // status is set by bioinformaticians when they decide to keep the file although QC thresholds were not met
        ACCEPTED(JobNotifyCase.SHOULD_NOT_OCCUR),
        // status is set by OTP when QC thresholds were not met but the project is configured to allow failed files
        AUTO_ACCEPTED(JobNotifyCase.NOTIFY),
        // status is set by OTP when project is configured to not check QC thresholds
        UNCHECKED(JobNotifyCase.NO_NOTIFY),
        // status is set by OTP when QC error thresholds were not met, replacing BLOCKED
        WARNING(JobNotifyCase.NOTIFY),

        final JobNotifyCase jobNotifyCase

        /**
         * Notify category of {@link QcTrafficLightStatus}. It defines, if notify emails should be send or should not be sent or that the status
         * should not occur automatically in job system, but only set manually in the gui.
         */
        @TupleConstructor
        static enum JobNotifyCase {
            /**
             * For that cases, emails should be send
             */
            NOTIFY,
            /**
             * For that cases, no emails should be send
             */
            NO_NOTIFY,
            /**
             * Cases set manually and should therefore not occur during workflow
             */
            SHOULD_NOT_OCCUR,
        }
    }

    /**
     * This enum is used to specify the different transfer states of the {@link AbstractBamFile} until it is copied to the project folder
     */
    enum FileOperationStatus {
        /**
         * default value -> state of the {@link AbstractBamFile} when it is created (declared)
         * no processing has been started on the bam file and it is also not ready to be transferred yet
         */
        DECLARED,
        /**
         * An {@link AbstractBamFile} with this status needs to be transferred.
         */
        NEEDS_PROCESSING,
        /**
         * An {@link AbstractBamFile} is in process of being transferred.
         */
        INPROGRESS,
        /**
         * The transfer of the {@link AbstractBamFile} is finished.
         */
        PROCESSED
    }

    boolean withdrawn = false

    /**
     * Coverage without N of the BamFile
     */
    // Has to be from Type Double so that it can be nullable
    Double coverage

    /**
     * Coverage with N of the BAM file.
     * In case of sequencing types that need a BED file this value stays 'null' since there is no differentiation between 'with N' and 'without N'.
     */
    Double coverageWithN

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN

    /**
     * Checksum to verify success of copying.
     * When the file - and all other files handled by the transfer workflow - are copied, its checksum is stored in this property.
     * Otherwise it is null.
     */
    String md5sum

    /** Additional digest, may be used in the future (to verify xz compression) */
    String sha256sum

    /**
     * date of last modification of the file on the file system
     */
    Date dateFromFileSystem

    /**
     * file size
     */
    long fileSize = -1

    /**
     * Holds the number of lanes which were merged in this bam file
     */
    Integer numberOfMergedLanes

    /**
     * bam file satisfies criteria from this {@link AbstractMergingWorkPackage}
     */
    AbstractMergingWorkPackage workPackage

    /**
     * This property contains the transfer state of an AbstractBamFile to the project folder.
     * Be aware that for RoddyBamFile it is only used for documentation of the state.
     */
    FileOperationStatus fileOperationStatus = FileOperationStatus.DECLARED

    QcTrafficLightStatus qcTrafficLightStatus = QcTrafficLightStatus.NOT_RUN_YET

    abstract AbstractMergingWorkPackage getMergingWorkPackage()
    abstract Set<SeqTrack> getContainedSeqTracks()
    abstract AbstractQualityAssessment getQualityAssessment()

    static constraints = {
        coverage(nullable: true)
        coverageWithN(nullable: true)
        comment nullable: true
        dateFromFileSystem(nullable: true)
        md5sum nullable: true, matches: /^[0-9a-f]{32}$/
        sha256sum nullable: true
        numberOfMergedLanes nullable: true, validator: { val, obj ->
            if (Hibernate.getClass(obj) == ExternallyProcessedBamFile) {
                val == null
            } else {
                val >= 1
            }
        }
        qcTrafficLightStatus validator: { status, obj ->
            if (status == QcTrafficLightStatus.ACCEPTED && !obj.comment) {
                return "comment.missing"
            }
        }
    }

    static mapping = {
        'class' index: "abstract_bam_file_class_idx"
        withdrawn index: "abstract_bam_file_withdrawn_idx"
        qualityAssessmentStatus index: "abstract_bam_file_quality_assessment_status_idx"
        comment cascade: "all-delete-orphan"
        numberOfMergedLanes index: "abstract_merged_bam_file_number_of_merged_lanes_idx"
        qcTrafficLightStatus index: "abstract_bam_file_qc_traffic_light_status"
        workPackage lazy: false, index: "abstract_merged_bam_file_work_package_idx"
    }

    BedFile getBedFile() {
        assert seqType.needsBedFile : "A BED file is only available when needed."

        List<BedFile> bedFiles = BedFile.findAllWhere(
                referenceGenome: referenceGenome,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit
        )
        return exactlyOneElement(bedFiles, "Wrong BedFile count, found: ${bedFiles}")
    }

    @Override
    Project getProject() {
        return individual?.project
    }

    Individual getIndividual() {
        return sample?.individual
    }

    Sample getSample() {
        return mergingWorkPackage?.sample
    }

    SampleType getSampleType() {
        return  sample?.sampleType
    }

    SeqType getSeqType() {
        return mergingWorkPackage?.seqType
    }

    Pipeline getPipeline() {
        return mergingWorkPackage?.pipeline
    }

    abstract boolean isMostRecentBamFile()

    abstract String getBamFileName()

    abstract String getBaiFileName()

    /**
     * @deprecated method is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
     */
    @Deprecated
    abstract AlignmentConfig getAlignmentConfig()

    /**
     * @deprecated use {@link AbstractAbstractBamFileService#getFinalInsertSizeFile()} and {@link AbstractBamFileServiceFactoryService#getService()}
     */
    @Deprecated
    abstract File getFinalInsertSizeFile()

    abstract Integer getMaximalReadLength()

    /**
     * @return The reference genome which was used to produce this BAM file.
     */
    ReferenceGenome getReferenceGenome() {
        return workPackage.referenceGenome
    }

    void withdraw() {
        withTransaction {
            BamFilePairAnalysis.findAllBySampleType1BamFileOrSampleType2BamFile(this, this).each {
                AbstractBamFileAnalysisService.withdraw(it)
            }

            withdrawn = true
            assert AbstractBamFileService.saveBamFile(this)
        }
    }

    /**
     * @deprecated use {@link AbstractBamFileService#getBaseDirectory()}
     */
    @Deprecated
    File getBaseDirectory() {
        String antiBodyTarget = seqType.hasAntibodyTarget ? "-${((MergingWorkPackage) mergingWorkPackage).antibodyTarget.name}" : ''
        OtpPath viewByPid = individual.getViewByPidPath(seqType)
        OtpPath path = new OtpPath(
                viewByPid,
                "${sample.sampleType.dirName}${antiBodyTarget}".toString(),
                seqType.libraryLayoutDirName,
                'merged-alignment'
        )
        return path.absoluteDataManagementPath
    }

    /**
     * @deprecated use {@link AbstractAbstractBamFileService#getPathForFurtherProcessing} and
     * {@link AbstractBamFileServiceFactoryService#getService()}
     */
    @Deprecated
    File getPathForFurtherProcessing() {
        mergingWorkPackage.refresh() // Sometimes the mergingWorkPackage.processableBamFileInProjectFolder is empty but should have a value
        AbstractBamFile processableBamFileInProjectFolder = mergingWorkPackage.processableBamFileInProjectFolder
        if (this.id == processableBamFileInProjectFolder?.id) {
            return pathForFurtherProcessingNoCheck
        }
        throw new IllegalStateException("This BAM file is not in the project folder or not processable.\n" +
                "this: ${this}\nprocessableBamFileInProjectFolder: ${processableBamFileInProjectFolder}")
    }

    /**
     * @deprecated use {@link AbstractAbstractBamFileService#getPathForFurtherProcessingNoCheck} and
     * {@link AbstractBamFileServiceFactoryService#getService()}
     */
    @Deprecated
    protected abstract File getPathForFurtherProcessingNoCheck()
}
