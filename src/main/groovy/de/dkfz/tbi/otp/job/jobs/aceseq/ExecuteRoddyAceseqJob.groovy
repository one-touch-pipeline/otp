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
package de.dkfz.tbi.otp.job.jobs.aceseq

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.AbstractExecutePanCanJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkFileUtils

@Component
@Scope("prototype")
@Slf4j
class ExecuteRoddyAceseqJob extends AbstractExecutePanCanJob<AceseqInstance> implements AutoRestartableJob {

    @Autowired
    AceseqService aceseqService

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(AceseqInstance aceseqInstance) {
        assert aceseqInstance

        aceseqService.validateInputBamFiles(aceseqInstance)

        AbstractMergedBamFile bamFileDisease = aceseqInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = aceseqInstance.sampleType2BamFile
        File bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing
        File bamFileControlPath = bamFileControl.pathForFurtherProcessing

        final Realm realm = aceseqInstance.project.realm
        ReferenceGenome referenceGenome = bamFileDisease.referenceGenome
        referenceGenomeService.checkReferenceGenomeFilesAvailability(bamFileDisease.mergingWorkPackage)

        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        File chromosomeLengthFile = referenceGenomeService.chromosomeLengthFile(bamFileDisease.mergingWorkPackage)
        assert chromosomeLengthFile: "Path to the chromosome length file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeLengthFile)

        File gcContentFile = referenceGenomeService.gcContentFile(bamFileDisease.mergingWorkPackage)
        assert gcContentFile: "Path to the gc content file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(gcContentFile)

        SophiaInstance sophiaInstance = SophiaInstance.getLatestValidSophiaInstanceForSamplePair(aceseqInstance.samplePair)
        File aceseqInputFile = sophiaInstance.finalAceseqInputFile
        assert aceseqInputFile : "Path to the ACEseq input file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(aceseqInputFile)

        linkFileUtils.createAndValidateLinks([(aceseqInputFile): new File(
                aceseqInstance.workDirectory, aceseqInputFile.name)], realm, aceseqInstance.project.unixGroup
        )

        List<String> cValues = []
        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("REFERENCE_GENOME:${referenceGenomeFastaFile}")
        cValues.add("CHROMOSOME_LENGTH_FILE:${chromosomeLengthFile}")
        cValues.add("CHR_SUFFIX:${referenceGenome.chromosomeSuffix}")
        cValues.add("CHR_PREFIX:${referenceGenome.chromosomePrefix}")
        cValues.add("aceseqOutputDirectory:${aceseqInstance.workDirectory}")
        cValues.add("svOutputDirectory:${aceseqInstance.workDirectory}")
        cValues.add("MAPPABILITY_FILE:${referenceGenome.mappabilityFile}")
        cValues.add("REPLICATION_TIME_FILE:${referenceGenome.replicationTimeFile}")
        cValues.add("GC_CONTENT_FILE:${gcContentFile}")
        cValues.add("GENETIC_MAP_FILE:${referenceGenome.geneticMapFile}")
        cValues.add("KNOWN_HAPLOTYPES_FILE:${referenceGenome.knownHaplotypesFile}")
        cValues.add("KNOWN_HAPLOTYPES_LEGEND_FILE:${referenceGenome.knownHaplotypesLegendFile}")
        cValues.add("GENETIC_MAP_FILE_X:${referenceGenome.geneticMapFileX}")
        cValues.add("KNOWN_HAPLOTYPES_FILE_X:${referenceGenome.knownHaplotypesFileX}")
        cValues.add("KNOWN_HAPLOTYPES_LEGEND_FILE_X:${referenceGenome.knownHaplotypesLegendFileX}")

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(AceseqInstance aceseqInstance) {
        return ""
    }


    @Override
    protected void validate(AceseqInstance aceseqInstance) throws Throwable {
        assert aceseqInstance : "The input aceseqInstance must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(aceseqInstance, aceseqInstance.project.realm)

        List<File> directories = [
                aceseqInstance.workExecutionStoreDirectory,
        ]
        directories.addAll(aceseqInstance.workExecutionDirectories)


        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        aceseqService.validateInputBamFiles(aceseqInstance)

        aceseqInstance.processingState = AnalysisProcessingStates.FINISHED
        aceseqInstance.save(flush: true)
    }
}
