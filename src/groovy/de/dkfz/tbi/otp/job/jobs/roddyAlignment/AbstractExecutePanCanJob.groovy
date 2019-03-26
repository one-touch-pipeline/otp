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

package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ChromosomeIdentifierSortingService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService

import java.nio.file.FileSystem
import java.nio.file.Path

abstract class AbstractExecutePanCanJob<R extends RoddyResult> extends AbstractRoddyJob<R> {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService

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


    protected Path linkBamFileInWorkDirectory(AbstractMergedBamFile abstractMergedBamFile, File workDirectory) {
        Realm realm = abstractMergedBamFile.realm
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
        String bamFileName = "${abstractMergedBamFile.sampleType.dirName}_${abstractMergedBamFile.individual.pid}_merged.mdup.bam"
        String baiFileName = "${bamFileName}.bai"

        Path targetFileBam = fileService.toPath(abstractMergedBamFile.pathForFurtherProcessing, fileSystem)
        Path targetFileBai = targetFileBam.resolveSibling(abstractMergedBamFile.baiFileName)
        Path workDirectoryPath = fileService.toPath(workDirectory, fileSystem)

        Path linkBamFile = workDirectoryPath.resolve(bamFileName)
        Path linkBaiFile = workDirectoryPath.resolve(baiFileName)
        fileService.createLink(linkBamFile, targetFileBam, realm)
        fileService.createLink(linkBaiFile, targetFileBai, realm)
        return linkBamFile
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(R roddyResult, Realm realm) throws Throwable {
        assert roddyResult: "roddyResult must not be null"
        assert realm: "realm must not be null"

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyResult)
        String nameInConfigFile = roddyResult.config.getNameUsedInConfig()

        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyResult.config.configFilePath))

        return [
                executeRoddyCommandService.defaultRoddyExecutionCommand(roddyResult, nameInConfigFile, analysisIDinConfigFile, realm),
                prepareAndReturnAdditionalImports(roddyResult),
                prepareAndReturnWorkflowSpecificParameter(roddyResult),
                prepareAndReturnCValues(roddyResult),
        ].join(" ")
    }

    String prepareAndReturnAdditionalImports(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"

        String fasttrack = (roddyResult.processingPriority >= ProcessingPriority.FAST_TRACK.priority) ?
                "-fasttrack"
                : ""
        String pluginVersion = roddyResult.config.pluginVersion
        String seqType = roddyResult.seqType.roddyName.toLowerCase()
        return "--additionalImports=${pluginVersion}-${seqType}${fasttrack}"
    }


    String prepareAndReturnCValues(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"
        List<String> cValues = prepareAndReturnWorkflowSpecificCValues(roddyResult)
        return "--cvalues=\"${cValues.join(',').replace('$', '\\$')}\""
    }


    String getChromosomeIndexParameterWithMitochondrium(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return "CHROMOSOME_INDICES:( ${sortedList.join(' ')} )"
    }


    String getChromosomeIndexParameterWithoutMitochondrium(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return "CHROMOSOME_INDICES:( ${sortedList.join(' ')} )"
    }




    protected abstract List<String> prepareAndReturnWorkflowSpecificCValues(R roddyResult)

    protected abstract String prepareAndReturnWorkflowSpecificParameter(R roddyResult)
}
