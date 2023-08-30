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
package de.dkfz.tbi.otp.dataprocessing.singleCell

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.SingleCellBamFileService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.HasIdentifier
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

@SuppressWarnings('JavaIoPackageAccess')
@ManagedEntity
class SingleCellBamFile extends AbstractBamFile implements HasIdentifier, ProcessParameterObject {

    /**
     * @deprecated use {@link SingleCellBamFileService#INPUT_DIRECTORY_NAME} instead
     */
    @Deprecated
    static final String INPUT_DIRECTORY_NAME = 'cell-ranger-input'

    /**
     * @deprecated use {@link SingleCellBamFileService#OUTPUT_DIRECTORY_NAME} instead
     */
    @Deprecated
    static final String OUTPUT_DIRECTORY_NAME = 'outs'

    /**
     * @deprecated use {@link SingleCellBamFileService#ORIGINAL_BAM_FILE_NAME} instead
     */
    @Deprecated
    static final String ORIGINAL_BAM_FILE_NAME = 'possorted_genome_bam.bam'

    /**
     * @deprecated use {@link SingleCellBamFileService#ORIGINAL_BAI_FILE_NAME} instead
     */
    @Deprecated
    static final String ORIGINAL_BAI_FILE_NAME = 'possorted_genome_bam.bam.bai'

    // is created manually
    /**
     * @deprecated use {@link SingleCellBamFileService#ORIGINAL_BAM_MD5SUM_FILE_NAME} instead
     */
    @Deprecated
    static final String ORIGINAL_BAM_MD5SUM_FILE_NAME = 'possorted_genome_bam.md5sum'

    /**
     * @deprecated use {@link SingleCellBamFileService#METRICS_SUMMARY_CSV_FILE_NAME} instead
     */
    @Deprecated
    static final String METRICS_SUMMARY_CSV_FILE_NAME = "metrics_summary.csv"

    /**
     * @deprecated use {@link SingleCellBamFileService#WEB_SUMMARY_HTML_FILE_NAME} instead
     */
    @Deprecated
    static final String WEB_SUMMARY_HTML_FILE_NAME = "web_summary.html"

    /**
     * @deprecated use {@link SingleCellBamFileService#CELL_RANGER_COMMAND_FILE_NAME} instead
     */
    @Deprecated
    static final String CELL_RANGER_COMMAND_FILE_NAME = "cell_ranger_command.txt"

    /**
     * @deprecated use {@link SingleCellBamFileService#CREATED_RESULT_FILES} instead
     */
    @Deprecated
    static final List<String> CREATED_RESULT_FILES = [
            WEB_SUMMARY_HTML_FILE_NAME,
            METRICS_SUMMARY_CSV_FILE_NAME,
            ORIGINAL_BAM_FILE_NAME,
            ORIGINAL_BAI_FILE_NAME,
            ORIGINAL_BAM_MD5SUM_FILE_NAME,
            'filtered_feature_bc_matrix.h5',
            'raw_feature_bc_matrix.h5',
            'molecule_info.h5',
            'cloupe.cloupe',
    ].asImmutable()

    /**
     * @deprecated use {@link SingleCellBamFileService#CREATED_RESULT_DIRS} instead
     */
    @Deprecated
    static final List<String> CREATED_RESULT_DIRS = [
            'filtered_feature_bc_matrix',
            'raw_feature_bc_matrix',
            'analysis',
    ].asImmutable()

