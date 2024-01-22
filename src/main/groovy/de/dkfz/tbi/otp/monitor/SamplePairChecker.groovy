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
package de.dkfz.tbi.otp.monitor

import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
class SamplePairChecker extends PipelinesChecker<AbstractBamFile> {

    static final String HEADER_UNKNOWN_DISEASE_STATUS =
            'For the following project sampleType combination the sampleType was not classified as disease or control'
    static final String HEADER_DISEASE_STATE_IGNORED =
            'For the following project sampleType combination the sampleType category is set to IGNORED'
    static final String HEADER_DISEASE_STATE_UNDEFINED =
            'For the following project sampleType combination the sampleType category is set to UNDEFINED'
    static final String HEADER_UNKNOWN_THRESHOLD =
            'For the following project sampleType seqType combination no threshold is defined'
    static final String HEADER_NO_SAMPLE_PAIR =
            'For the following BamFile no SamplePair could be found'
    static final String HEADER_SAMPLE_PAIR_WITHOUT_DISEASE_BAM_FILE =
            'The following samplePairs have no disease bam file'
    static final String HEADER_SAMPLE_PAIR_WITHOUT_CONTROL_BAM_FILE =
            'The following samplePairs have no control bam file'
    static final String HEADER_BLOCKED_SAMPLE_PAIRS =
            'The following samplePairs are waiting'

    static final String BLOCKED_BAM_IS_WITHDRAWN = "bam file is withdrawn"
    static final String BLOCKED_BAM_IS_IN_PROCESSING = "bam file is in processing"
    static final String BLOCKED_TO_FEW_LANES = "bam file has too few lanes"
    static final String BLOCKED_TO_FEW_COVERAGE = "bam file has insufficient coverage"

    @Override
    List<SamplePair> handle(List<AbstractBamFile> bamFilesInput, MonitorOutputCollector output) {
        if (!bamFilesInput) {
            return []
        }
        output.showWorkflowOldSystem("Sample pairs", false)

        List<SeqType> supportedSeqTypes = SeqTypeService.allAnalysableSeqTypes

        Map bamFileOfSupportedSeqType = bamFilesInput.groupBy {
            supportedSeqTypes.contains(it.seqType)
        }

        if (bamFileOfSupportedSeqType[false]) {
            output.showUniqueNotSupportedSeqTypes(bamFileOfSupportedSeqType[false]) { AbstractBamFile abstractBamFile ->
                "${abstractBamFile.seqType.displayNameWithLibraryLayout}"
            }
        }

        List<AbstractBamFile> bamFiles = bamFileOfSupportedSeqType[true] ?: []

        List<AbstractBamFile> unknownDiseaseStatus = bamFilesWithoutCategory(bamFiles)
        output.showUniqueList(HEADER_UNKNOWN_DISEASE_STATUS, unknownDiseaseStatus) {
            "${it.project.name} ${it.sampleType.name}"
        }

        bamFiles = bamFiles - unknownDiseaseStatus

        List<AbstractBamFile> undefinedDiseaseStatus = bamFilesWithCategory(bamFiles, SampleTypePerProject.Category.UNDEFINED)
        output.showUniqueList(HEADER_DISEASE_STATE_UNDEFINED, undefinedDiseaseStatus) {
            "${it.project.name} ${it.sampleType.name}"
        }
        bamFiles = bamFiles - undefinedDiseaseStatus

        List<AbstractBamFile> ignoredDiseaseStatus = bamFilesWithCategory(bamFiles, SampleTypePerProject.Category.IGNORED)
        output.showUniqueList(HEADER_DISEASE_STATE_IGNORED, ignoredDiseaseStatus) {
            "${it.project.name} ${it.sampleType.name}"
        }
        bamFiles = bamFiles - ignoredDiseaseStatus

        List<AbstractBamFile> unknownThreshold = bamFilesWithoutThreshold(bamFiles)
        output.showUniqueList(HEADER_UNKNOWN_THRESHOLD, unknownThreshold) {
            "${it.project.name} ${it.sampleType.name} ${it.seqType.name}"
        }
        bamFiles = bamFiles - unknownThreshold

        List<AbstractBamFile> noPairFound = bamFilesWithoutSamplePair(bamFiles)
        output.showUniqueList(HEADER_NO_SAMPLE_PAIR, noPairFound)
        bamFiles = bamFiles - noPairFound

        List<SamplePair> allSamplePairs = samplePairsForBamFiles(bamFiles)

        List<SamplePair> samplePairWithoutBamFile = samplePairWithMissingBamFile(allSamplePairs)
        Map diseaseBamFileMissed = samplePairWithoutBamFile.groupBy {
            CollectionUtils.atMostOneElement(AbstractBamFile.findAllByWorkPackage(it.mergingWorkPackage1, [max: 1])) == null
        }
        output.showList(HEADER_SAMPLE_PAIR_WITHOUT_DISEASE_BAM_FILE, diseaseBamFileMissed[true])
        output.showList(HEADER_SAMPLE_PAIR_WITHOUT_CONTROL_BAM_FILE, diseaseBamFileMissed[false])

        List<BlockedSamplePair> waitingSamplePairs = blockedSamplePairs(allSamplePairs)
        output.showList(HEADER_BLOCKED_SAMPLE_PAIRS, waitingSamplePairs)

        return allSamplePairs - samplePairWithoutBamFile - waitingSamplePairs*.samplePair
    }

