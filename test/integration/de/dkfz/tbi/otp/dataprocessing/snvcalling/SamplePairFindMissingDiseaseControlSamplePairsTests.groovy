package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class SamplePairFindMissingDiseaseControlSamplePairsTests {

    SeqType wholeGenome
    SeqType exome
    SeqType rna

    Project project
    SampleType diseaseSampleType
    SampleType controlSampleType
    SampleTypePerProject diseaseStpp
    SampleTypePerProject controlStpp
    Individual individual
    Sample diseaseSample
    Sample controlSample
    MergingWorkPackage diseaseMwp
    MergingWorkPackage controlMwp

    @Before
    void before() {
        wholeGenome = DomainFactory.createWholeGenomeSeqType()
        exome = DomainFactory.createExomeSeqType()
        rna = DomainFactory.createRnaPairedSeqType()
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()

        project = Project.build()
        diseaseSampleType = SampleType.build()
        controlSampleType = SampleType.build()
        diseaseStpp = SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        controlStpp = SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        individual = Individual.build(project: project)
        diseaseSample = Sample.build(individual: individual, sampleType: diseaseSampleType)
        controlSample = Sample.build(individual: individual, sampleType: controlSampleType)
        diseaseMwp = DomainFactory.createMergingWorkPackage(sample: diseaseSample, seqType: wholeGenome, libraryPreparationKit: libraryPreparationKit)
        controlMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, controlSample)
    }

    @Test
    void testMatch() {
        assertFindsOne(diseaseMwp, controlMwp)
    }

    @Test
    void testDiseaseSeqTypeMismatch() {
        diseaseMwp.seqType = exome
        assert diseaseMwp.save(flush: true)
        assertFindsNothing()

        assertFindsOne(DomainFactory.createMergingWorkPackage(controlMwp, diseaseSample), controlMwp)
    }

    @Test
    void testControlSeqTypeMismatch() {
        controlMwp.seqType = exome
        assert controlMwp.save(flush: true)
        assertFindsNothing()

        assertFindsOne(diseaseMwp, DomainFactory.createMergingWorkPackage(diseaseMwp, controlSample))
    }

    @Test
    void testNotAnalysableSeqType() {
        diseaseMwp.seqType = rna
        assert diseaseMwp.save(flush: true)
        controlMwp.seqType = rna
        assert controlMwp.save(flush: true)

        assertFindsNothing()
    }

    @Test
    void testDiseaseIndividualMismatch() {
        diseaseSample.individual = Individual.build(project: project)
        assert diseaseSample.save(flush: true)
        assertFindsNothing()

        assertFindsOne(DomainFactory.createMergingWorkPackage(controlMwp,
                Sample.build(individual: individual, sampleType: diseaseSampleType)), controlMwp)
    }

    @Test
    void testControlIndividualMismatch() {
        controlSample.individual = Individual.build(project: project)
        assert controlSample.save(flush: true)
        assertFindsNothing()

        assertFindsOne(diseaseMwp, DomainFactory.createMergingWorkPackage(
                diseaseMwp, Sample.build(individual: individual, sampleType: controlSampleType)))
    }

    @Test
    void testDiseaseLibPrepKitMismatch_WGS() {
        diseaseMwp.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        diseaseMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseLibPrepKitMismatch_Exome() {
        diseaseMwp.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        diseaseMwp.seqType = exome
        diseaseMwp.save(flush: true)

        controlMwp.seqType = exome
        controlMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseLibPrepKitMismatch_LibPrepKitNull() {
        diseaseMwp.libraryPreparationKit = null
        diseaseMwp.save(flush: true)

        assertFindsOne()
    }

    @Test
    void testDiseaseStppMissing() {
        diseaseStpp.delete(flush: true)
        assertFindsNothing()
    }

    @Test
    void testControlStppMissing() {
        controlStpp.delete(flush: true)
        assertFindsNothing()
    }

    @Test
    void testDiseaseStppProjectMismatch() {
        diseaseStpp.project = Project.build()
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppProjectMismatch() {
        controlStpp.project = Project.build()
        assert controlStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeMismatch() {
        diseaseStpp.sampleType = SampleType.build()
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: diseaseSampleType, category: SampleType.Category.DISEASE)
        assertFindsOne()
    }

    @Test
    void testControlStppSampleTypeMismatch() {
        controlStpp.sampleType = SampleType.build()
        assert controlStpp.save(flush: true)
        assertFindsNothing()

        SampleTypePerProject.build(project: project, sampleType: controlSampleType, category: SampleType.Category.CONTROL)
        assertFindsOne()
    }

    @Test
    void testDiseaseStppSampleTypeCategoryIgnored() {
        diseaseStpp.category = SampleType.Category.IGNORED
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testControlStppSampleTypeCategoryIgnored() {
        controlStpp.category = SampleType.Category.IGNORED
        assert controlStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testBothStppDisease() {
        controlStpp.category = SampleType.Category.DISEASE
        assert controlStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testBothStppControl() {
        diseaseStpp.category = SampleType.Category.CONTROL
        assert diseaseStpp.save(flush: true)
        assertFindsNothing()
    }

    @Test
    void testMatchingSamplePairAlreadyExists() {
        DomainFactory.createSamplePair(
                diseaseMwp,
                controlMwp,
        )
        assertFindsNothing()
    }

    @Test
    void testSamplePairWithOtherIndividualExists() {
        Individual otherIndividual = Individual.build(project: project)
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp,
                        Sample.build(individual: otherIndividual, sampleType: diseaseSampleType)),
                DomainFactory.createMergingWorkPackage(controlMwp,
                        Sample.build(individual: otherIndividual, sampleType: controlSampleType)),
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType1Exists() {
        final SampleType sampleType1 = SampleType.build()
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp, sampleType1),
                controlMwp,
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSampleType2Exists() {
        DomainFactory.createSamplePair(
                diseaseMwp,
                DomainFactory.createMergingWorkPackage(diseaseMwp, SampleType.build()),
        )
        assertFindsOne()
    }

    @Test
    void testSamplePairWithOtherSeqTypeExists() {
        DomainFactory.createSamplePair(
                DomainFactory.createMergingWorkPackage(diseaseMwp, [seqType: exome]),
                DomainFactory.createMergingWorkPackage(controlMwp, [seqType: exome]),
        )
        assertFindsOne()
    }

    @Test
    void testFindTwo() {
        MergingWorkPackage diseaseExomeMwp = DomainFactory.createMergingWorkPackage(diseaseMwp, [seqType: exome])
        MergingWorkPackage controlExomeMwp = DomainFactory.createMergingWorkPackage(controlMwp, [seqType: exome])
        final List<SamplePair> samplePairs =
                SamplePair.findMissingDiseaseControlSamplePairs().sort { it.seqType.name }
        assert samplePairs.size() == 2
        assertEqualsAndNotPersisted(samplePairs[0], diseaseExomeMwp, controlExomeMwp)
        assertEqualsAndNotPersisted(samplePairs[1], diseaseMwp, controlMwp)
    }

    @Test
    void testFindNoExternalMergingWorkPackage() {
        ExternalMergingWorkPackage disease2 = DomainFactory.createExternalMergingWorkPackage([sample: diseaseMwp.sample, seqType: diseaseMwp.seqType])
        ExternalMergingWorkPackage control2 = DomainFactory.createExternalMergingWorkPackage([sample: controlMwp.sample, seqType: controlMwp.seqType])

        diseaseMwp.delete(flush: true)
        controlMwp.delete(flush: true)

        assert AbstractMergingWorkPackage.list().size() == 2

        assertFindsNothing()
    }

    void assertFindsNothing() {
        assert SamplePair.findMissingDiseaseControlSamplePairs().empty
    }

    void assertFindsOne(final MergingWorkPackage mergingWorkPackage1 = diseaseMwp,
                        final MergingWorkPackage mergingWorkPackage2 = controlMwp) {
        assertEqualsAndNotPersisted(
                exactlyOneElement(SamplePair.findMissingDiseaseControlSamplePairs()),
                mergingWorkPackage1, mergingWorkPackage2)
    }

    void assertEqualsAndNotPersisted(final SamplePair samplePair,
                                     final MergingWorkPackage mergingWorkPackage1,
                                     final MergingWorkPackage mergingWorkPackage2) {
        assert samplePair.mergingWorkPackage1 == mergingWorkPackage1 &&
               samplePair.mergingWorkPackage2 == mergingWorkPackage2 &&
               samplePair.id                  == null
    }
}
