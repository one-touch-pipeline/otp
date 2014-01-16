package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster

class MergingPassServiceTests {

    MergingPassService mergingPassService

    @Test(expected = IllegalArgumentException)
    void testMergingPassFinishedAndStartQAMergingPassIsNull() {
        MergingPass mergingPass = mergingPassService.create()
        mergingPassService.mergingPassFinishedAndStartQA(mergingPass)
    }

    @Test
    void testMergingPassFinishedAndStartQA() {
        MergingSet mergingSet = createMergingSet("1")
        MergingPass mergingPass = mergingPassService.create()
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.UNKNOWN,
                        fileOperationStatus: AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
        assertEquals(AbstractBamFile.QaProcessingStatus.UNKNOWN, processedMergedBamFile.qualityAssessmentStatus)
        mergingPassService.mergingPassFinishedAndStartQA(mergingPass)
        assertEquals(MergingSet.State.PROCESSED, mergingSet.status)
        assertEquals(AbstractBamFile.QaProcessingStatus.NOT_STARTED, processedMergedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testMergedBamFileSetQaNotStartedInputNull() {
        mergingPassService.mergedBamFileSetQaNotStarted(null)
    }

    @Test
    void testMergedBamFileSetQaNotStarted() {
        MergingPass mergingPass = createMergingPass("0")
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.UNKNOWN,
                        fileOperationStatus: AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
        assertEquals(AbstractBamFile.QaProcessingStatus.UNKNOWN, processedMergedBamFile.qualityAssessmentStatus)
        mergingPassService.mergedBamFileSetQaNotStarted(mergingPass)
        assertEquals(AbstractBamFile.QaProcessingStatus.NOT_STARTED, processedMergedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testProjectMergingPassIsNull() {
        MergingPass mergingPass = mergingPassService.create()
        assertNull(mergingPassService.project(mergingPass))
    }

    @Test
    void testProject() {
        MergingSet mergingSet = createMergingSet("1")
        MergingPass mergingPass = mergingPassService.create()
        Project projectExp = mergingPass.mergingSet.mergingWorkPackage.sample.individual.project
        Project projectAct = mergingPassService.project(mergingPass)
        assertEquals(projectExp, projectAct)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmForDataProcessingMergingPassIsNull() {
        MergingPass mergingPass = mergingPassService.create()
        assertNull(mergingPassService.realmForDataProcessing(mergingPass))
    }

    @Test
    void testRealmForDataProcessing() {
        MergingSet mergingSet = createMergingSet("1")
        MergingPass mergingPass = mergingPassService.create()
        Realm realm = DomainFactory.createRealmDataProcessingDKFZ().save([flush: true])
        Realm realmAct = mergingPassService.realmForDataProcessing(mergingPass)
        assertEquals(realm, realmAct)
    }

    @Test
    void testCreateMergingPass() {
        MergingPass mergingPass = mergingPassService.create()
        assertNull(mergingPass)

        MergingSet mergingSet = createMergingSet("1")
        assertNotNull(mergingSet)
        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(0, mergingPass.identifier)

        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(1, mergingPass.identifier)

        mergingSet.status = MergingSet.State.PROCESSED
        mergingSet.save([flush: true, failOnError: true])
        mergingPass = mergingPassService.create()
        assertNull(mergingPass)

        mergingSet = createMergingSet("2")
        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(0, mergingPass.identifier)

        mergingPass = mergingPassService.create()
        assertNotNull(mergingPass)
        assertEquals(mergingSet, mergingPass.mergingSet)
        assertEquals(1, mergingPass.identifier)
    }

    @Test
    void testMergingPassStarted() {
        MergingPass mergingPass = createMergingPass("1")
        mergingPassService.mergingPassStarted(mergingPass)
        assertEquals(mergingPass.mergingSet.status, MergingSet.State.INPROGRESS)
    }

    @Test
    void testUpdateMergingSet() {
        MergingPass mergingPass = createMergingPass("1")
        assertEquals(MergingSet.State.NEEDS_PROCESSING, mergingPass.mergingSet.status)
        mergingPassService.updateMergingSet(mergingPass, MergingSet.State.DECLARED)
        assertEquals(MergingSet.State.DECLARED, mergingPass.mergingSet.status)
        mergingPassService.updateMergingSet(mergingPass, MergingSet.State.INPROGRESS)
        assertEquals(MergingSet.State.INPROGRESS, mergingPass.mergingSet.status)
        mergingPassService.updateMergingSet(mergingPass, MergingSet.State.NEEDS_PROCESSING)
        assertEquals(MergingSet.State.NEEDS_PROCESSING, mergingPass.mergingSet.status)
        mergingPassService.updateMergingSet(mergingPass, MergingSet.State.PROCESSED)
        assertEquals(MergingSet.State.PROCESSED, mergingPass.mergingSet.status)
    }

    private MergingSet createMergingSet(String uniqueId) {
        Project project = new Project(
                        name: "name_" + uniqueId,
                        dirName: "dirName",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "pid_" + uniqueId,
                        mockPid: "mockPid_" + uniqueId,
                        mockFullName: "mockFullName_" + uniqueId,
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "name_" + uniqueId
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
                        name:"seqType_" + uniqueId,
                        libraryLayout:"library",
                        dirName: "dir"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        return mergingSet
    }

    private MergingPass createMergingPass(String uniqueId) {
        MergingSet mergingSet = createMergingSet(uniqueId)
        MergingPass mergingPass = new MergingPass(identifier: 1, mergingSet: mergingSet)
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))
        return mergingPass
    }
}