    List<AbstractBamFile> bamFilesWithoutCategory(List<AbstractBamFile> bamFiles) {
        if (!bamFiles) {
            return []
        }
        return AbstractBamFile.executeQuery("""
                select
                    bamFile
                from
                    AbstractBamFile bamFile
                where
                    bamFile in (:bamFiles)
                    and not exists (
                        select
                            sampleTypePerProject
                        from
                            SampleTypePerProject sampleTypePerProject
                        where
                            sampleTypePerProject.project = bamFile.workPackage.sample.individual.project
                            and sampleTypePerProject.sampleType = bamFile.workPackage.sample.sampleType
                    )
            """, [
                bamFiles: bamFiles,
        ])
    }

    List<AbstractBamFile> bamFilesWithCategory(List<AbstractBamFile> bamFiles, SampleTypePerProject.Category category) {
        if (!bamFiles) {
            return []
        }
        return AbstractBamFile.executeQuery("""
                select
                    bamFile
                from
                    AbstractBamFile bamFile,
                    SampleTypePerProject sampleTypePerProject
                where
                    bamFile in (:bamFiles)
                    and sampleTypePerProject.project = bamFile.workPackage.sample.individual.project
                    and sampleTypePerProject.sampleType = bamFile.workPackage.sample.sampleType
                    and sampleTypePerProject.category = '${category}'
            """.toString(), [
                bamFiles: bamFiles,
        ])
    }

    List<AbstractBamFile> bamFilesWithoutThreshold(List<AbstractBamFile> bamFiles) {
        if (!bamFiles) {
            return []
        }
        return AbstractBamFile.executeQuery("""
                select
                    bamFile
                from
                    AbstractBamFile bamFile
                where
                    bamFile in (:bamFiles)
                    and not exists (
                        select
                            processingThresholds
                        from
                            ProcessingThresholds processingThresholds
                        where
                            processingThresholds.project = bamFile.workPackage.sample.individual.project
                            and processingThresholds.sampleType = bamFile.workPackage.sample.sampleType
                            and processingThresholds.seqType = bamFile.workPackage.seqType
                    )
            """, [
                bamFiles: bamFiles,
        ])
    }

    List<AbstractBamFile> bamFilesWithoutSamplePair(List<AbstractBamFile> bamFiles) {
        if (!bamFiles) {
            return []
        }
        return AbstractBamFile.executeQuery("""
                select
                    bamFile
                from
                    AbstractBamFile bamFile
                where
                    bamFile in (:bamFiles)
                    and not exists (
                        select
                            samplePair
                        from
                            SamplePair samplePair
                        where
                            mergingWorkPackage1 = bamFile.workPackage
                            or mergingWorkPackage2 = bamFile.workPackage
                    )
            """, [
                bamFiles: bamFiles,
        ])
    }

    List<SamplePair> samplePairsForBamFiles(List<AbstractBamFile> bamFiles) {
        if (!bamFiles) {
            return []
        }
        List<MergingWorkPackage> mergingWorkPackages = bamFiles*.mergingWorkPackage
        return SamplePair.createCriteria().list {
            or {
                'in'('mergingWorkPackage1', mergingWorkPackages)
                'in'('mergingWorkPackage2', mergingWorkPackages)
            }
        }
    }

    List<SamplePair> samplePairWithMissingBamFile(List<SamplePair> allSamplePairs) {
        if (!allSamplePairs) {
            return []
        }
        return SamplePair.executeQuery("""
                select
                    samplePair
                from
                    SamplePair samplePair
                where
                    samplePair in (:samplePair)
                    and (
                        not exists (
                            select
                                bamFile
                            from
                                AbstractBamFile bamFile
                            where
                                bamFile.workPackage = samplePair.mergingWorkPackage1
                        ) or not exists (
                            select
                                bamFile
                            from
                                AbstractBamFile bamFile
                            where
                                bamFile.workPackage = samplePair.mergingWorkPackage2
                        )
                    )
            """, [
                samplePair: allSamplePairs,
        ])
    }

