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
package de.dkfz.tbi.otp.job.jobs.snvcalling

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.AbstractExecutePanCanJob
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

import java.nio.file.Path

@CompileDynamic
@Component
@Scope("prototype")
@Slf4j
class ExecuteRoddySnvJob extends AbstractExecutePanCanJob<RoddySnvCallingInstance> implements AutoRestartableJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    SnvCallingService snvCallingService

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    IndividualService individualService

    @Override
    @SuppressWarnings('UnnecessaryObjectReferences') // old wf
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddySnvCallingInstance roddySnvCallingInstance) {
        assert roddySnvCallingInstance

        snvCallingService.validateInputBamFiles(roddySnvCallingInstance)

        AbstractBamFile bamFileDisease = roddySnvCallingInstance.sampleType1BamFile
        AbstractBamFile bamFileControl = roddySnvCallingInstance.sampleType2BamFile
        File bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing
        File bamFileControlPath = bamFileControl.pathForFurtherProcessing

        ReferenceGenome referenceGenome = roddySnvCallingInstance.referenceGenome
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        Path individualPath = individualService.getViewByPidPath(roddySnvCallingInstance.individual, roddySnvCallingInstance.seqType)
        Path resultDirectory = snvCallingService.getWorkDirectory(roddySnvCallingInstance)

        List<String> cValues = []
        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("REFERENCE_GENOME:${referenceGenomeFastaFile.path}")
        cValues.add("CHROMOSOME_LENGTH_FILE:${referenceGenomeService.chromosomeLengthFile(bamFileControl.mergingWorkPackage).path}")
        cValues.add("CHR_SUFFIX:${referenceGenome.chromosomeSuffix}")
        cValues.add("CHR_PREFIX:${referenceGenome.chromosomePrefix}")
        cValues.add("${getChromosomeIndexParameterWithoutMitochondrium(roddySnvCallingInstance.referenceGenome)}")
        cValues.add("analysisMethodNameOnOutput:${individualPath.relativize(resultDirectory)}")

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(RoddySnvCallingInstance roddySnvCallingInstance) {
        return ""
    }

    @Override
    protected void validate(RoddySnvCallingInstance roddySnvCallingInstance) throws Throwable {
        assert roddySnvCallingInstance: "The input roddyResult must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(roddySnvCallingInstance)

        List<File> directories = [
                roddySnvCallingInstance.workExecutionStoreDirectory,
        ]
        directories.addAll(roddySnvCallingInstance.workExecutionDirectories)

        List<Path> files = [
                snvCallingService.getCombinedPlotPath(roddySnvCallingInstance),
                snvCallingService.getSnvCallingResult(roddySnvCallingInstance),
                snvCallingService.getSnvDeepAnnotationResult(roddySnvCallingInstance),
        ]

        directories.each {
            FileService.ensureDirIsReadableAndNotEmptyStatic(it.toPath())
        }

        files.each {
            FileService.ensureFileIsReadableAndNotEmptyStatic(it)
        }

        snvCallingService.getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty(roddySnvCallingInstance)
        snvCallingService.validateInputBamFiles(roddySnvCallingInstance)

        roddySnvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        roddySnvCallingInstance.save(flush: true)
    }
}
