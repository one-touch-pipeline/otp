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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QcTrafficLightStatus
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils

@Rollback
@Integration
class SamplePairCheckerIntegrationSpec extends Specification implements IsRoddy {

    SamplePair createSamplePair(Map properties = [:]) {
        return DomainFactory.createSamplePairPanCan([
                mergingWorkPackage1: DomainFactory.createMergingWorkPackage([
                        seqType : SeqTypeService.wholeGenomePairedSeqType,
                        pipeline: DomainFactory.createPanCanPipeline(),
                ])
        ] + properties)
    }

    AbstractBamFile createRoddyBamFile(Map properties = [:]) {
        return createRoddyBamFile(
                properties, DomainFactory.createMergingWorkPackage([
                        seqType : DomainFactory.createWholeGenomeSeqType(),
                        pipeline: DomainFactory.createPanCanPipeline(),
                ]), RoddyBamFile
        )
    }

    void "bamFilesWithoutCategory, when some bam files has a category and some not, return only bam files without category"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        AbstractBamFile bamFile1 = createBamFile()
        AbstractBamFile bamFile2 = createBamFile()
        AbstractBamFile bamFile3 = createBamFile()
        AbstractBamFile bamFile4 = createBamFile()
        AbstractBamFile bamFile5 = createBamFile()
        List<AbstractBamFile> bamFiles = [bamFile1, bamFile2, bamFile3, bamFile4, bamFile5]

        DomainFactory.createSampleTypePerProjectForBamFile(bamFile1)
        DomainFactory.createSampleTypePerProjectForBamFile(bamFile2)

        List<AbstractBamFile> expected = [bamFile3, bamFile4, bamFile5]