    List<BlockedSamplePair> blockedSamplePairs(List<SamplePair> allSamplePairs) {
        if (!allSamplePairs) {
            return []
        }
        def connectDomains = { String number ->
            return """(
                        bamFile${number}.workPackage = samplePair.mergingWorkPackage${number}
                        and sampleTypePerProject${number}.project = samplePair.mergingWorkPackage${number}.sample.individual.project
                        and sampleTypePerProject${number}.sampleType = samplePair.mergingWorkPackage${number}.sample.sampleType
                        and sampleTypePerProject${number}.category in ('${SampleTypePerProject.Category.DISEASE}', '${SampleTypePerProject.Category.CONTROL}')
                        and processingThresholds${number}.project = samplePair.mergingWorkPackage${number}.sample.individual.project
                        and processingThresholds${number}.sampleType = samplePair.mergingWorkPackage${number}.sample.sampleType
                        and processingThresholds${number}.seqType = samplePair.mergingWorkPackage${number}.seqType
                        and bamFile${number}.id = (
                            select
                                max(bamFile.id)
                            from
                                AbstractBamFile bamFile
                            where
                                bamFile.workPackage = bamFile${number}.workPackage
                        )
                    )"""
        }

        def testBamFileIsBlocked = { String number ->
            return """(
                            bamFile${number}.withdrawn = true
                            or bamFile${number}.md5sum is null
                            or bamFile${number}.coverage < processingThresholds${number}.coverage
                            or bamFile${number}.numberOfMergedLanes < processingThresholds${number}.numberOfLanes
                        )"""
        }

        return SamplePair.executeQuery("""
                select
                    new ${BlockedSamplePair.name} (
                        samplePair,
                        bamFile1,
                        bamFile2,
                        processingThresholds1,
                        processingThresholds2
                    )
                from
                    SamplePair samplePair,
                    AbstractBamFile bamFile1,
                    AbstractBamFile bamFile2,
                    ProcessingThresholds processingThresholds1,
                    ProcessingThresholds processingThresholds2,
                    SampleTypePerProject sampleTypePerProject1,
                    SampleTypePerProject sampleTypePerProject2
                where
                    samplePair in (:samplePair)
                    and ${connectDomains('1')}
                    and ${connectDomains('2')}
                    and (
                        ${testBamFileIsBlocked('1')}
                        or ${testBamFileIsBlocked('2')}
                    )
            """.toString(), [
                samplePair: allSamplePairs,
        ])
    }

    @CompileDynamic
    @TupleConstructor
    static class BlockedSamplePair {
        SamplePair samplePair
        AbstractBamFile bamFile1
        AbstractBamFile bamFile2
        ProcessingThresholds processingThresholds1
        ProcessingThresholds processingThresholds2

        @Override
        String toString() {
            List<String> reasonsForBlocking = []
            [
                    disease: [
                            bamFile             : bamFile1,
                            processingThresholds: processingThresholds1,
                    ],
                    control: [
                            bamFile             : bamFile2,
                            processingThresholds: processingThresholds2,
                    ],
            ].each { String key, Map map ->
                AbstractBamFile bamFile = map.bamFile
                ProcessingThresholds processingThresholds = map.processingThresholds
                if (bamFile.withdrawn) {
                    reasonsForBlocking << "${key} ${BLOCKED_BAM_IS_WITHDRAWN}"
                } else if (bamFile.md5sum == null) {
                    reasonsForBlocking << "${key} ${BLOCKED_BAM_IS_IN_PROCESSING}"
                } else {
                    if (!processingThresholds.isAboveLaneThreshold(bamFile)) {
                        reasonsForBlocking << "${key} ${BLOCKED_TO_FEW_LANES} (${bamFile.numberOfMergedLanes} of ${processingThresholds.numberOfLanes})"
                    }
                    if (!processingThresholds.isAboveCoverageThreshold(bamFile)) {
                        reasonsForBlocking << "${key} ${BLOCKED_TO_FEW_COVERAGE} (${bamFile.coverage.round(2)} of ${processingThresholds.coverage})"
                    }
                }
            }
            return "${samplePair} (${reasonsForBlocking.join(', ')})"
        }
    }
}
