package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class MergingPassServiceTests {

    private static final int LENGTH_NO_BAMFILE = 0

    private static final int LENGTH_ONE_BAMFILE = 10

    MergingPassService mergingPassService

    TestData testData = new TestData()

    private void createMergingPassService(List<ProcessedMergedBamFile> processedMergedBamFile = null) {
        mergingPassService = new MergingPassService()
        mergingPassService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                def data = passesFunc()
                if (!processedMergedBamFile) {
                    assert [] == data
                    return LENGTH_NO_BAMFILE
                } else {
                    def expected = processedMergedBamFile*.mergingPass
                    assert CollectionUtils.containSame(expected, data)
                    return LENGTH_ONE_BAMFILE
                }
            }
        ] as DataProcessingFilesService
    }

    private ProcessedMergedBamFile createProcessedMergedBamFileAlreadyTransfered(Map map = [:]) {
        return DomainFactory.createProcessedMergedBamFile([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
            md5sum: "12345678901234567890123456789012",
            fileSize: 10000,
        ] + map)
    }

    private static List<ProcessedMergedBamFile> createProcessedMergedBamFileWithLaterProcessedPass(Map mapLaterBamFile = [:], Map mapPassOfLaterBamFile = [:], Map mapMergingSet = [:]) {
        MergingSet mergingSet = DomainFactory.createMergingSet([
            status: MergingSet.State.PROCESSED
        ] + mapMergingSet)

        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: DomainFactory.createMergingPass([
                mergingSet: mergingSet,
                identifier: 2
            ]),
            workPackage: mergingSet.mergingWorkPackage,
        ])

        ProcessedMergedBamFile processedMergedBamFileLaterPass = ProcessedMergedBamFile.build([
            mergingPass: DomainFactory.createMergingPass([
                mergingSet: mergingSet,
                identifier: 3
            ] + mapPassOfLaterBamFile),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            workPackage: mergingSet.mergingWorkPackage,
        ] + mapLaterBamFile)

        return [processedMergedBamFile, processedMergedBamFileLaterPass]
    }

    private static List<ProcessedMergedBamFile> createProcessedMergedBamFileWithFurtherMerged(Map bamFileMap = [:], Map passMap = [:], Map mergingSetMap = [:], Map furtherMergedBamFileMap = [:], Map furtherMergedPassMap = [:], Map furtherMergedSetMap = [:], Map mergingSetAssignmentMap = [:] ) {
        MergingSet mergingSet = DomainFactory.createMergingSet([
            status: MergingSet.State.PROCESSED
        ] + mergingSetMap)

        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: DomainFactory.createMergingPass([
                mergingSet: mergingSet,
            ] + passMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            workPackage: mergingSet.mergingWorkPackage,
        ] + bamFileMap)

        MergingSet furtherMergedSet = DomainFactory.createMergingSet([
            mergingWorkPackage: processedMergedBamFile.mergingWorkPackage,
            identifier: MergingSet.nextIdentifier(processedMergedBamFile.mergingWorkPackage),
            status: MergingSet.State.PROCESSED
        ] + furtherMergedSetMap)

        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build([
            mergingSet: furtherMergedSet,
            bamFile: processedMergedBamFile,
        ] + mergingSetAssignmentMap)

        ProcessedMergedBamFile furtherMergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: DomainFactory.createMergingPass([
                mergingSet: furtherMergedSet,
            ] + furtherMergedPassMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            workPackage: furtherMergedSet.mergingWorkPackage,
        ] + furtherMergedBamFileMap)

        return [processedMergedBamFile, furtherMergedBamFile]
    }



    @Test
    public void testDeleteOldMergingProcessingFiles_NoProcessedMergedBamFile() {
        Date createdBeforeDate = new Date()
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered()
        createMergingPassService([processedMergedBamFile])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
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

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_WrongQaStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered(qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS)
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionAlreadyTransferred_WrongFileOperationStateAndNoMd5sum() {
        //FileOperationState and md5sum are can't check seperatly, because they are related together
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered(fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, md5sum: null)
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }



    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass()
        createMergingPassService([processedMergedBamFiles[0]])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_PassIdentifierIsSmaller() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass([:], [identifier: 1])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_OtherMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = DomainFactory.createMergingSet()
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass(
                [workPackage: mergingSet.mergingWorkPackage], [mergingSet: mergingSet]
        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_LaterBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFile = createProcessedMergedBamFileWithLaterProcessedPass([withdrawn: true])
        createMergingPassService([processedMergedBamFile[1]])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_BothBamFilesAreWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass([withdrawn: true])
        ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFiles[0]
        processedMergedBamFile.withdrawn = true
        processedMergedBamFile.save(flush: true)
        createMergingPassService(processedMergedBamFiles)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_CreateDateIsLater() {
        Date createdBeforeDate = new Date().plus(-1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass()
        createMergingPassService()

        //since the property createDate is always set by grails, I use HQL to change the value
        processedMergedBamFiles[0].executeUpdate("update ProcessedMergedBamFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: new Date().plus(-2),
            id: processedMergedBamFiles[0].id
        ])

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_QualityAssessmentStatusIsNotFinished() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass([qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionLaterPass_MergingSetStatusIsNotProcessed() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithLaterProcessedPass([:], [:], [status: MergingSet.State.INPROGRESS])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }



    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged()
        createMergingPassService([processedMergedBamFiles[0]])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_CheckBamFileWrongQAStatus() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_CheckBamFileWrongStatus() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [status: AbstractBamFile.State.INPROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_CheckBamFileWrongMergingSetStatus() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [:], [:], [status: MergingSet.State.INPROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_WrongFurtherBamFile() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged()
        processedMergedBamFiles[0].save(flush: true)
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedMergedBamFiles[0]))
        msa.bamFile = DomainFactory.createProcessedMergedBamFile(msa.mergingSet.mergingWorkPackage).save(flush: true)
        msa.save(flush: true)
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileQAStatusWrong() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileTooYoung() {
        Date createdBeforeDate = new Date().plus(-1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged()
        createMergingPassService()

        //since the property createDate is always set by grails, I use HQL to change the value
        processedMergedBamFiles[0].executeUpdate("update ProcessedMergedBamFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: new Date().plus(-2),
            id: processedMergedBamFiles[0].id
        ])

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [withdrawn: true]
                        )
        createMergingPassService([processedMergedBamFiles[1]])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_BothBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [withdrawn: true], [:], [:],
                        [withdrawn: true]
                        )

        createMergingPassService(processedMergedBamFiles)

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_ConditionFurtherMerged_FurtherBamFileIsHasOtherMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = DomainFactory.createMergingSet()
        List<ProcessedMergedBamFile> processedMergedBamFiles = createProcessedMergedBamFileWithFurtherMerged(
                        [:], [:], [:],
                        [workPackage: mergingSet.mergingWorkPackage], [mergingSet: mergingSet]
                        )
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }



    @Test
    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesNotExistAndDeletionDateIsNotSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: false,
            deletionDate: null,
            ])
        createMergingPassService([processedMergedBamFile])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesNotExistAndDeletionDateIsSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: false,
            deletionDate: new Date(),
            ])
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesExistAndDeletionDateIsNotSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: true,
            deletionDate: null,
            ])
        createMergingPassService([processedMergedBamFile])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_GeneralCondition_FileDoesExistAndDeletionDateIsSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered([
            fileExists: true,
            deletionDate: new Date(),
            ])
        createMergingPassService([processedMergedBamFile])

        assert LENGTH_ONE_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

    @Test
    public void testDeleteOldMergingProcessingFiles_GeneralCondition_CreateDateToLate() {
        Date createdBeforeDate = new Date().plus(-1)
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileAlreadyTransfered()
        createMergingPassService()

        assert LENGTH_NO_BAMFILE == mergingPassService.deleteOldMergingProcessingFiles(createdBeforeDate)
    }

}
