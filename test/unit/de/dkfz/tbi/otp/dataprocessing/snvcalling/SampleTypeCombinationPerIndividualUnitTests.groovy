package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(SampleTypeCombinationPerIndividual)
@Mock([Realm, Project, Individual, SampleType, SeqType])
class SampleTypeCombinationPerIndividualUnitTests {

    void testSaveSnvCombinationPerIndividualOnlyIndividual() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.individual = new Individual()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividualOnlySampleType1() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.sampleType1 = new SampleType()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividualOnlySampleType2() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.sampleType2 = new SampleType()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividualOnlySeqType() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.seqType = new SeqType()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividual() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.individual = new Individual()
        sampleCombinationPerIndividual.seqType = new SeqType()
        sampleCombinationPerIndividual.sampleType1 = new SampleType()
        sampleCombinationPerIndividual.sampleType2 = new SampleType()
        assertTrue(sampleCombinationPerIndividual.validate())
        //println sampleCombinationPerIndividual.errors
    }

    void testGetSampleTypeCombinationPath() {
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

        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
            individual: individual,
            seqType: seqType,
            sampleType1: sampleType1,
            sampleType2: sampleType2
            )
        sampleCombinationPerIndividual.save(flush: true)
        String path = "dirName/sequencing/whole_genome_sequencing/view-by-pid/654321/snv_results/paired/tumor_control"
        File expectedExtension = new File(path)

        OtpPath sampleTypeCombinationPath = sampleCombinationPerIndividual.getSampleTypeCombinationPath()
        assertEquals(expectedExtension, sampleTypeCombinationPath.relativePath)
        assertEquals(project, sampleTypeCombinationPath.project)
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

        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
            individual: individual,
            seqType: seqType,
            sampleType1: sampleType1,
            sampleType2: sampleType2
            )
        sampleCombinationPerIndividual.save(flush: true)

        String path = "dirName/sequencing/whole_genome_sequencing/view-by-pid/654321/snv_results/paired/tumor_control"
        String fileCalling = "snvs_654321_raw.vcf.gz"
        File expectedExtension = new File("${path}/${fileCalling}")

        OtpPath snvCallingResultFileLinkedPath = sampleCombinationPerIndividual.getResultFileLinkedPath(SnvCallingStep.CALLING)
        assertEquals(expectedExtension, snvCallingResultFileLinkedPath.relativePath)
        assertEquals(project, snvCallingResultFileLinkedPath.project)

        String fileDeepAnnotation = "snvs_654321.vcf.gz"
        File expectedExtensionDeepAnnotation = new File("${path}/${fileDeepAnnotation}")

        OtpPath snvDeepAnnotationResultFileLinkedPath = sampleCombinationPerIndividual.getResultFileLinkedPath(SnvCallingStep.SNV_DEEPANNOTATION)
        assertEquals(expectedExtensionDeepAnnotation, snvDeepAnnotationResultFileLinkedPath.relativePath)
        assertEquals(project, snvDeepAnnotationResultFileLinkedPath.project)
    }
}