        when:
        List returnValue = samplePairChecker.bamFilesWithoutCategory(bamFiles)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }

    @Unroll
    void "bamFilesWithCategory, when some bam files has the category #category and some not, return only bam files with category #category"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        AbstractBamFile bamFile1 = createBamFile()
        AbstractBamFile bamFile2 = createBamFile()
        AbstractBamFile bamFile3 = createBamFile()
        AbstractBamFile bamFile4 = createBamFile()
        AbstractBamFile bamFile5 = createBamFile()
        List<AbstractBamFile> bamFiles = [bamFile1, bamFile2, bamFile3, bamFile4, bamFile5]

        DomainFactory.createSampleTypePerProjectForBamFile(bamFile1)
        DomainFactory.createSampleTypePerProjectForBamFile(bamFile2)
        DomainFactory.createSampleTypePerProjectForBamFile(bamFile3, category)
        DomainFactory.createSampleTypePerProjectForBamFile(bamFile4, SampleTypePerProject.Category.CONTROL)
        DomainFactory.createSampleTypePerProjectForBamFile(bamFile5, category)

        List<AbstractBamFile> expected = [bamFile3, bamFile5]

        when:
        List returnValue = samplePairChecker.bamFilesWithCategory(bamFiles, category)

        then:
        TestCase.assertContainSame(expected, returnValue)

        where:
        category << [
                SampleTypePerProject.Category.IGNORED,
                SampleTypePerProject.Category.UNDEFINED,
        ]
    }

    void "bamFilesWithoutThreshold, when some bam files has a threshold  and some not, return only bam files without threshold"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        AbstractBamFile bamFile1 = createBamFile()
        AbstractBamFile bamFile2 = createBamFile()
        AbstractBamFile bamFile3 = createBamFile()
        AbstractBamFile bamFile4 = createBamFile()
        AbstractBamFile bamFile5 = createBamFile()
        List<AbstractBamFile> bamFiles = [bamFile1, bamFile2, bamFile3, bamFile4, bamFile5]

        DomainFactory.createProcessingThresholdsForBamFile(bamFile1)
        DomainFactory.createProcessingThresholdsForBamFile(bamFile2)

        List<AbstractBamFile> expected = [bamFile3, bamFile4, bamFile5]

        when:
        List returnValue = samplePairChecker.bamFilesWithoutThreshold(bamFiles)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }

    void "bamFilesWithoutSamplePair, when some bam files has a sample pair and some not, return only bam files without sample pair"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        AbstractBamFile bamFile1 = createBamFile()
        AbstractBamFile bamFile2 = createBamFile()
        AbstractBamFile bamFile3 = createBamFile()
        AbstractBamFile bamFile4 = createBamFile()
        AbstractBamFile bamFile5 = createBamFile()
        List<AbstractBamFile> bamFiles = [bamFile1, bamFile2, bamFile3, bamFile4, bamFile5]

        DomainFactory.createSamplePair(mergingWorkPackage1: bamFile1.mergingWorkPackage)
        DomainFactory.createSamplePair(mergingWorkPackage2: bamFile2.mergingWorkPackage)

        List<AbstractBamFile> expected = [bamFile3, bamFile4, bamFile5]

        when:
        List returnValue = samplePairChecker.bamFilesWithoutSamplePair(bamFiles)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }

    void "samplePairsForBamFiles, when some bam files has a samplePair and some not, return sample pair for the given bam files"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        AbstractBamFile bamFile1 = createBamFile()
        AbstractBamFile bamFile2 = createBamFile()
        AbstractBamFile bamFile3 = createBamFile()
        AbstractBamFile bamFile4 = createBamFile()
        AbstractBamFile bamFile5 = createBamFile()
        List<AbstractBamFile> bamFiles = [bamFile1, bamFile2, bamFile3, bamFile4, bamFile5]

        SamplePair samplePair1 = DomainFactory.createSamplePair(mergingWorkPackage1: bamFile1.mergingWorkPackage)
        SamplePair samplePair2 = DomainFactory.createSamplePair(mergingWorkPackage2: bamFile2.mergingWorkPackage)

        List<SamplePair> expected = [samplePair1, samplePair2]

        when:
        List returnValue = samplePairChecker.samplePairsForBamFiles(bamFiles)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }

    void "samplePairWithMissingBamFile, when not all sample pairs has both bam file objects, return bam files without both bam files"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        SamplePair samplePair1 = DomainFactory.createSamplePair(mergingWorkPackage1: DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline()))
        SamplePair samplePair2 = DomainFactory.createSamplePair(mergingWorkPackage1: DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline()))
        SamplePair samplePair3 = DomainFactory.createSamplePair(mergingWorkPackage1: DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline()))
        SamplePair samplePair4 = DomainFactory.createSamplePair(mergingWorkPackage1: DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline()))
        List<SamplePair> samplePairs = [samplePair1, samplePair2, samplePair3, samplePair4]

        createBamFile([workPackage: samplePair1.mergingWorkPackage1])
        createBamFile([workPackage: samplePair1.mergingWorkPackage2])
        createBamFile([workPackage: samplePair2.mergingWorkPackage1])
        createBamFile([workPackage: samplePair3.mergingWorkPackage2])

        List<SamplePair> expected = [samplePair2, samplePair3, samplePair4]

        when:
        List returnValue = samplePairChecker.samplePairWithMissingBamFile(samplePairs)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }

    @Unroll
    void "blockedSamplePairs, when case '#testcase', result contains expected values"() {
        given:
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        SamplePair samplePair = DomainFactory.createSamplePair(mergingWorkPackage1: DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline()))
        List<SeqTrack> seqTracks1 = []
        numberOfMergedLanes1.times {
            seqTracks1.add(createSeqTrack())
        }
        List<SeqTrack> seqTracks2 = []
        numberOfMergedLanes2.times {
            seqTracks2.add(createSeqTrack())
        }
        createBamFile([
                workPackage         : samplePair.mergingWorkPackage1,
                numberOfMergedLanes : numberOfMergedLanes1,
                seqTracks           : seqTracks1,
                coverage            : coverage1,
                withdrawn           : withdrawn1,
                fileOperationStatus : inProcessing1 ? AbstractBamFile.FileOperationStatus.INPROGRESS : AbstractBamFile.FileOperationStatus.PROCESSED,
                md5sum              : inProcessing1 ? null : HelperUtils.randomMd5sum,
                fileSize            : inProcessing1 ? 0 : DomainFactory.counter++,
                qcTrafficLightStatus: qcTrafficLightStatus1,
                comment             : DomainFactory.createComment(),
        ])
        createBamFile([
                workPackage         : samplePair.mergingWorkPackage2,
                numberOfMergedLanes : numberOfMergedLanes2,
                seqTracks           : seqTracks2,
                coverage            : coverage2,
                withdrawn           : withdrawn2,
                fileOperationStatus : inProcessing2 ? AbstractBamFile.FileOperationStatus.INPROGRESS : AbstractBamFile.FileOperationStatus.PROCESSED,
                md5sum              : inProcessing2 ? null : HelperUtils.randomMd5sum,
                fileSize            : inProcessing2 ? 0 : DomainFactory.counter++,
                qcTrafficLightStatus: qcTrafficLightStatus2,
                comment             : DomainFactory.createComment(),
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(samplePair.mergingWorkPackage2, SampleTypePerProject.Category.CONTROL)
        DomainFactory.createProcessingThresholdsForMergingWorkPackage(samplePair.mergingWorkPackage1, [coverage: 30.0, numberOfLanes: 3])
        DomainFactory.createProcessingThresholdsForMergingWorkPackage(samplePair.mergingWorkPackage2, [coverage: 30.0, numberOfLanes: 3])

        when:
        List<SamplePairChecker.BlockedSamplePair> result = samplePairChecker.blockedSamplePairs([samplePair])

        then:
        (text == null) == result.empty
        if (text) {
            String message = result[0]
            assert message.contains(text)
        }

        where:
        testcase                     | withdrawn1 | inProcessing1 | numberOfMergedLanes1 | coverage1 | qcTrafficLightStatus1         | withdrawn2 | inProcessing2 | numberOfMergedLanes2 | coverage2 | qcTrafficLightStatus2         || text
        'thresholds reached'         | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || null
        'disease withdrawn'          | true       | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || "disease ${SamplePairChecker.BLOCKED_BAM_IS_WITHDRAWN}"
        'disease is in processing'   | false      | true          | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || "disease ${SamplePairChecker.BLOCKED_BAM_IS_IN_PROCESSING}"
        'disease lane count to less' | false      | false         | 1                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || "disease ${SamplePairChecker.BLOCKED_TO_FEW_LANES}"
        'disease coverage to less'   | false      | false         | 4                    | 20        | QcTrafficLightStatus.ACCEPTED | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || "disease ${SamplePairChecker.BLOCKED_TO_FEW_COVERAGE}"
        'control withdrawn'          | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | true       | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || "control ${SamplePairChecker.BLOCKED_BAM_IS_WITHDRAWN}"
        'control is in processing'   | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | true          | 4                    | 40        | QcTrafficLightStatus.ACCEPTED || "control ${SamplePairChecker.BLOCKED_BAM_IS_IN_PROCESSING}"
        'control lane count to less' | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | false         | 1                    | 40        | QcTrafficLightStatus.ACCEPTED || "control ${SamplePairChecker.BLOCKED_TO_FEW_LANES}"
        'control coverage to less'   | false      | false         | 4                    | 40        | QcTrafficLightStatus.ACCEPTED | false      | false         | 4                    | 20        | QcTrafficLightStatus.ACCEPTED || "control ${SamplePairChecker.BLOCKED_TO_FEW_COVERAGE}"
    }

    void "handle, if no bam files given, do nothing"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        SamplePairChecker samplePairChecker = new SamplePairChecker()

        when:
        List<SamplePair> result = samplePairChecker.handle([], output)

        then:
        [] == result
        0 * output._
    }

    void "handle, if bam files given, then return non waiting sample pairs and create output for the others"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        SamplePairChecker samplePairChecker = Spy(SamplePairChecker)

        and: 'bam has not supported seq type'
        AbstractBamFile unsupportedSeqType = createBamFile(
                seqTracks: [createSeqTrack(seqType: createSeqType(name: "unsupported seq type", dirName: "unsupported_seq_type"))])

        and: 'sample type has no disease state'
        AbstractBamFile unknownDiseaseStatus = createRoddyBamFile()
        DomainFactory.createProcessingThresholdsForBamFile(unknownDiseaseStatus)

        and: 'sample type has disease state undefined'
        AbstractBamFile undefinedDiseaseStatus = createRoddyBamFile()
        DomainFactory.createSampleTypePerProjectForBamFile(undefinedDiseaseStatus, SampleTypePerProject.Category.UNDEFINED)
        DomainFactory.createProcessingThresholdsForBamFile(undefinedDiseaseStatus)

        and: 'sample type has disease state ignored'
        AbstractBamFile ignoredDiseaseStatus = createRoddyBamFile()
        DomainFactory.createSampleTypePerProjectForBamFile(ignoredDiseaseStatus, SampleTypePerProject.Category.IGNORED)
        DomainFactory.createProcessingThresholdsForBamFile(ignoredDiseaseStatus)

        and: 'sample type has no threshold'
        AbstractBamFile unknownThreshold = createRoddyBamFile()
        DomainFactory.createSampleTypePerProjectForBamFile(unknownThreshold, SampleTypePerProject.Category.DISEASE)

        and: 'na sample pair exist for bam file'
        AbstractBamFile noSamplePairFound = createRoddyBamFile()
        DomainFactory.createSampleTypePerProjectForBamFile(noSamplePairFound, SampleTypePerProject.Category.DISEASE)
        DomainFactory.createProcessingThresholdsForBamFile(noSamplePairFound)

        and: 'sample pair without disease bam file'
        AbstractBamFile missingDiseaseBamFile = createRoddyBamFile()
        DomainFactory.createSampleTypePerProjectForBamFile(missingDiseaseBamFile, SampleTypePerProject.Category.CONTROL)
        DomainFactory.createProcessingThresholdsForBamFile(missingDiseaseBamFile)
        SamplePair missingDiseaseSamplePair = DomainFactory.createSamplePair([mergingWorkPackage2: missingDiseaseBamFile.mergingWorkPackage])

        and: 'sample pair without control bam file'
        AbstractBamFile missingControlBamFile = createRoddyBamFile()
        DomainFactory.createSampleTypePerProjectForBamFile(missingControlBamFile, SampleTypePerProject.Category.DISEASE)
        DomainFactory.createProcessingThresholdsForBamFile(missingControlBamFile)
        SamplePair missingControlSamplePair = DomainFactory.createSamplePair([mergingWorkPackage1: missingControlBamFile.mergingWorkPackage])

        and: 'bam files of sample pair does not reached threshold'
        SamplePair thresholdSamplePair = createSamplePair()
        AbstractBamFile thresholdDiseaseBamFile = createRoddyBamFile([workPackage: thresholdSamplePair.mergingWorkPackage1, coverage: 10])
        DomainFactory.createProcessingThresholdsForBamFile(thresholdDiseaseBamFile, [coverage: 50])
        AbstractBamFile thresholdControlFile = createRoddyBamFile([workPackage: thresholdSamplePair.mergingWorkPackage2, coverage: 10])
        DomainFactory.createProcessingThresholdsForBamFile(thresholdControlFile, [coverage: 50])
        DomainFactory.createSampleTypePerProjectForBamFile(thresholdControlFile)

        and: 'fine sample pair'
        SamplePair fineSamplePair = createSamplePair()
        List<SeqTrack> seqTracks = [createSeqTrack(), createSeqTrack()]
        AbstractBamFile diseaseBamFile = createRoddyBamFile([
                workPackage        : fineSamplePair.mergingWorkPackage1,
                coverage           : 30,
                numberOfMergedLanes: 2,
                seqTracks          : seqTracks,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : DomainFactory.counter++,
        ])
        AbstractBamFile controlBamFile = createRoddyBamFile([
                workPackage        : fineSamplePair.mergingWorkPackage2,
                coverage           : 30,
                numberOfMergedLanes: 2,
                seqTracks          : seqTracks,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : DomainFactory.counter++,
        ])
        DomainFactory.createSampleTypePerProjectForBamFile(controlBamFile)
        DomainFactory.createProcessingThresholdsForBamFile(diseaseBamFile, [coverage: 10, numberOfLanes: 1])
        DomainFactory.createProcessingThresholdsForBamFile(controlBamFile, [coverage: 10, numberOfLanes: 1])

        and: 'other preparing'
        List<AbstractBamFile> bamFiles = [
                unsupportedSeqType,
                unknownDiseaseStatus,
                undefinedDiseaseStatus,
                ignoredDiseaseStatus,
                unknownThreshold,
                noSamplePairFound,
                missingDiseaseBamFile,
                missingControlBamFile,
                thresholdDiseaseBamFile,
                diseaseBamFile,
        ]
        List<SamplePair> samplePairs = [fineSamplePair]

        when:
        List<SamplePair> result = samplePairChecker.handle(bamFiles, output)

        then:
        1 * output.showWorkflowOldSystem(_, _)

        samplePairs == result

        then:
        1 * output.showUniqueNotSupportedSeqTypes([unsupportedSeqType], _)

        then:
        1 * samplePairChecker.bamFilesWithoutCategory(_)
        1 * output.showUniqueList(SamplePairChecker.HEADER_UNKNOWN_DISEASE_STATUS, [unknownDiseaseStatus], _)

        then:
        1 * samplePairChecker.bamFilesWithCategory(_, SampleTypePerProject.Category.UNDEFINED)
        1 * output.showUniqueList(SamplePairChecker.HEADER_DISEASE_STATE_UNDEFINED, [undefinedDiseaseStatus], _)

        then:
        1 * samplePairChecker.bamFilesWithCategory(_, SampleTypePerProject.Category.IGNORED)
        1 * output.showUniqueList(SamplePairChecker.HEADER_DISEASE_STATE_IGNORED, [ignoredDiseaseStatus], _)

        then:
        1 * samplePairChecker.bamFilesWithoutThreshold(_)
        1 * output.showUniqueList(SamplePairChecker.HEADER_UNKNOWN_THRESHOLD, [unknownThreshold], _)

        then:
        1 * samplePairChecker.bamFilesWithoutSamplePair(_)
        1 * output.showUniqueList(SamplePairChecker.HEADER_NO_SAMPLE_PAIR, [noSamplePairFound])

        then:
        1 * samplePairChecker.samplePairWithMissingBamFile(_)
        1 * output.showList(SamplePairChecker.HEADER_SAMPLE_PAIR_WITHOUT_DISEASE_BAM_FILE, [missingDiseaseSamplePair])
        1 * output.showList(SamplePairChecker.HEADER_SAMPLE_PAIR_WITHOUT_CONTROL_BAM_FILE, [missingControlSamplePair])

        then:
        1 * samplePairChecker.blockedSamplePairs(_)
        1 * output.showList(SamplePairChecker.HEADER_BLOCKED_SAMPLE_PAIRS, _) >> { String header, List<SamplePairChecker.BlockedSamplePair> blocked ->
            assert !blocked.empty
            assert blocked[0].samplePair == thresholdSamplePair
        }

        0 * output._
    }
}
