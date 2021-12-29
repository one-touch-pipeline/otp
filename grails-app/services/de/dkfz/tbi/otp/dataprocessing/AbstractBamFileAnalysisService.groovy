/*
 * Copyright 2011-2020 The OTP authors
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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.FileInconsistencyException
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.IndividualService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

@Transactional
abstract class AbstractBamFileAnalysisService<T extends BamFilePairAnalysis> implements BamFileAnalysisServiceTrait {

    AbstractMergedBamFileService abstractMergedBamFileService
    IndividualService individualService
    ProcessingOptionService processingOptionService

    static final List<AnalysisProcessingStates> PROCESSING_STATES_NOT_PROCESSABLE = [
            AnalysisProcessingStates.IN_PROGRESS,
    ]

    /**
     * The method goes through the list of sample pairs and checks if there is a disease/control pair
     * which can be processed and returns it.
     * Criteria to pass before being processed are:
     * - bam & bai file available for disease and control
     * - if coverage threshold is given: coverage higher than coverage threshold
     * - if lane number threshold is given: lane number has reached lane number threshold
     * - processing of disease & control files finished -> transfer completed
     * - all not withdrawn lanes for these samples, available in OTP, are already merged in the bam files
     * - disease/control pair listed in {@link SamplePair}
     * - pair is not set to IGNORED
     * - pair is not already in processing
     * - config file is available
     */
    @SuppressWarnings('SpaceInsideParentheses')
    SamplePair samplePairForProcessing(int minPriority, SamplePair sp = null) {
        final String WORKPACKAGE = "workPackage"
        final String SAMPLE = "${WORKPACKAGE}.sample"
        final String SAMPLE_TYPE = "${SAMPLE}.sampleType"
        final String SEQ_TYPE = "${WORKPACKAGE}.seqType"
        final String INDIVIDUAL = "${SAMPLE}.individual"

        double threshold = processingOptionService.findOptionAsDouble(ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE, getAnalysisType().toString())

        def testIfBamFileFulfillCriteria = { String number ->
            return "AND EXISTS (FROM AbstractMergedBamFile ambf${number} " +
            // check that the file is not withdrawn
            "       WHERE ambf${number}.withdrawn = false " +
            //check that the bam file belongs to the SamplePair
            "       AND ambf${number}.${WORKPACKAGE} = sp.mergingWorkPackage${number} " +
            //check that transfer workflow is finished
            "       AND ambf${number}.md5sum IS NOT NULL " +
                    pipelineSpecificBamFileChecks(number) +
                    //checks that qc of the bam file is okay
            "       AND (ambf${number}.qcTrafficLightStatus is null OR ambf${number}.qcTrafficLightStatus NOT IN (:rejecetedQcTrafficLightStatus))" +

            //check that coverage is high enough & number of lanes are enough
            "       AND EXISTS ( FROM ProcessingThresholds pt " +
            "           WHERE pt.project = ambf${number}.${INDIVIDUAL}.project " +
            "           AND pt.seqType = ambf${number}.${SEQ_TYPE} " +
            "           AND pt.sampleType = ambf${number}.${SAMPLE_TYPE} " +
            "           AND (pt.coverage is null OR ambf${number}.coverage IS NULL OR pt.coverage <= ambf${number}.coverage) " +
            "           AND (:threshold <= ambf${number}.coverage OR ambf${number}.coverage IS NULL) " +
            "           AND (pt.numberOfLanes is null OR ambf${number}.numberOfMergedLanes IS NULL OR pt.numberOfLanes <= ambf${number}.numberOfMergedLanes) " +
            "           ) " +
            //check that the file is in the workpackage
            "       AND ambf${number}.${WORKPACKAGE}.bamFileInProjectFolder = ambf${number} " +
            //check that the file file operation status ist processed
            "       AND ambf${number}.fileOperationStatus = '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}' " +
            //check that the id is the last for that MergingWorkPackage
            "       AND ambf${number} = (select max(bamFile.id) from AbstractMergedBamFile bamFile where bamFile.workPackage = ambf${number}.workPackage)" +
            "       ) "
        }

        String samplePairForProcessing =
                "FROM SamplePair sp " +
                //check that sample pair shall be processed
                "WHERE " + getProcessingStateCheck() +

                (sp ? "AND sp = :sp " : '') +
                //check that processing priority of the corresponding project is high enough
                'AND sp.mergingWorkPackage1.sample.individual.project.processingPriority.priority >= :minPriority ' +
                'AND sp.mergingWorkPackage1.seqType in (:seqTypes) ' +
                checkReferenceGenome() +


                //check that the config file is available with at least one script with same version
                checkConfig() +


                //check that this sample pair is not in process
                "AND NOT EXISTS (FROM ${getAnalysisClass().name} sci " +
                "   WHERE sci.samplePair = sp " +
                "   AND sci.processingState IN (:processingStates) " +
                "   AND sci.withdrawn = false " +
                ") " +

                //check that the first bam file fulfill the criteria
                testIfBamFileFulfillCriteria("1") +

                //check that the second bam file fulfill the criteria
                testIfBamFileFulfillCriteria("2") +

                "ORDER BY sp.mergingWorkPackage1.sample.individual.project.processingPriority.priority DESC, sp.dateCreated"

        Map parameters = [
                needsProcessing: ProcessingStatus.NEEDS_PROCESSING,
                processingStates: PROCESSING_STATES_NOT_PROCESSABLE,
                minPriority: minPriority,
                analysis: getAnalysisType(),
                seqTypes: seqTypes,
                threshold: threshold,
                rejecetedQcTrafficLightStatus: [AbstractMergedBamFile.QcTrafficLightStatus.REJECTED, AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED],
        ]
        if (sp) {
            parameters.sp = sp
        }
        parameters.putAll(checkReferenceGenomeMap())

        return SamplePair.find(samplePairForProcessing.toString(), parameters)
    }

    void validateInputBamFiles(final BamFilePairAnalysis analysis) throws Throwable {
        try {
            abstractMergedBamFileService.getExistingBamFilePath(analysis.sampleType1BamFile)
            abstractMergedBamFileService.getExistingBamFilePath(analysis.sampleType2BamFile)
        } catch (final AssertionError e) {
            throw new FileInconsistencyException('The input BAM files have changed on the file system while this job processed them.', e)
        }
    }

    static void withdraw(BamFilePairAnalysis bamFilePairAnalysis) {
        BamFilePairAnalysis.withTransaction {
            bamFilePairAnalysis.withdrawn = true
            assert bamFilePairAnalysis.save(flush: true)
        }
    }

    @SuppressWarnings("UnusedMethodParameter")
    protected String pipelineSpecificBamFileChecks(String number) {
        return ''
    }

    final Pipeline getPipeline() {
        return CollectionUtils.<Pipeline> exactlyOneElement(Pipeline.findAllByName(getPipelineName()))
    }

    final List<SeqType> getSeqTypes() {
        return getPipelineName().getSeqTypes()
    }

    abstract protected String getProcessingStateCheck()
    abstract Class<BamFilePairAnalysis> getAnalysisClass()
    abstract protected Pipeline.Type getAnalysisType()
    abstract Pipeline.Name getPipelineName()

    String checkConfig() {
        return "AND EXISTS (FROM ${getConfigName()} cps " +
                "   WHERE cps.project = sp.mergingWorkPackage1.sample.individual.project " +
                "   AND cps.pipeline.type = :analysis " +
                "   AND cps.obsoleteDate is null " +
                "   AND cps.seqType = sp.mergingWorkPackage1.seqType " +
                ") "
    }

    Path getSamplePairPath(SamplePair samplePair) {
        return individualService.getViewByPidPath(samplePair.individual, samplePair.seqType)
                .resolve(resultsPathPart)
                .resolve(samplePair.seqType.libraryLayoutDirName)
                .resolve("${samplePair.sampleType1.dirName}_${samplePair.sampleType2.dirName}")
    }

    Path getWorkDirectory(T instance) {
        return getSamplePairPath(instance.samplePair).resolve(instance.instanceName)
    }

    protected abstract String getResultsPathPart()
}
