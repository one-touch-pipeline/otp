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
package de.dkfz.tbi.otp.job.processing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@CompileDynamic
@Transactional
class RoddyConfigValueService {

    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService
    LsdfFilesService lsdfFilesService
    ProcessingOptionService processingOptionService
    ReferenceGenomeService referenceGenomeService

    private static final ObjectMapper MAPPER = new ObjectMapper()

    Map<String, String> getDefaultValues() {
        return [
                "BASE_REFERENCE_GENOME"   : processingOptionService.findOptionAsString(ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME),
                "sharedFilesBaseDirectory": processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY),
        ]
    }

    /**
     * Generates configuration values that are common to all alignment workflows
     */
    Map<String, String> getAlignmentValues(RoddyBamFile roddyBamFile, String combinedConfig) {
        assert roddyBamFile

        Map<String, String> cValues = [:]

        String referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath
        cValues.put("INDEX_PREFIX", referenceGenomeFastaFile) // used for PanCancer pipeline
        cValues.put("GENOME_FA", referenceGenomeFastaFile) // used for RNA pipeline

        cValues.put("possibleControlSampleNamePrefixes", "${roddyBamFile.sampleType.dirName}")
        cValues.put("possibleTumorSampleNamePrefixes", "")

        cValues.putAll(getAdapterTrimmingFile(roddyBamFile, combinedConfig))

        if (roddyBamFile.project.fingerPrinting && roddyBamFile.referenceGenome.fingerPrintingFileName) {
            cValues.put("runFingerprinting", "true")
            cValues.put("fingerprintingSitesFile", referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome).absolutePath)
        } else {
            cValues.put("runFingerprinting", "false")
        }

        return cValues
    }

    boolean getRunArriba(WorkflowStep workflowStep) {
        JsonNode combinedConfigJson = MAPPER.readTree(workflowStep.workflowRun.combinedConfig)
        Boolean runArriba = combinedConfigJson?.RODDY?.cvalues?.fields()?.find { it.key == ProjectService.RUN_ARRIBA }?.value?.value?.asBoolean()
        Boolean useSingleEndProcessing = combinedConfigJson?.RODDY?.cvalues?.fields()?.find { it.key == 'useSingleEndProcessing' }?.value?.value?.asBoolean()
        return ((runArriba == null || runArriba == Boolean.TRUE) && (useSingleEndProcessing == null || useSingleEndProcessing == Boolean.FALSE))
    }

    private Map<String, String> getAdapterTrimmingFile(RoddyBamFile roddyBamFile, String combinedConfig) {
        JsonNode combinedConfigJson = MAPPER.readTree(combinedConfig)
        boolean adapterTrimming = combinedConfigJson?.RODDY?.cvalues?.fields()?.find { it.key == "useAdaptorTrimming" }?.value?.value?.asBoolean()

        if (!roddyBamFile.seqType.isRna() && adapterTrimming) {
            String adapterFile = exactlyOneElement(
                    roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile.unique(),
                    "There is not exactly one adapter available for BAM file ${roddyBamFile}"
            )
            assert adapterFile: "There is exactly one adapter available for BAM file ${roddyBamFile}, but it is null"
            return ["CLIP_INDEX": "${adapterFile}"]
        }
        return [:]
    }

    Map<String, String> getFilesToMerge(RoddyBamFile roddyBamFile) {
        assert roddyBamFile

        List vbpSequenceFiles = []

        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            List<RawSequenceFile> rawSequenceFiles = seqTrack.sequenceFilesWhereIndexFileIsFalse
            rawSequenceFiles.sort { it.mateNumber }.each { RawSequenceFile rawSequenceFile ->
                vbpSequenceFiles.add(lsdfFilesService.getFileViewByPidPath(rawSequenceFile))
            }
        }
        return ["fastq_list": vbpSequenceFiles.join(";")]
    }

    Map<String, String> getChromosomeIndexParameterWithMitochondrion(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return ["CHROMOSOME_INDICES": "( ${sortedList.join(' ')} )"]
    }

    String createMetadataTable(List<SeqTrack> seqTracks) {
        StringBuilder builder = new StringBuilder()
        builder << "Sample\tLibrary\tPID\tReadLayout\tRun\tMate\tSequenceFile\n"
        builder << RawSequenceFile.findAllBySeqTrackInListAndIndexFile(seqTracks, false).sort { it.mateNumber }.collect { RawSequenceFile rawSequenceFile ->
            [
                    rawSequenceFile.sampleType.dirName, // it is correct that the header is 'Sample', this is because of the different names for the same things
                    rawSequenceFile.seqTrack.libraryDirectoryName,
                    rawSequenceFile.individual.pid,
                    rawSequenceFile.seqType.libraryLayoutDirName,
                    rawSequenceFile.run.dirName,
                    rawSequenceFile.mateNumber,
                    lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile),
            ].join("\t")
        }.join("\n")
        return builder.toString()
    }
}
