package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import org.junit.*

@Mock([
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqPlatformGroup,
        SeqType,
])
class SamplePairUnitTests {

    List setUpForPathTests() {
        Realm realm = DomainFactory.createRealmDataManagement()
        Project project = DomainFactory.createProject(
                realmName: realm.name,
        )
        Individual individual = DomainFactory.createIndividual(
                project: project,
        )
        SeqType seqType = DomainFactory.createSeqType(
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED
        )
        SampleType sampleType1 = DomainFactory.createSampleType(
                name: "TUMOR",
        )
        SampleType sampleType2 = DomainFactory.createSampleType(
                name: "CONTROL",
        )
        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleType1,
                category: SampleType.Category.DISEASE,
        )
        MergingWorkPackage mergingWorkPackage1 = DomainFactory.createMergingWorkPackage(
                seqType: seqType,
                sample: DomainFactory.createSample(
                        individual: individual,
                        sampleType: sampleType1,
                ),
        )
        SamplePair samplePair = DomainFactory.createSamplePair(mergingWorkPackage1,
                DomainFactory.createMergingWorkPackage(mergingWorkPackage1, sampleType2))

        String snvPath = "${project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}/snv_results/${seqType.libraryLayoutDirName}/tumor_control"
        String indelPath = "${project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}/indel_results/${seqType.libraryLayoutDirName}/tumor_control"
        return [snvPath, indelPath, samplePair, project]
    }

    @Test
    void testGetSnvSamplePairPath() {
        def (String snvPath, String indelPath, SamplePair samplePair, Project project) = setUpForPathTests()
        File expectedExtension = new File(snvPath)

        OtpPath samplePairPath = samplePair.getSnvSamplePairPath()
        assertEquals(expectedExtension, samplePairPath.relativePath)
        assertEquals(project, samplePairPath.project)
    }

    @Test
    void testGetIndelSamplePairPath() {
        def (String snvPath, String indelPath, SamplePair samplePair, Project project) = setUpForPathTests()
        File expectedExtension = new File(indelPath)

        OtpPath samplePairPath = samplePair.getIndelSamplePairPath()
        assertEquals(expectedExtension, samplePairPath.relativePath)
        assertEquals(project, samplePairPath.project)
    }


    @Test
    void testSetIndelProcessingStatus() {
        SamplePair samplePair = setUpForPathTests()[2]
        SamplePair.setIndelProcessingStatus([samplePair], SamplePair.ProcessingStatus.DISABLED)

        assert samplePair.indelProcessingStatus == SamplePair.ProcessingStatus.DISABLED
    }

    @Test
    void testSetSnvProcessingStatus() {
        SamplePair samplePair = setUpForPathTests()[2]
        SamplePair.setSnvProcessingStatus([samplePair], SamplePair.ProcessingStatus.DISABLED)

        assert samplePair.snvProcessingStatus == SamplePair.ProcessingStatus.DISABLED
    }
}
