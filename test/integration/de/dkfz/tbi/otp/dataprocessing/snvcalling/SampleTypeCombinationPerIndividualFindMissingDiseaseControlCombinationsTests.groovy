package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import org.junit.Before
import org.junit.Test
import de.dkfz.tbi.otp.ngsdata.*

class SampleTypeCombinationPerIndividualFindMissingDiseaseControlCombinationsTests {

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
    void testMatchingStcpiAlreadyExists() {
        SampleTypeCombinationPerIndividual.build(
                individual: individual,
                sampleType1: diseaseSampleType,
                sampleType2: controlSampleType,
                seqType: wholeGenome,
        )
        assertFindsNothing()
    }

    @Test
    void testStcpiWithOtherIndividualExists() {
        SampleTypeCombinationPerIndividual.build(
                individual: Individual.build(project: project),
                sampleType1: diseaseSampleType,
                sampleType2: controlSampleType,
                seqType: wholeGenome,
        )
        assertFindsOne()
    }

    @Test
    void testStcpiWithOtherSampleType1Exists() {
        final SampleType sampleType1 = SampleType.build()
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        SampleTypeCombinationPerIndividual.build(
                individual: individual,
                sampleType1: sampleType1,
                sampleType2: controlSampleType,
                seqType: wholeGenome,
        )
        assertFindsOne()
    }

    @Test
    void testStcpiWithOtherSampleType2Exists() {
        SampleTypeCombinationPerIndividual.build(
                individual: individual,
                sampleType1: diseaseSampleType,
                sampleType2: SampleType.build(),
                seqType: wholeGenome,
        )
        assertFindsOne()
    }

    @Test
    void testStcpiWithOtherSeqTypeExists() {
        SampleTypeCombinationPerIndividual.build(
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
        final List<SampleTypeCombinationPerIndividual> combinations =
                SampleTypeCombinationPerIndividual.findMissingDiseaseControlCombinations().sort { it.seqType.name }
        assert combinations.size() == 2
        assertEqualsAndNotPersisted(combinations[0], individual, diseaseSampleType, controlSampleType, exome)
        assertEqualsAndNotPersisted(combinations[1], individual, diseaseSampleType, controlSampleType, wholeGenome)
    }

    void assertFindsNothing() {
        assert SampleTypeCombinationPerIndividual.findMissingDiseaseControlCombinations().empty
    }

    void assertFindsOne() {
        assertEqualsAndNotPersisted(
                exactlyOneElement(SampleTypeCombinationPerIndividual.findMissingDiseaseControlCombinations()),
                individual, diseaseSampleType, controlSampleType, wholeGenome)
    }

    void assertEqualsAndNotPersisted(final SampleTypeCombinationPerIndividual combination, final Individual individual,
                          final SampleType sampleType1, final SampleType sampleType2, final SeqType seqType) {
        assert combination.individual  == individual &&
               combination.sampleType1 == sampleType1 &&
               combination.sampleType2 == sampleType2 &&
               combination.seqType     == seqType &&
               combination.id          == null
    }
}
