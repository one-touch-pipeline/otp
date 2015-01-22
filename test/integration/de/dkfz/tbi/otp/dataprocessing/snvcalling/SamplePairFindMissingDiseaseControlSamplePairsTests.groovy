package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import org.joda.time.LocalDate
import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.ngsdata.*

class SamplePairFindMissingDiseaseControlSamplePairsTests {

    SeqType wholeGenome
    SeqType exome
    SeqType chipSeq
    FileType sequenceFileType

    Project project
    SampleType diseaseSampleType
    SampleType controlSampleType
    SampleTypePerProject diseaseStpp
    SampleTypePerProject controlStpp
    Individual individual
    Sample diseaseSample
    Sample controlSample
    SeqTrack diseaseSeqTrack
    SeqTrack controlSeqTrack
    DataFile dataFile

    @Before
    void before() {
        wholeGenome = SeqType.build(name: SeqTypeNames.WHOLE_GENOME.seqTypeName, libraryLayout: 'PAIRED')
        exome = SeqType.build(name: SeqTypeNames.EXOME.seqTypeName, libraryLayout: 'PAIRED')
        chipSeq = SeqType.build(name: SeqTypeNames.CHIP_SEQ.seqTypeName, libraryLayout: 'PAIRED')
        sequenceFileType = FileType.build(type: FileType.Type.SEQUENCE)

        project = Project.build()
        diseaseSampleType = SampleType.build()
        controlSampleType = SampleType.build()
        diseaseStpp = SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        controlStpp = SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        individual = Individual.build(project: project)
        diseaseSample = Sample.build(individual: individual, sampleType: diseaseSampleType)
        controlSample = Sample.build(individual: individual, sampleType: controlSampleType)
        diseaseSeqTrack = SeqTrack.build(sample: diseaseSample, seqType: wholeGenome)
        controlSeqTrack = SeqTrack.build(sample: controlSample, seqType: wholeGenome)
        dataFile = DataFile.build(seqTrack: SeqTrack.build(sample: Sample.build(individual: individual)))
    }

    @Test
    void testMatch() {
        assertFindsOne()
    }

