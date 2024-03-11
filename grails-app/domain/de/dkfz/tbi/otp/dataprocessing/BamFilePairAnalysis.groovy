/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QcTrafficLightStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.Artefact

@ManagedEntity
abstract class BamFilePairAnalysis implements Artefact, CommentableWithProject, ProcessParameterObject, Entity {
    /**
     * Refers to the config file which is stored in the database and is used as a basis for all the files in the filesystem.
     */
    ConfigPerProjectAndSeqType config

    AbstractBamFile sampleType1BamFile

    AbstractBamFile sampleType2BamFile

    /**
     * Used to construct paths in {@link AbstractBamFileAnalysisService#getWorkDirectory()}.
     * For example 2014-08-25_15h32.
     */
    String instanceName

    boolean withdrawn = false

    SamplePair samplePair

    static belongsTo = [
            samplePair: SamplePair,
    ]

    /**
     * The overall processing state of this analysis run.
     * Because the analysis StartJob creates an instance of a BamFilePairAnalysis immediately when starting it, this will always start
     * as {@link AnalysisProcessingStates#IN_PROGRESS}.
     */
    AnalysisProcessingStates processingState = AnalysisProcessingStates.IN_PROGRESS

    QcTrafficLightStatus qcTrafficLightStatus

    static constraints = {
        sampleType1BamFile validator: { AbstractBamFile val, BamFilePairAnalysis obj ->
            obj.samplePair &&
                    val.fileOperationStatus == FileOperationStatus.PROCESSED &&
                    val.mergingWorkPackage.id == obj.samplePair.mergingWorkPackage1.id
        }
        sampleType2BamFile validator: { AbstractBamFile val, BamFilePairAnalysis obj ->
            obj.samplePair &&
                    val.fileOperationStatus == FileOperationStatus.PROCESSED &&
                    val.mergingWorkPackage.id == obj.samplePair.mergingWorkPackage2.id
        }
        instanceName blank: false, unique: 'samplePair', shared: "pathComponent"
        config nullable: true, validator: { val ->
            !val || ([SnvConfig, RoddyWorkflowConfig, RunYapsaConfig].any { it.isAssignableFrom(Hibernate.getClass(val)) }) &&
                    val?.pipeline?.type != Pipeline.Type.ALIGNMENT
        }
        qcTrafficLightStatus nullable: true, validator: { status, obj ->
            if (status == QcTrafficLightStatus.ACCEPTED && !obj.comment) {
                return "comment.missing"
            }
        }
        comment nullable: true
        workflowArtefact nullable: true
    }

    static Closure mapping = {
        sampleType1BamFile index: "bam_file_pair_analysis_sample_type_1_bam_file_idx"
        sampleType2BamFile index: "bam_file_pair_analysis_sample_type_2_bam_file_idx"
        qcTrafficLightStatus index: "bam_file_pair_analysis_qc_traffic_light_status_idx"
        samplePair index: "bam_file_pair_analysis_sample_pair_idx"
        config lazy: false, index: "bam_file_pair_analysis_config_idx"
        comment cascade: "all-delete-orphan"
        workflowArtefact index: "bam_file_pair_analysis_workflow_artefact_idx"
    }

    @Override
    Project getProject() {
        return samplePair.project
    }

    @Override
    Individual getIndividual() {
        return samplePair.individual
    }

    @Override
    SeqType getSeqType() {
        return samplePair.seqType
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return sampleType1BamFile.containedSeqTracks + sampleType2BamFile.containedSeqTracks
    }

    /**
     * @deprecated use {@link AbstractAnalysisLinkFileService#getDirectoryPath()} or {@link AbstractAnalysisWorkFileService#getDirectoryPath()}
     * (use {@link AnalysisLinkFileServiceFactoryService#getService()} or {@link AnalysisWorkFileServiceFactoryService#getService()}
     * to get the correct service if necessary)
     */
    @Deprecated
    File getWorkDirectory() {
        return instancePath.absoluteDataManagementPath
    }

    /**
     * @deprecated use {@link AbstractAnalysisLinkFileService#getDirectoryPath()} or {@link AbstractAnalysisWorkFileService#getDirectoryPath()}
     * (use {@link AnalysisLinkFileServiceFactoryService#getService()} or {@link AnalysisWorkFileServiceFactoryService#getService()}
     * to get the correct service if necessary)
     */
    @Deprecated
    abstract OtpPath getInstancePath()

    abstract ReferenceGenome getReferenceGenome()
}
