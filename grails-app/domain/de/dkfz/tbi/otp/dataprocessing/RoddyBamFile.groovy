/*
 * Copyright 2011-2025 The OTP authors
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
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.infrastructure.alignment.RoddyBamFileNames
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.HasIdentifier
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

/**
 * This bam file is produced by some Roddy alignment workflow.
 */
@ManagedEntity
class RoddyBamFile extends AbstractBamFile implements HasIdentifier, ProcessParameterObject, RoddyResult {

    static hasMany = [
            seqTracks                   : SeqTrack,
            roddyExecutionDirectoryNames: String,
    ]

    /**
     * config file used to create this bam file
     */
    RoddyWorkflowConfig config

    String workDirectoryName

    static Closure constraints = {
        seqTracks minSize: 1, validator: { val, obj, errors ->
            obj.isConsistentAndContainsNoWithdrawnData().each {
                errors.reject(null, it)
            }
        }
        workPackage validator: { val, obj ->
            [Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Name.RODDY_RNA_ALIGNMENT].contains(val?.pipeline?.name) &&
                    MergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }

        config nullable: true, validator: { val, obj -> !val || (val.pipeline?.id == obj.workPackage?.pipeline?.id) }
        identifier validator: { val, obj ->
            !RoddyBamFile.findAllByWorkPackageAndIdentifierAndIdNotEqual(obj.workPackage, val, obj.id)
        }
        roddyExecutionDirectoryNames nullable: true
        workDirectoryName nullable: true, validator: { val, obj ->
            (val == null || (OtpPathValidator.isValidPathComponent(val) &&
                    !RoddyBamFile.findAllByWorkPackageAndWorkDirectoryNameAndIdNotEqual(obj.workPackage, val, obj.id)))
        } // needs to be nullable for objects created before link structure was used
        md5sum validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED && obj.fileSize > 0))
        }
        fileOperationStatus validator: { val, obj ->
            return (val == FileOperationStatus.PROCESSED) == (obj.md5sum != null)
        }
    }

    static Closure mapping = {
        config index: "roddy_bam_file_config_idx"
    }

    List<String> isConsistentAndContainsNoWithdrawnData() {
        List<String> errors = []

        Closure assertAndTrackOnError = { def expression, String errorMessage ->
            if (!expression) {
                errors << errorMessage
            }
        }

        Set<SeqTrack> allContainedSeqTracks = this.containedSeqTracks

        assertAndTrackOnError withdrawn || !allContainedSeqTracks.any { it.withdrawn },
                "not withdrawn bam file has withdrawn seq tracks"

        assertAndTrackOnError numberOfMergedLanes == allContainedSeqTracks.size(),
                "total number of merged lanes is not equal to number of contained seq tracks: ${numberOfMergedLanes} vs ${allContainedSeqTracks.size()}"

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
        return seqTracks
    }

    boolean hasMultipleLibraries() {
        return this.seqTracks*.libraryDirectoryName.unique().size() > 1
    }

    @Override
    AbstractQualityAssessment getQualityAssessment() {
        return CollectionUtils.atMostOneElement(RoddyMergedBamQa.withCriteria {
            eq 'chromosome', RoddyQualityAssessment.ALL
            abstractBamFile {
                eq 'id', this.id
            }
        })
    }

    @Override
    boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    @Override
    String toString() {
        String latest = mergingWorkPackage ? (mostRecentBamFile ? ' (latest)' : '') : '?'
        String withdrawn = withdrawn ? ' (withdrawn)' : ''
        return "RBF ${id}: ${identifier}${latest}${withdrawn} ${qcTrafficLightStatus} ${mergingWorkPackage?.toStringWithoutIdAndPipeline()}"
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

    // Example: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/whole_genome_sequencing/view-by-pid/somePid/control/paired/merged-alignment/.merging_3
    /**
     * @deprecated use {@link PanCancerWorkFileService#getDirectoryPath} instead
     */
    @Override
    @Deprecated
    File getWorkDirectory() {
        if (workflowArtefact?.producedBy?.workFolder) {
            return new File(workflowArtefact?.producedBy?.workDirectory)
        }
        return new File(baseDirectory, workDirectoryName)
    }

    /**
     * @deprecated use {@link PanCancerLinkFileService#getQADirectory} instead
     */
    @Deprecated
    File getFinalQADirectory() {
        return new File(baseDirectory, RoddyBamFileNames.QUALITY_CONTROL_DIR)
    }

    /**
     * @deprecated use {@link PanCancerWorkFileService#getQADirectory} instead
     */
    @Deprecated
    File getWorkQADirectory() {
        return new File(workDirectory, RoddyBamFileNames.QUALITY_CONTROL_DIR)
    }

    /**
     * @deprecated use {@link PanCancerLinkFileService#getMergedQADirectory} instead
     */
    @Deprecated
    File getFinalMergedQADirectory() {
        return new File(this.finalQADirectory, RoddyBamFileNames.MERGED_DIR)
    }

    /**
     * @deprecated use {@link PanCancerWorkFileService#getMergedQADirectory} instead
     */
    @Deprecated
    File getWorkMergedQADirectory() {
        return new File(this.workQADirectory, RoddyBamFileNames.MERGED_DIR)
    }

    /**
     * @deprecated use {@link PanCancerWorkFileService#getMergedQAJsonFile} instead
     */
    @Deprecated
    File getWorkMergedQAJsonFile() {
        return new File(workMergedQADirectory, RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME)
    }

    /**
     * @deprecated use {@link PanCancerLinkFileService#getBamFile} instead
     */
    @Deprecated
    File getFinalBamFile() {
        return new File(baseDirectory, this.bamFileName)
    }

    /**
     * @deprecated use {@link PanCancerWorkFileService#getBamFile} instead
     */
    @Deprecated
    File getWorkBamFile() {
        return new File(workDirectory, this.bamFileName)
    }

    /**
     * @deprecated use {@link PanCancerWorkFileService#getBaiFile} instead
     */
    @Deprecated
    File getWorkBaiFile() {
        return new File(workDirectory, this.baiFileName)
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
    @Deprecated
    boolean isOldStructureUsed() {
        return !workDirectoryName
    }

    /**
     * return for old structure the final bam file and for the new structure the work bam file
     * @deprecated use {@link PanCancerLinkFileService#getPathForFurtherProcessingNoCheck} instead
     */
    @Deprecated
    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return isOldStructureUsed() ? finalBamFile : workBamFile
    }

    Long getNumberOfReadsFromQa() {
        AbstractQualityAssessment qa = qualityAssessment
        return qa.pairedRead1 + qa.pairedRead2
    }

    Long getNumberOfReadsFromFastQc() {
        Set<SeqTrack> seqTracks = containedSeqTracks
        assert seqTracks.size() == numberOfMergedLanes: "Found ${seqTracks.size()} SeqTracks, but expected ${numberOfMergedLanes}"

        assert seqTracks.every { SeqTrack seqTrack ->
            seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
        }: "Not all Fastqc workflows of all seqtracks are finished"

        List<Integer> numberOfReads = seqTracks*.NReads

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
        return seqTracks*.sequenceFilesWhereIndexFileIsFalse.flatten().max { it.meanSequenceLength }.meanSequenceLength
    }
}