    /**
     * @deprecated use {@link SingleCellBamFileService#CREATED_RESULT_FILES_AND_DIRS} instead
     */
    @Deprecated
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
        workDirectoryName validator: { String val, SingleCellBamFile obj ->
            uniquePerWorkPackageAndProperties(obj, ["workDirectoryName": val]) && OtpPathValidator.isValidRelativePath(val)
        }
        seqTracks minSize: 1
        identifier validator: { int val, SingleCellBamFile obj ->
            uniquePerWorkPackageAndProperties(obj, ["identifier": val])
        }
        md5sum validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED && obj.fileSize > 0))
        }
        fileOperationStatus validator: { val, obj ->
            return (val == FileOperationStatus.PROCESSED) == (obj.md5sum != null)
        }
    }

    /*
     * A GORM unique constraint on workPackage, like: `workDirectoryName unique: 'workPackage'` does not work
     * as hibernate seems to have problems applying the constraints when a property with the same name also
     * exists in a sister class, see RoddyBamFile.workDirectoryName.
     */
    private static boolean uniquePerWorkPackageAndProperties(SingleCellBamFile bam, Map properties) {
        List<SingleCellBamFile> result = findAllWhere([workPackage: bam.workPackage] + properties)
        return [] == result || [bam] == result
    }

    @Override
    boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    @Override
    String getBamFileName() {
        String antiBodyTarget = seqType.hasAntibodyTarget ? "-${((MergingWorkPackage) mergingWorkPackage).antibodyTarget.name}" : ''
        return "${sampleType.dirName}${antiBodyTarget}_${individual.pid}_merged.mdup.bam"
    }

    @Override
    String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    String getMd5SumFileName() {
        return "${bamFileName}.md5"
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
     */
    @Override
    @Deprecated
    AlignmentConfig getAlignmentConfig() {
        return mergingWorkPackage.config
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getWorkDirectory} instead
     */
    @Deprecated
    File getWorkDirectory() {
        return new File(baseDirectory, workDirectoryName)
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#buildWorkDirectoryName} instead
     */
    @Deprecated
    static String buildWorkDirectoryName(CellRangerMergingWorkPackage workPackage, int identifier) {
        return [
                "RG_${workPackage.referenceGenome.name ?: '-'}",
                "TV_${workPackage.referenceGenomeIndex.toolWithVersion.replace(" ", "-")}",
                "EC_${workPackage.expectedCells ?: '-'}",
                "FC_${workPackage.enforcedCells ?: '-'}",
                "PV_${workPackage.config.programVersion.replace("/", "-")}",
                "ID_${identifier}",
        ].join('_')
    }

    String getSingleCellSampleName() {
        return "${individual.pid}_${sampleType.dirName}"
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getSampleDirectory} instead
     */
    @Deprecated
    File getSampleDirectory() {
        return new File(new File(workDirectory, INPUT_DIRECTORY_NAME), singleCellSampleName)
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getOutputDirectory} instead
     */
    @Deprecated
    File getOutputDirectory() {
        return new File(workDirectory, singleCellSampleName)
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getResultDirectory} instead
     */
    @Deprecated
    File getResultDirectory() {
        return new File(outputDirectory, OUTPUT_DIRECTORY_NAME)
    }

    /**
     * Map of names to use for link and name used by CellRanger
     * @deprecated use {@link SingleCellBamFileService#getFileMappingForLinks} instead
     */
    @Deprecated
    Map<String, String> getFileMappingForLinks() {
        return CREATED_RESULT_FILES_AND_DIRS.collectEntries {
            [(getLinkNameForFile(it)): it]
        }
    }

    /**
     * list of linked files
     * @deprecated use {@link SingleCellBamFileService#getLinkedResultFiles} instead
     */
    @Deprecated
    List<File> getLinkedResultFiles() {
        File result = workDirectory
        return CREATED_RESULT_FILES_AND_DIRS.collect {
            new File(result, getLinkNameForFile(it))
        }
    }

    /**
     * return the name to use for the links of the result file, because the bam file should be named differently
     * @deprecated use {@link SingleCellBamFileService#getLinkNameForFile} instead
     */
    @Deprecated
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

    /**
     * @deprecated use {@link SingleCellBamFileService#getFinalInsertSizeFile} instead
     */
    @Deprecated
    @Override
    File getFinalInsertSizeFile() {
        throw new MissingPropertyException("Final insert size file is not implemented for single cell BAM files")
    }

    @Override
    Integer getMaximalReadLength() {
        throw new MissingPropertyException("Maximal read length is not implemented for single cell BAM files")
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getPathForFurtherProcessingNoCheck} instead
     */
    @Deprecated
    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(workDirectory, bamFileName)
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getQualityAssessmentCsvFile} instead
     */
    @Deprecated
    File getQualityAssessmentCsvFile() {
        return new File(resultDirectory, METRICS_SUMMARY_CSV_FILE_NAME)
    }

    /**
     * @deprecated use {@link SingleCellBamFileService#getWebSummaryResultFile} instead
     */
    @Deprecated
    File getWebSummaryResultFile() {
        return new File(resultDirectory, WEB_SUMMARY_HTML_FILE_NAME)
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
        QualityAssessmentMergedPass assessmentMergedPass = CollectionUtils.atMostOneElement(QualityAssessmentMergedPass.findAllWhere(
                abstractBamFile: this,
        ))
        if (!assessmentMergedPass) {
            assessmentMergedPass = new QualityAssessmentMergedPass(
                    abstractBamFile: this,
            )
            assessmentMergedPass.save(flush: true)
        }
        return assessmentMergedPass
    }

    @Override
    CellRangerQualityAssessment getQualityAssessment() {
        return CellRangerQualityAssessment.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'abstractBamFile', this
            }
        } as CellRangerQualityAssessment
    }

    @Override
    String toString() {
        String latest = mergingWorkPackage ? (mostRecentBamFile ? ' (latest)' : '') : '?'
        String withdrawn = withdrawn ? ' (withdrawn)' : ''
        return "SCBF ${id}: ${identifier}${latest}${withdrawn} ${qcTrafficLightStatus} ${mergingWorkPackage.toStringWithoutIdAndPipeline()}"
    }
}
