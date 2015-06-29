package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ThreadUtils
import grails.test.spock.IntegrationSpec

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SamplePairFindSamplePairsForSettingNeedsProcessingTestsIntegrationSpec extends IntegrationSpec {

    SnvCallingInstanceTestData snvCallingInstanceTestData
    SamplePair samplePair
    Project project
    SampleType sampleType1
    SampleTypePerProject sampleType1Stpp

    def setup() {
        snvCallingInstanceTestData.createSnvObjects()

        samplePair = snvCallingInstanceTestData.samplePair
        samplePair.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save(failOnError: true, flush: true)

        project = samplePair.project
        sampleType1 = snvCallingInstanceTestData.samplePair.sampleType1
        sampleType1Stpp = exactlyOneElement(SampleTypePerProject.findAllWhere(project: project, sampleType: sampleType1))
    }

    void testProcessingStatusAlreadyNeedsProcessing() {
        samplePair.processingStatus = ProcessingStatus.NEEDS_PROCESSING
        assert samplePair.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    void testProcessingStatusDisabled() {
        samplePair.processingStatus = ProcessingStatus.DISABLED
        assert samplePair.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    void testStppProjectMismatch() {
        when:
        sampleType1Stpp.project = Project.build()
        assert sampleType1Stpp.save(failOnError: true, flush: true)

        then:
        assertFindsNothing()

        when:
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)

        then:
        assertFindsOne()
    }

    void testSampleType1CategoryControl() {
        sampleType1Stpp.category = SampleType.Category.CONTROL
        assert sampleType1Stpp.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    void testSampleType1CategoryUnknown() {
        sampleType1Stpp.delete(flush: true)

        expect:
        assertFindsNothing()
    }

    void testNoSnvCallingInstanceExists() {
        expect:
        assertFindsOne()
    }

    void testTwoResults() {
        def (ProcessedMergedBamFile bamFileTumor2, SamplePair samplePair2) = snvCallingInstanceTestData.createDisease(snvCallingInstanceTestData.bamFileControl.mergingWorkPackage)
        samplePair2.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair2.save(failOnError: true, flush: true)

        expect:
        TestCase.containSame(
                SamplePair.findSamplePairsForSettingNeedsProcessing(),
                [samplePair, samplePair2]
        )
    }

    void testSnvCallingInstanceForOtherSamplePairExists() {
        def (ProcessedMergedBamFile bamFileTumor2, SamplePair samplePair2) = snvCallingInstanceTestData.createDisease(snvCallingInstanceTestData.bamFileControl.mergingWorkPackage)

        SnvConfig snvConfig = new SnvConfig(
                    project: samplePair2.project,
                    seqType: samplePair2.seqType,
                    configuration: "test config",
                    externalScriptVersion: "v1",
            )
        assert snvConfig.save(flush: true, failOnError: true)


        snvCallingInstanceTestData.createAndSaveSnvCallingInstance(
                samplePair: samplePair2,
                sampleType1BamFile: bamFileTumor2,
                config: snvConfig
        )

        expect:
        assertFindsOne()
    }

    void testSnvCallingInstanceAlreadyExists() {
        when:
        SnvConfig snvConfig = new SnvConfig(
                project: project,
                seqType: snvCallingInstanceTestData.samplePair.seqType,
                configuration: "test config",
                externalScriptVersion: "v1",
        )
        assert snvConfig.save(flush: true, failOnError: true)

        final SnvCallingInstance instance = snvCallingInstanceTestData.createAndSaveSnvCallingInstance(
                sampleType1BamFile: snvCallingInstanceTestData.bamFileTumor,
                sampleType2BamFile: snvCallingInstanceTestData.bamFileControl,
                snvConfig: snvConfig,
        )

        then: 'one SnvCallingInstance without any SnvJobResult'
        assertFindsNothing()

        when:
        final SnvJobResult callingResult = snvCallingInstanceTestData.createAndSaveSnvJobResult(instance, SnvCallingStep.CALLING)

        then: 'one SnvCallingInstance with one non-withdrawn SnvJobResult'
        assertFindsNothing()

        when:
        final SnvJobResult annotationResult = snvCallingInstanceTestData.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_ANNOTATION, callingResult)

        then: 'one SnvCallingInstance with two non-withdrawn SnvJobResults'
        assertFindsNothing()

        when:
        annotationResult.withdrawn = true
        assert annotationResult.save(failOnError: true, flush: true)

        then: 'one SnvCallingInstance with one non-withdrawn and one withdrawn SnvJobResult'
        assertFindsOne()

        when:
        snvCallingInstanceTestData.createAndSaveSnvCallingInstance(
                instanceName: HelperUtils.uniqueString,
                sampleType1BamFile: snvCallingInstanceTestData.bamFileTumor,
                sampleType2BamFile: snvCallingInstanceTestData.bamFileControl,
                snvConfig: snvConfig,
        )

        then: 'one SnvCallingInstance with one non-withdrawn and one withdrawn SnvJobResult and one without any SnvJobResult'
        assertFindsNothing()
    }

    void testLaterSampleType1DataFileExists() {
        createSnvCallingInstanceAndLaterDataFile(sampleType1)

        expect:
        assertFindsOne()
    }

    void testLaterSampleType2DataFileExists() {
        createSnvCallingInstanceAndLaterDataFile(samplePair.sampleType2)

        expect:
        assertFindsOne()
    }

    void testLaterDataFileWithOtherFileTypeExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.fileType = FileType.build()
        assert dataFile.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    void testLaterWithdrawnDataFileExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.fileWithdrawn = true
        assert dataFile.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    void testLaterDataFileForOtherIndividualExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.seqTrack.sample = Sample.build(individual: Individual.build(project: project), sampleType: sampleType1)
        assert dataFile.seqTrack.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    void testLaterDataFileForOtherSampleTypeExists() {
        createSnvCallingInstanceAndLaterDataFile(Sample.build(individual: samplePair.individual, sampleType: SampleType.build()))

        expect:
        assertFindsNothing()
    }

    void testLaterDataFileForOtherSeqTypeExists() {
        final DataFile dataFile = createSnvCallingInstanceAndLaterDataFile(sampleType1)
        dataFile.seqTrack.seqType = SeqType.build()
        assert dataFile.seqTrack.save(failOnError: true, flush: true)

        expect:
        assertFindsNothing()
    }

    private DataFile createSnvCallingInstanceAndLaterDataFile(
            final SampleType sampleType,
            AbstractMergedBamFile bamfileTumor = snvCallingInstanceTestData.bamFileTumor,
            AbstractMergedBamFile bamfileControl = snvCallingInstanceTestData.bamFileControl,
            SnvConfig snvConfig = null) {
        return createSnvCallingInstanceAndLaterDataFile(exactlyOneElement(Sample.findAllBySampleType(sampleType)),
                bamfileTumor, bamfileControl, snvConfig)
    }

    private DataFile createSnvCallingInstanceAndLaterDataFile(
            final Sample sample,
            AbstractMergedBamFile bamfileTumor = snvCallingInstanceTestData.bamFileTumor,
            AbstractMergedBamFile bamfileControl = snvCallingInstanceTestData.bamFileControl,
            SnvConfig snvConfig = null) {
        final SnvCallingInstance instance = snvCallingInstanceTestData.createAndSaveSnvCallingInstance(
                sampleType1BamFile: bamfileTumor,
                sampleType2BamFile: bamfileControl,
                snvConfig: snvConfig ?: new SnvConfig(
                        project: bamfileTumor.project,
                        seqType: bamfileTumor.seqType,
                        configuration: "test config",
                        externalScriptVersion: "v1",
                ).save(flush: true, failOnError: true)
        )
        final SeqTrack seqTrack = SeqTrack.findWhere(sample: sample, seqType: instance.seqType)
        assert ThreadUtils.waitFor( { System.currentTimeMillis() > instance.latestDataFileCreationDate.time }, 1, 1)
        final DataFile dataFile = DomainFactory.buildSequenceDataFile(seqTrack: seqTrack)
        assert dataFile.dateCreated > instance.latestDataFileCreationDate
        return dataFile
    }

    void assertFindsNothing() {
        assert SamplePair.findSamplePairsForSettingNeedsProcessing().empty
    }

    void assertFindsOne() {
        assert exactlyOneElement(SamplePair.findSamplePairsForSettingNeedsProcessing()) == samplePair
    }
}
