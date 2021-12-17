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
package de.dkfz.tbi.otp.job.jobs.sophia

import groovy.util.logging.Slf4j
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.AbstractExecutePanCanJob
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

import java.nio.file.Path

@Component
@Scope("prototype")
@Slf4j
class ExecuteRoddySophiaJob extends AbstractExecutePanCanJob<SophiaInstance> implements AutoRestartableJob {

    @Autowired
    SophiaService sophiaService

    @SuppressWarnings('JavaIoPackageAccess')
    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(SophiaInstance sophiaInstance) {
        assert sophiaInstance

        sophiaService.validateInputBamFiles(sophiaInstance)
        Path workDirectory = sophiaService.getWorkDirectory(sophiaInstance)

        AbstractMergedBamFile bamFileDisease = sophiaInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = sophiaInstance.sampleType2BamFile

        Path bamFileDiseasePath = linkBamFileInWorkDirectory(bamFileDisease, workDirectory)
        Path bamFileControlPath = linkBamFileInWorkDirectory(bamFileControl, workDirectory)

        File diseaseInsertSizeFile = bamFileDisease.finalInsertSizeFile
        File controlInsertSizeFile = bamFileControl.finalInsertSizeFile
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(diseaseInsertSizeFile)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(controlInsertSizeFile)

        Integer tumorDefaultReadLength = bamFileDisease.getMaximalReadLength()
        Integer controlDefaultReadLength = bamFileControl.getMaximalReadLength()

        assert tumorDefaultReadLength && controlDefaultReadLength: "neither tumorDefaultReadLength nor controlDefaultReadLength may be null"

        List<String> cValues = []

        if (!([bamFileDisease, bamFileControl].every {
            RoddyBamFile.isAssignableFrom(Hibernate.getClass(it)) || ExternallyProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(it))
        })) {
            throw new RuntimeException("Unsupported BAM-File type for '${bamFileDisease.class}' or '${bamFileControl.class}' ")
        }

        SophiaWorkflowQualityAssessment bamFileDiseaseQualityAssessment = bamFileDisease.getOverallQualityAssessment() as SophiaWorkflowQualityAssessment
        SophiaWorkflowQualityAssessment bamFileControlQualityAssessment = bamFileControl.getOverallQualityAssessment() as SophiaWorkflowQualityAssessment

        cValues.add("controlMedianIsize:${bamFileControlQualityAssessment.insertSizeMedian}")
        cValues.add("tumorMedianIsize:${bamFileDiseaseQualityAssessment.insertSizeMedian}")
        cValues.add("controlStdIsizePercentage:${bamFileControlQualityAssessment.insertSizeCV}")
        cValues.add("tumorStdIsizePercentage:${bamFileDiseaseQualityAssessment.insertSizeCV}")
        cValues.add("controlProperPairPercentage:${bamFileControlQualityAssessment.getPercentProperlyPaired()}")
        cValues.add("tumorProperPairPercentage:${bamFileDiseaseQualityAssessment.getPercentProperlyPaired()}")

        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("insertsizesfile_list:${controlInsertSizeFile};${diseaseInsertSizeFile}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("controlDefaultReadLength:${controlDefaultReadLength}")
        cValues.add("tumorDefaultReadLength:${tumorDefaultReadLength}")

        cValues.addAll(getSampleExtractionVersion2RoddyParameters())

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(SophiaInstance sophiaInstance) {
        return ""
    }

    @Override
    protected void validate(SophiaInstance sophiaInstance) throws Throwable {
        assert sophiaInstance : "The input sophiaInstance must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(sophiaInstance, sophiaInstance.project.realm)

        List<File> directories = [
                sophiaInstance.workExecutionStoreDirectory,
        ]
        directories.addAll(sophiaInstance.workExecutionDirectories)

        List<Path> files = [
                sophiaService.getFinalAceseqInputFile(sophiaInstance),
        ]

        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        files.each {
            FileService.ensureFileIsReadableAndNotEmpty(it)
        }

        sophiaService.validateInputBamFiles(sophiaInstance)

        sophiaInstance.processingState = AnalysisProcessingStates.FINISHED
        sophiaInstance.save(flush: true)
    }
}
