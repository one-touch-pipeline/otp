package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(SamplePair)
@Build([Realm, Individual, SampleTypePerProject, SeqType])
class SamplePairUnitTests {

    void testSaveSamplePairOnlyIndividual() {
        SamplePair samplePair = new SamplePair()
        samplePair.individual = new Individual()
        assertFalse(samplePair.validate())
    }

    void testSaveSamplePairOnlySampleType1() {
        SamplePair samplePair = new SamplePair()
        samplePair.sampleType1 = new SampleType()
        assertFalse(samplePair.validate())
    }

    void testSaveSamplePairOnlySampleType2() {
        SamplePair samplePair = new SamplePair()
        samplePair.sampleType2 = new SampleType()
        assertFalse(samplePair.validate())
    }

    void testSaveSamplePairOnlySeqType() {
        SamplePair samplePair = new SamplePair()
        samplePair.seqType = new SeqType()
        assertFalse(samplePair.validate())
    }

    void testSaveSamplePair() {
        final Individual individual = Individual.build()
        final SampleType sampleType1 = SampleType.build()
        SampleTypePerProject.build(project: individual.project, sampleType: sampleType1, category: SampleType.Category.DISEASE)
        SamplePair samplePair = new SamplePair()
        samplePair.individual = individual
        samplePair.seqType = new SeqType()
        samplePair.sampleType1 = sampleType1
        samplePair.sampleType2 = new SampleType()
        assertTrue(samplePair.validate())
    }

    void testGetSamplePairPath() {
        TestData testData = new TestData()
        Realm realm = DomainFactory.createRealmDataManagementDKFZ()
        Project project = testData.createProject([realmName: realm.name])
        project.save(flush: true)
        Individual individual = testData.createIndividual([project: project])
        individual.save(flush: true)
        SeqType seqType = testData.createSeqType()
        seqType.save(flush: true)
        SampleType sampleType1 = testData.createSampleType([name: "TUMOR"])
        sampleType1.save(flush: true)
        SampleType sampleType2 = testData.createSampleType([name: "CONTROL"])
        sampleType2.save(flush: true)
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)

        SamplePair samplePair = new SamplePair(
            individual: individual,
            seqType: seqType,
            sampleType1: sampleType1,
            sampleType2: sampleType2
            )
        samplePair.save(flush: true)
        String path = "dirName/sequencing/whole_genome_sequencing/view-by-pid/654321/snv_results/paired/tumor_control"
        File expectedExtension = new File(path)

        OtpPath samplePairPath = samplePair.getSamplePairPath()
        assertEquals(expectedExtension, samplePairPath.relativePath)
        assertEquals(project, samplePairPath.project)
    }

    void testGetResultFileLinkedPath() {
        TestData testData = new TestData()
        Realm realm = DomainFactory.createRealmDataManagementDKFZ()
        Project project = testData.createProject([realmName: realm.name])
        project.save(flush: true)
        Individual individual = testData.createIndividual([project: project])
        individual.save(flush: true)
        SeqType seqType = testData.createSeqType()
        seqType.save(flush: true)
        SampleType sampleType1 = testData.createSampleType([name: "TUMOR"])
        sampleType1.save(flush: true)
        SampleType sampleType2 = testData.createSampleType([name: "CONTROL"])
        sampleType2.save(flush: true)
        SampleTypePerProject.build(project: project, sampleType: sampleType1, category: SampleType.Category.DISEASE)

        SamplePair samplePair = new SamplePair(
            individual: individual,
            seqType: seqType,
            sampleType1: sampleType1,
            sampleType2: sampleType2
            )
        samplePair.save(flush: true)

        String path = "dirName/sequencing/whole_genome_sequencing/view-by-pid/654321/snv_results/paired/tumor_control"
        String fileCalling = "snvs_654321_raw.vcf.gz"
        File expectedExtension = new File("${path}/${fileCalling}")

        OtpPath snvCallingResultFileLinkedPath = samplePair.getResultFileLinkedPath(SnvCallingStep.CALLING)
        assertEquals(expectedExtension, snvCallingResultFileLinkedPath.relativePath)
        assertEquals(project, snvCallingResultFileLinkedPath.project)

        String fileDeepAnnotation = "snvs_654321.vcf.gz"
        File expectedExtensionDeepAnnotation = new File("${path}/${fileDeepAnnotation}")

        OtpPath snvDeepAnnotationResultFileLinkedPath = samplePair.getResultFileLinkedPath(SnvCallingStep.SNV_DEEPANNOTATION)
        assertEquals(expectedExtensionDeepAnnotation, snvDeepAnnotationResultFileLinkedPath.relativePath)
        assertEquals(project, snvDeepAnnotationResultFileLinkedPath.project)

        OtpPath filterResultFilesLinkedPath = samplePair.getResultFileLinkedPath(SnvCallingStep.FILTER_VCF)
        assertEquals(new File(path), filterResultFilesLinkedPath.relativePath)
        assertEquals(project, filterResultFilesLinkedPath.project)
    }
}