    @Test
    void testSeqTypeNotProcessable() {
        diseaseSeqTrack.seqType = chipSeq
        assert diseaseSeqTrack.save()
        controlSeqTrack.seqType = chipSeq
        assert controlSeqTrack.save()
        assertFindsNothing()

        SeqTrack.build(sample: diseaseSample, seqType: wholeGenome)
        SeqTrack.build(sample: controlSample, seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testDiseaseSeqTypeMismatch() {
        diseaseSeqTrack.seqType = exome
        assert diseaseSeqTrack.save()
        assertFindsNothing()

        SeqTrack.build(sample: diseaseSample, seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testControlSeqTypeMismatch() {
        controlSeqTrack.seqType = exome
        assert controlSeqTrack.save()
        assertFindsNothing()

        SeqTrack.build(sample: controlSample, seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testDiseaseIndividualMismatch() {
        diseaseSample.individual = Individual.build(project: project)
        assert diseaseSample.save()
        assertFindsNothing()

        diseaseSeqTrack = SeqTrack.build(sample: Sample.build(individual: individual, sampleType: diseaseSampleType), seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testControlIndividualMismatch() {
        controlSample.individual = Individual.build(project: project)
        assert controlSample.save()
        assertFindsNothing()

        controlSeqTrack = SeqTrack.build(sample: Sample.build(individual: individual, sampleType: controlSampleType), seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppMissing() {
        diseaseStpp.delete()
        assertFindsNothing()
    }

    @Test
    void testControlStppMissing() {
        controlStpp.delete()
        assertFindsNothing()
    }

    @Test
    void testDiseaseStppProjectMismatch() {
        diseaseStpp.project = Project.build()
        assert diseaseStpp.save()
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppProjectMismatch() {
        controlStpp.project = Project.build()
        assert controlStpp.save()
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeMismatch() {
        diseaseStpp.sampleType = SampleType.build()
        assert diseaseStpp.save()
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppSampleTypeMismatch() {
        controlStpp.sampleType = SampleType.build()
        assert controlStpp.save()
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeCategoryIgnored() {
        diseaseStpp.category = SampleType.Category.IGNORED
        assert diseaseStpp.save()
        assertFindsNothing()
    }

    @Test
    void testControlStppSampleTypeCategoryIgnored() {
        controlStpp.category = SampleType.Category.IGNORED
        assert controlStpp.save()
        assertFindsNothing()
    }

    @Test
    void testBothStppDisease() {
        controlStpp.category = SampleType.Category.DISEASE
        assert controlStpp.save()
        assertFindsNothing()
    }

    @Test
    void testBothStppControl() {
        diseaseStpp.category = SampleType.Category.CONTROL
        assert diseaseStpp.save()
        assertFindsNothing()
    }

    @Test
    void testNoRecentDataFileForIndividual() {
        assert SamplePair.findMissingDiseaseControlSamplePairs(dataFile.dateCreated.plus(1)).empty
    }

    @Test
    void testDiseaseSeqTrackWithdrawn() {
        DataFile.build(seqTrack: diseaseSeqTrack, fileType: sequenceFileType, fileWithdrawn: true)
        assertFindsNothing()

        SeqTrack.build(sample: diseaseSample, seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testControlSeqTrackWithdrawn() {
        DataFile.build(seqTrack: controlSeqTrack, fileType: sequenceFileType, fileWithdrawn: true)
        assertFindsNothing()

        SeqTrack.build(sample: controlSample, seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testMatchingSamplePairAlreadyExists() {
        SamplePair.build(
                individual: individual,
                sampleType1: diseaseSampleType,
                sampleType2: controlSampleType,
                seqType: wholeGenome,
        )
        assertFindsNothing()
    }

    @Test
    void testSamplePairWithOtherIndividualExists() {
        SamplePair.build(
                individual: Individual.build(project: project),
                sampleType1: diseaseSampleType,
                sampleType2: controlSampleType,
                seqType: wholeGenome,
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType1Exists() {
        final SampleType sampleType1 = SampleType.build()
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        SamplePair.build(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: controlSampleType,
                seqType: wholeGenome,
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType2Exists() {
        SamplePair.build(
                individual: individual,
                sampleType1: diseaseSampleType,
                sampleType2: SampleType.build(),
                seqType: wholeGenome,
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSeqTypeExists() {
        SamplePair.build(
                individual: individual,
                sampleType1: diseaseSampleType,
                sampleType2: controlSampleType,
                seqType: exome,
        )
        assertFindsOne()
    }

    @Test
    void testDistinct() {
        SeqTrack.build(sample: diseaseSample, seqType: wholeGenome)
        SeqTrack.build(sample: controlSample, seqType: wholeGenome)
        assertFindsOne()
    }

    @Test
    void testFindTwo() {
        SeqTrack.build(sample: diseaseSample, seqType: exome)
        SeqTrack.build(sample: controlSample, seqType: exome)
        final List<SamplePair> samplePairs =
                SamplePair.findMissingDiseaseControlSamplePairs(dataFile.dateCreated).sort { it.seqType.name }
        assert samplePairs.size() == 2
        assertEqualsAndNotPersisted(samplePairs[0], individual, diseaseSampleType, controlSampleType, exome)
        assertEqualsAndNotPersisted(samplePairs[1], individual, diseaseSampleType, controlSampleType, wholeGenome)
    }

    void assertFindsNothing() {
        assert SamplePair.findMissingDiseaseControlSamplePairs(dataFile.dateCreated).empty
    }

    void assertFindsOne() {
        assertEqualsAndNotPersisted(
                exactlyOneElement(SamplePair.findMissingDiseaseControlSamplePairs(dataFile.dateCreated)),
                individual, diseaseSampleType, controlSampleType, wholeGenome)
    }

    void assertEqualsAndNotPersisted(final SamplePair samplePair, final Individual individual,
                          final SampleType sampleType1, final SampleType sampleType2, final SeqType seqType) {
        assert samplePair.individual  == individual &&
               samplePair.sampleType1 == sampleType1 &&
               samplePair.sampleType2 == sampleType2 &&
               samplePair.seqType     == seqType &&
               samplePair.id          == null
    }
}
