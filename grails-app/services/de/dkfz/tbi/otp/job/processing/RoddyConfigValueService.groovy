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
package de.dkfz.tbi.otp.job.processing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentLinkFileServiceFactoryService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Transactional
class RoddyConfigValueService {

    AlignmentLinkFileServiceFactoryService alignmentLinkFileServiceFactoryService
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService
    RawSequenceDataViewFileService rawSequenceDataViewFileService
    ProcessingOptionService processingOptionService
    ReferenceGenomeService referenceGenomeService

    private static final ObjectMapper MAPPER = new ObjectMapper()

    Map<String, Map<String, String>> getDefaultValues() {
        return [
                BASE_REFERENCE_GENOME   : createPathValueMap(processingOptionService.findOptionAsString(
                        ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME)),
                sharedFilesBaseDirectory: createPathValueMap(processingOptionService.findOptionAsString(
                        ProcessingOption.OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY)),
        ]
    }

    /**
     * Generates configuration values that are common to all alignment workflows
     */
    Map<String, Map<String, String>> getAlignmentValues(RoddyBamFile roddyBamFile, String combinedConfig) {
        assert roddyBamFile

        Map<String, Map<String, String>> cValues = [:]

        String referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath
        cValues.put("INDEX_PREFIX", createPathValueMap(referenceGenomeFastaFile)) // used for PanCancer pipeline
        cValues.put("GENOME_FA", createPathValueMap(referenceGenomeFastaFile)) // used for RNA pipeline

        cValues.put("possibleControlSampleNamePrefixes", createValueMap(roddyBamFile.sampleType.dirName))
        cValues.put("possibleTumorSampleNamePrefixes", createValueMap(""))

        cValues.putAll(getAdapterTrimmingFileForPanCancer(roddyBamFile, combinedConfig))

        if (roddyBamFile.project.fingerPrinting && roddyBamFile.referenceGenome.fingerPrintingFileName) {
            cValues.put("runFingerprinting", createBooleanValueMap("true"))
            cValues.put("fingerprintingSitesFile", createPathValueMap(referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome).absolutePath))
        } else {
            cValues.put("runFingerprinting", createBooleanValueMap("false"))
        }

