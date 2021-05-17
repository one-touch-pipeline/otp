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
package de.dkfz.tbi.otp.job.jobs.indelCalling

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.AbstractExecutePanCanJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

import java.nio.file.Path
import java.nio.file.Paths

@Component
@Scope("prototype")
@Slf4j
class ExecuteRoddyIndelJob extends AbstractExecutePanCanJob<IndelCallingInstance> implements AutoRestartableJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    IndelCallingService indelCallingService


    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(IndelCallingInstance indelCallingInstance) {
        assert indelCallingInstance

        indelCallingService.validateInputBamFiles(indelCallingInstance)
        File workDirectory = indelCallingInstance.workDirectory

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        Path bamFileDiseasePath = linkBamFileInWorkDirectory(bamFileDisease, workDirectory)
        Path bamFileControlPath = linkBamFileInWorkDirectory(bamFileControl, workDirectory)

        ReferenceGenome referenceGenome = indelCallingInstance.referenceGenome
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        Path individualPath = Paths.get(indelCallingInstance.individual.getViewByPidPath(indelCallingInstance.seqType).absoluteDataManagementPath.path)
        Path resultDirectory = Paths.get(indelCallingInstance.instancePath.absoluteDataManagementPath.path)

        List<String> cValues = []
        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("REFERENCE_GENOME:${referenceGenomeFastaFile.path}")
        cValues.add("CHR_SUFFIX:${referenceGenome.chromosomeSuffix}")
        cValues.add("CHR_PREFIX:${referenceGenome.chromosomePrefix}")
        cValues.add("analysisMethodNameOnOutput:${individualPath.relativize(resultDirectory)}")
        cValues.add("VCF_NORMAL_HEADER_COL:${bamFileControl.sampleType.dirName}")
        cValues.add("VCF_TUMOR_HEADER_COL:${bamFileDisease.sampleType.dirName}")
        cValues.add("SEQUENCE_TYPE:${bamFileDisease.seqType.roddyName}")

        if (bamFileDisease.seqType.isExome()) {
            BedFile bedFile = bamFileDisease.bedFile
            File bedFilePath = bedFileService.filePath(bedFile) as File
            cValues.add("EXOME_CAPTURE_KIT_BEDFILE:${bedFilePath}")
        }

        cValues.addAll(getSampleExtractionVersion2RoddyParameters())

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(IndelCallingInstance indelCallingInstance) {
        return ""
    }


    @Override
    protected void validate(IndelCallingInstance indelCallingInstance) throws Throwable {
        assert indelCallingInstance: "The input indelCallingInstance must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(indelCallingInstance, indelCallingInstance.project.realm)

        List<File> directories = [
                indelCallingInstance.workExecutionStoreDirectory,
        ]
        directories.addAll(indelCallingInstance.workExecutionDirectories)

        List<File> files = [
                indelCallingInstance.getCombinedPlotPath(),
                indelCallingInstance.getIndelQcJsonFile(),
                indelCallingInstance.getSampleSwapJsonFile(),
        ]

        files.addAll(indelCallingInstance.getResultFilePathsToValidate())

        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        files.each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }

        indelCallingService.validateInputBamFiles(indelCallingInstance)
    }
}
