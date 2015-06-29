package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*

class MergingPassServiceTests {

    private static final int LENGTH_NO_BAMFILE = 0

    private static final int LENGTH_ONE_BAMFILE = 10

    MergingPassService mergingPassService

    TestData testData = new TestData()


    @Test(expected = IllegalArgumentException)
    void testMergingPassFinishedAndStartQAMergingPassIsNull() {
        MergingPass mergingPass = mergingPassService.create()
        mergingPassService.mergingPassFinishedAndStartQA(mergingPass)
    }

    @Test
    void testMergingPassFinishedAndStartQA() {
        MergingSet mergingSet = createMergingSet("1")
        MergingPass mergingPass = mergingPassService.create()
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.UNKNOWN,
                        fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED,
                        numberOfMergedLanes: 1,
                        ])
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
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.UNKNOWN,
                        fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED,
                        numberOfMergedLanes: 1,
                        ])
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
        Project projectExp = mergingPass.project
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
        Project project = TestData.createProject(
                        name: "project_" + uniqueId,
                        dirName: "project_" + uniqueId,
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
                        dirName: "dir_" + uniqueId
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage(
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



    private createDataForMayProcessingFilesBeDeleted() {
        MergingSet mergingSet = MergingSet.build(status: MergingSet.State.PROCESSED)
        MergingPass mergingPass1 = MergingPass.build([mergingSet: mergingSet])
        ProcessedMergedBamFile processedMergedBamFile1 = ProcessedMergedBamFile.build(mergingPass: mergingPass1, workPackage: mergingPass1.mergingWorkPackage)
        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = QualityAssessmentMergedPass.build(processedMergedBamFile: processedMergedBamFile1)
        MergingPass mergingPass2 = MergingPass.build([
            mergingSet: mergingSet,
            identifier: 1,
            ])
        ProcessedMergedBamFile processedMergedBamFile2 = ProcessedMergedBamFile.build([
            mergingPass: mergingPass2,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            withdrawn: false,
            workPackage: mergingPass2.mergingWorkPackage,
        ])
        Date createdBefore = new Date().plus(1) //some date later the the bam file

        mergingPassService = new MergingPassService()
        mergingPassService.abstractBamFileService = [
            hasBeenQualityAssessedAndMerged: {final AbstractBamFile bamFile, final Date before ->
                return false
            }
        ] as AbstractBamFileService
        [mergingPassService, mergingPass1, createdBefore, processedMergedBamFile2]
    }

    /*
     * Most test for testMayProcessingFilesBeDeleted are written as unit test.
     * But on one path the method contains a critera, which aren't handled by the unit test correctly.
     */

    public void testMayProcessingFilesBeDeleted_MergingSetIsProcessed() {
        MergingPassService mergingPassService
        MergingPass mergingPass
        Date createdBefore
        ProcessedMergedBamFile processedMergedBamFileNew
        (mergingPassService, mergingPass, createdBefore, processedMergedBamFileNew) = createDataForMayProcessingFilesBeDeleted()

        assert mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    public void testMayProcessingFilesBeDeleted_MergingSetIsProcessed_WrongQualityAssesment() {
        MergingPassService mergingPassService
        MergingPass mergingPass
        Date createdBefore
        ProcessedMergedBamFile processedMergedBamFileNew
        (mergingPassService, mergingPass, createdBefore, processedMergedBamFileNew) = createDataForMayProcessingFilesBeDeleted()
        processedMergedBamFileNew.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.IN_PROGRESS

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    public void testMayProcessingFilesBeDeleted_MergingSetIsProcessed_DateNotGreater() {
        MergingPassService mergingPassService
        MergingPass mergingPass
        Date createdBefore
        ProcessedMergedBamFile processedMergedBamFileNew
        (mergingPassService, mergingPass, createdBefore, processedMergedBamFileNew) = createDataForMayProcessingFilesBeDeleted()
        createdBefore = processedMergedBamFileNew.dateCreated

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    public void testMayProcessingFilesBeDeleted_MergingSetIsProcessed_WithDrawnIsTrue() {
        MergingPassService mergingPassService
        MergingPass mergingPass
        Date createdBefore
        ProcessedMergedBamFile processedMergedBamFileNew
        (mergingPassService, mergingPass, createdBefore, processedMergedBamFileNew) = createDataForMayProcessingFilesBeDeleted()
        processedMergedBamFileNew.withdrawn = true

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    public void testMayProcessingFilesBeDeleted_MergingSetIsProcessed_IdentifierIsSmaller() {
        MergingPassService mergingPassService
        MergingPass mergingPass
        Date createdBefore
        ProcessedMergedBamFile processedMergedBamFileNew
        (mergingPassService, mergingPass, createdBefore, processedMergedBamFileNew) = createDataForMayProcessingFilesBeDeleted()
        processedMergedBamFileNew.mergingPass.identifier = -1

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    public void testMayProcessingFilesBeDeleted_MergingSetIsProcessed_MergingSetIsDifferent() {
        MergingPassService mergingPassService
        MergingPass mergingPass
        Date createdBefore
        ProcessedMergedBamFile processedMergedBamFileNew
        (mergingPassService, mergingPass, createdBefore, processedMergedBamFileNew) = createDataForMayProcessingFilesBeDeleted()
        MergingWorkPackage mergingWorkPackage = processedMergedBamFileNew.mergingSet.mergingWorkPackage
        processedMergedBamFileNew.mergingPass.mergingSet = MergingSet.build(mergingWorkPackage: mergingWorkPackage, identifier: MergingSet.nextIdentifier(mergingWorkPackage))

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }



    private void createMergingPassService(ProcessedMergedBamFile processedMergedBamFile = null) {
        mergingPassService = new MergingPassService()
        mergingPassService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                def data = passesFunc()
                if (!processedMergedBamFile) {
                    assert [] == data
                    return LENGTH_NO_BAMFILE
                } else {
                    def expected = [
                        processedMergedBamFile.mergingPass
                    ]
                    assert expected == data
                    return LENGTH_ONE_BAMFILE
                }
            }
        ] as DataProcessingFilesService
    }

    private ProcessedMergedBamFile createProcessedMergedBamFileAlreadyTransfered(Map map = [:]) {
        return ProcessedMergedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
            md5sum: "12345678901234567890123456789012",
            fileSize: 10000,
        ] + map)
    }

    private ProcessedMergedBamFile createProcessedMergedBamFileWithLaterProcessedPass(Map mapLaterBamFile = [:], Map mapPassOfLaterBamFile = [:], Map mapMergingSet = [:]) {
        MergingSet mergingSet = MergingSet.build([
            status: MergingSet.State.PROCESSED
        ] + mapMergingSet)

        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: MergingPass.build([
                mergingSet: mergingSet,
                identifier: 2
            ]),
            workPackage: mergingSet.mergingWorkPackage,
        ])

        ProcessedMergedBamFile processedMergedBamFileLaterPass = ProcessedMergedBamFile.build([
            mergingPass: MergingPass.build([
                mergingSet: mergingSet,
                identifier: 3
            ] + mapPassOfLaterBamFile),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            workPackage: mergingSet.mergingWorkPackage,
        ] + mapLaterBamFile)

        return processedMergedBamFile
    }

    private ProcessedMergedBamFile createProcessedMergedBamFileWithFurtherMerged(Map bamFileMap = [:], Map passMap = [:], Map mergingSetMap = [:], Map furtherMergedBamFileMap = [:], Map furtherMergedPassMap = [:], Map furtherMergedSetMap = [:], Map mergingSetAssignmentMap = [:] ) {
        MergingSet mergingSet = MergingSet.build([
            status: MergingSet.State.PROCESSED
        ] + mergingSetMap)

        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: MergingPass.build([
                mergingSet: mergingSet,
            ] + passMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            workPackage: mergingSet.mergingWorkPackage,
        ] + bamFileMap)

        MergingSet furtherMergedSet = MergingSet.build([
            mergingWorkPackage: processedMergedBamFile.mergingWorkPackage,
            identifier: MergingSet.nextIdentifier(processedMergedBamFile.mergingWorkPackage),
            status: MergingSet.State.PROCESSED
        ] + furtherMergedSetMap)

        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build([
            mergingSet: furtherMergedSet,
            bamFile: processedMergedBamFile,
        ] + mergingSetAssignmentMap)

        ProcessedMergedBamFile furtherMergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: MergingPass.build([
                mergingSet: furtherMergedSet,
            ] + furtherMergedPassMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            workPackage: furtherMergedSet.mergingWorkPackage,
        ] + furtherMergedBamFileMap)

        return processedMergedBamFile
    }



    public void testDeleteOldMergingProcessingFiles_NoProcessedMergedBamFile() {
        Date createdBeforeDate = new Date()
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered()
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_5Files() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = (1..5).collect { createProcessedMergedBamFileAlreadyTransfered() //
        }

        mergingPassService = new MergingPassService()
        mergingPassService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                def expected = processedMergedBamFiles*.mergingPass
                def data = passesFunc()
                assert expected as Set == data as Set
                return expected.size() * LENGTH_ONE_BAMFILE
            }
        ] as DataProcessingFilesService

        assert 5 * LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_WrongQaStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered(qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS)
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_WrongFileOperationStateAndNoMd5sum() {
        //FileOperationState and md5sum are can't check seperatly, because they are related together
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered(fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, md5sum: null)
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }



    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass()
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_PassIdentifierIsSmaller() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([:], [identifier: 1])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_OtherMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = MergingSet.build()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([workPackage: mergingSet.mergingWorkPackage], [mergingSet:mergingSet])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_LaterBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([withdrawn: true])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_BothBamFilesAreWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([withdrawn: true])
        processedMergedBamFile.withdrawn = true
        processedMergedBamFile.save(flush: true)
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_CreateDateIsLater() {
        Date createdBeforeDate = new Date().plus(-1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass()
        createMergingPassService()

        //since the property createDate is always set by grails, I use HQL to change the value
        processedMergedBamFile.executeUpdate("update ProcessedMergedBamFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: new Date().plus(-2),
            id: processedMergedBamFile.id
        ])

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_QualityAssessmentStatusIsNotFinished() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_MergingSetStatusIsNotProcessed() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([:], [:], [status: MergingSet.State.INPROGRESS])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }



    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged()
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_CheckBamFileWrongQAStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_CheckBamFileWrongStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [status: AbstractBamFile.State.INPROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_CheckBamFileWrongMergingSetStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [:], [:], [status: MergingSet.State.INPROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_WrongFurtherBamFile() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedMergedBamFile))
        msa.bamFile = DomainFactory.createProcessedMergedBamFile(msa.mergingSet.mergingWorkPackage)
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileQAStatusWrong() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileTooYoung() {
        Date createdBeforeDate = new Date().plus(-1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged()
        createMergingPassService()

        //since the property createDate is always set by grails, I use HQL to change the value
        processedMergedBamFile.executeUpdate("update ProcessedMergedBamFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: new Date().plus(-2),
            id: processedMergedBamFile.id
        ])

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [withdrawn: true]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_BothBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [withdrawn: true], [:], [:],
                        [withdrawn: true]
                        )

        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileIsHasOtherMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = MergingSet.build()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [workPackage: mergingSet.mergingWorkPackage], [mergingSet: mergingSet]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }



    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesNotExistAndDeletionDateIsNotSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: false,
            deletionDate: null,
            ])
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesNotExistAndDeletionDateIsSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: false,
            deletionDate: new Date(),
            ])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesExistAndDeletionDateIsNotSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: true,
            deletionDate: null,
            ])
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesExistAndDeletionDateIsSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: true,
            deletionDate: new Date(),
            ])
        createMergingPassService(processedMergedBamFile)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    public void testDeleteOldMergingProcessingFiles_GeneralCondition_CreateDateToLate() {
        Date createdBeforeDate = new Date().plus(-1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered()
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

}
