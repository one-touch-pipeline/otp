package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(SamplePair)
@Build([
        MergingWorkPackage,
        Realm,
        SampleTypePerProject,
])
class SamplePairUnitTests {

    List setUpForPathTests() {
        TestData testData = new TestData()
        Realm realm = DomainFactory.createRealmDataManagement()
        Project project = DomainFactory.createProject([realmName: realm.name])
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
        MergingWorkPackage mergingWorkPackage1 = MergingWorkPackage.build(
                sample: Sample.build(
                        individual: individual,
                        sampleType: sampleType1,
                ),
                seqType: seqType,
        )
        SamplePair samplePair = DomainFactory.createSamplePair(mergingWorkPackage1,
                DomainFactory.createMergingWorkPackage(mergingWorkPackage1, sampleType2))

        String path = "${project.dirName}/sequencing/whole_genome_sequencing/view-by-pid/654321/snv_results/paired/tumor_control"
        return [path, samplePair, project]
    }

    @Test
    void testGetSamplePairPath() {
        def (String path, SamplePair samplePair, Project project) = setUpForPathTests()
        File expectedExtension = new File(path)

        OtpPath samplePairPath = samplePair.getSamplePairPath()
        assertEquals(expectedExtension, samplePairPath.relativePath)
        assertEquals(project, samplePairPath.project)
    }
}
