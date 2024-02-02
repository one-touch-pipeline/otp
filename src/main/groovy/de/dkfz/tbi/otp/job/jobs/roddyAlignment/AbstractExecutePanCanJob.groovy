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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.FileSystem
import java.nio.file.Path

@CompileDynamic
abstract class AbstractExecutePanCanJob<R extends RoddyResult> extends AbstractRoddyJob<R> {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    BedFileService bedFileService

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    FileService fileService

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService

    @Autowired
    ProcessingOptionService processingOptionService

    protected Path linkBamFileInWorkDirectory(AbstractBamFile abstractBamFile, Path workDirectory) {
        String bamFileName = "${abstractBamFile.sampleType.dirName}_${abstractBamFile.individual.pid}_merged.mdup.bam"
        String baiFileName = "${bamFileName}.bai"

        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path targetFileBam = fileService.toPath(abstractBamFile.pathForFurtherProcessing, fileSystem)
        Path targetFileBai = targetFileBam.resolveSibling(abstractBamFile.baiFileName)

        Path linkBamFile = workDirectory.resolve(bamFileName)
        Path linkBaiFile = workDirectory.resolve(baiFileName)
        fileService.createLink(linkBamFile, targetFileBam, CreateLinkOption.DELETE_EXISTING_FILE)
        fileService.createLink(linkBaiFile, targetFileBai, CreateLinkOption.DELETE_EXISTING_FILE)
        return linkBamFile
    }

    @SuppressWarnings('JavaIoPackageAccess')
    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(R roddyResult) throws Throwable {
        assert roddyResult: "roddyResult must not be null"

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyResult)
        String nameInConfigFile = roddyResult.config.nameUsedInConfig

        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyResult.config.configFilePath))

        return [
                executeRoddyCommandService.defaultRoddyExecutionCommand(roddyResult, nameInConfigFile, analysisIDinConfigFile),
                prepareAndReturnAdditionalImports(roddyResult),
                prepareAndReturnWorkflowSpecificParameter(roddyResult),
                prepareAndReturnCValues(roddyResult),
        ].join(" ")
    }

    String prepareAndReturnAdditionalImports(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"

        ProcessingPriority processingPriority = roddyResult.processingPriority
        String roddyConfigSuffix = processingPriority ?
                "-${processingPriority.roddyConfigSuffix}"
                : ""
        String programVersion = roddyResult.config.programVersion
        String seqType = roddyResult.seqType.roddyName.toLowerCase()
        return "--additionalImports=${programVersion}-${seqType}${roddyConfigSuffix}"
    }

    String prepareAndReturnCValues(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"
        List<String> cValues = prepareAndReturnWorkflowSpecificCValues(roddyResult)
        cValues.add("sharedFilesBaseDirectory:${processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY)}")
        return "--cvalues=\"${cValues.join(',').replace('$', '\\$')}\""
    }

    String getChromosomeIndexParameterWithoutMitochondrium(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return "CHROMOSOME_INDICES:( ${sortedList.join(' ')} )"
    }

    /**
     * Parameters so Roddy properly parses sample type names containing '_'
     */
    protected static List<String> getSampleExtractionVersion2RoddyParameters() {
        return [
                "selectSampleExtractionMethod:version_2",
                "matchExactSampleName:true",
                "allowSampleTerminationWithIndex:false",
                "useLowerCaseFilenameForSampleExtraction:false",
        ]
    }

    protected abstract List<String> prepareAndReturnWorkflowSpecificCValues(R roddyResult)

    protected abstract String prepareAndReturnWorkflowSpecificParameter(R roddyResult)
}