        return cValues
    }

    @CompileDynamic
    boolean getRunArriba(WorkflowStep workflowStep) {
        JsonNode combinedConfigJson = MAPPER.readTree(workflowStep.workflowRun.combinedConfig)
        Boolean runArriba = combinedConfigJson?.RODDY?.cvalues?.fields()?.find { it.key == ProjectService.RUN_ARRIBA }?.value?.value?.asBoolean()
        Boolean useSingleEndProcessing = combinedConfigJson?.RODDY?.cvalues?.fields()?.find { it.key == 'useSingleEndProcessing' }?.value?.value?.asBoolean()
        return ((runArriba == null || runArriba == Boolean.TRUE) && (useSingleEndProcessing == null || useSingleEndProcessing == Boolean.FALSE))
    }

    /**
     * This Method returns whether adapter trimming should be used for the PanCancer or WGBS Workflow.
     */
    @CompileDynamic
    boolean isAdapterTrimmingUsedForPanCancer(String combinedConfig) {
        JsonNode combinedConfigJson = MAPPER.readTree(combinedConfig)
        return combinedConfigJson?.RODDY?.cvalues?.fields()?.find { it.key == "useAdaptorTrimming" }?.value?.value?.asBoolean()
    }

    /**
     * This Method is used to get the Adapter trimming File for the PanCancer and WGBS Workflow.
     */
    protected Map<String, Map<String, String>> getAdapterTrimmingFileForPanCancer(RoddyBamFile roddyBamFile, String combinedConfig) {
        boolean adapterTrimming = isAdapterTrimmingUsedForPanCancer(combinedConfig)

        if (!roddyBamFile.seqType.isRna() && adapterTrimming) {
            String adapterFile = exactlyOneElement(
                    roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile.unique(),
                    "There is not exactly one adapter available for BAM file ${roddyBamFile}"
            )
            assert adapterFile: "There is exactly one adapter available for BAM file ${roddyBamFile}, but it is null"
            return [CLIP_INDEX: createPathValueMap(adapterFile)]
        }
        return [:]
    }

    Map<String, Map<String, String>> getFilesToMerge(RoddyBamFile roddyBamFile) {
        assert roddyBamFile

        List<Path> vbpSequenceFiles = roddyBamFile.seqTracks
                .collectMany { SeqTrack seqTrack -> seqTrack.sequenceFilesWhereIndexFileIsFalse }
                .sort { RawSequenceFile rawSequenceFile -> rawSequenceFile.mateNumber }
                .collect { RawSequenceFile rawSequenceFile -> rawSequenceDataViewFileService.getFilePath(rawSequenceFile) }
        return [fastq_list: createValueMap(vbpSequenceFiles.join(";"))]
    }

    @CompileDynamic
    Map<String, Map<String, String>> getChromosomeIndexParameterWithMitochondrion(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return [CHROMOSOME_INDICES: createBashArrayValueMap("( ${sortedList.join(' ')} )")]
    }

    @CompileDynamic
    Map<String, Map<String, String>> getChromosomeIndexParameterWithoutMitochondrion(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return [CHROMOSOME_INDICES: createBashArrayValueMap("( ${sortedList.join(' ')} )")]
    }

    private Map<String, Map<String, String>> getAnalysisInputVersionCommon(BamFilePairAnalysis analysis, Closure<Path> extractPath) {
        AbstractBamFile bamFileDisease = analysis.sampleType1BamFile
        AbstractBamFile bamFileControl = analysis.sampleType2BamFile

        Path bamFileDiseasePath = extractPath(bamFileDisease)
        Path bamFileControlPath = extractPath(bamFileControl)

        return [
                bamfile_list                     : createValueMap("${bamFileControlPath};${bamFileDiseasePath}" as String),
                sample_list                      : createValueMap("${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}" as String),
                possibleTumorSampleNamePrefixes  : createValueMap(bamFileDisease.sampleType.dirName),
                possibleControlSampleNamePrefixes: createValueMap(bamFileControl.sampleType.dirName),
        ]
    }

    Map<String, Map<String, String>> getAnalysisInputVersion1(BamFilePairAnalysis analysis) {
        return getAnalysisInputVersionCommon(analysis) { AbstractBamFile bamFile ->
            alignmentLinkFileServiceFactoryService.getService(bamFile).getPathForFurtherProcessing(bamFile)
        }
    }

    Map<String, Map<String, String>> getAnalysisInputVersion2(BamFilePairAnalysis analysis, Path workDirectory) {
        return sampleExtractionVersion2RoddyParameters + getAnalysisInputVersionCommon(analysis) { AbstractBamFile bamFile ->
            workDirectory.resolve("${bamFile.sampleType.dirName}_${bamFile.individual.pid}_merged.mdup.bam")
        }
    }

    /**
     * Parameters so Roddy properly parses sample type names containing '_'
     */
    private Map<String, Map<String, String>> getSampleExtractionVersion2RoddyParameters() {
        return [
                selectSampleExtractionMethod           : createValueMap('version_2'),
                matchExactSampleName                   : createBooleanValueMap('true'),
                allowSampleTerminationWithIndex        : createBooleanValueMap('false'),
                useLowerCaseFilenameForSampleExtraction: createBooleanValueMap('false'),
        ]
    }

    @CompileDynamic
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
                    rawSequenceDataViewFileService.getFilePath(rawSequenceFile),
            ].join("\t")
        }.join("\n")
        return builder.toString()
    }

    Map<String, String> createValueMap(String value, RoddyConfigValueType type = RoddyConfigValueType.PLAIN) {
        Map<String, String> map = [
                value: value,
        ]
        if (type != RoddyConfigValueType.PLAIN) {
            map.put("type", type.roddyType)
            if (type == RoddyConfigValueType.BOOLEAN) {
                assert value in ["true", "false"]: "Boolean value must be 'true' or 'false'"
            }
        }
        return map
    }

    Map<String, String> createPathValueMap(String value) {
        return createValueMap(value, RoddyConfigValueType.PATH)
    }

    Map<String, String> createBooleanValueMap(String value) {
        return createValueMap(value, RoddyConfigValueType.BOOLEAN)
    }

    Map<String, String> createBashArrayValueMap(String value) {
        return createValueMap(value, RoddyConfigValueType.BASH_ARRAY)
    }
}
