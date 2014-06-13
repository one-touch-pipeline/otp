package de.dkfz.tbi.otp.dataprocessing

import org.junit.Test

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils



class ProcessedAlignmentFileServiceTests {

    private static final int LENGTH_NO_FILE = 0

    private static final int LENGTH_ONE_FILE = 10

    ProcessedAlignmentFileService processedAlignmentFileService



    private void changeDateForBamFile(ProcessedBamFile processedBamFile, Date date) {
        //The dateCreated is changed via hibernate update, since grails handle this property itself and do not allow to set the value.
        processedBamFile.executeUpdate("update ProcessedBamFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: date,
            id: processedBamFile.id
        ])
        processedBamFile.refresh()
    }

    private ProcessedAlignmentFileService createServiceForMayProcessingFilesBeDeleted() {
        ProcessedAlignmentFileService processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.abstractBamFileService = [
            hasBeenQualityAssessedAndMerged: {final AbstractBamFile bamFile, final Date before ->
                return false
            }
        ] as AbstractBamFileService
        return processedAlignmentFileService
    }

    private AlignmentPass createTestDataForMayProcessingFilesBeDeleted(Map secondProcessedMergedBamFileMap = [:], Map secondAlignmentPassMap = [:]) {
        SeqTrack seqTrack = SeqTrack.build()

        AlignmentPass alignmentPass = TestData.createAndSaveAlignmentPass([
            seqTrack: seqTrack,
            identifier: 1
        ])

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: alignmentPass,
        ])

        AlignmentPass alignmentPass2 = TestData.createAndSaveAlignmentPass([
            alignmentState: AlignmentState.FINISHED,
            seqTrack: seqTrack,
            identifier: 2,
        ] + secondAlignmentPassMap)

        ProcessedBamFile processedBamFile2 = ProcessedBamFile.build([
            alignmentPass: alignmentPass2,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            withdrawn: false
        ] + secondProcessedMergedBamFileMap)

        return alignmentPass
    }



    //many test for mayProcessingFilesBeDeleted are written as unit test

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_OK() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()
        Date createdBefore = new Date().plus(1)

        assert processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_WrongAlignmentstateOfSeqtrack() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()
        alignmentPass.alignmentState = AlignmentState.NOT_STARTED
        alignmentPass.seqTrack.save(flush: true)
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_WrongQualityAssessmentStatus() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        ])
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_DateCreatedLater() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()
        Date createdBefore = new Date().plus(1)
        ProcessedBamFile processedBamFile = CollectionUtils.exactlyOneElement(ProcessedBamFile.findAllByAlignmentPassNotEqual(alignmentPass))
        changeDateForBamFile(processedBamFile, new Date().plus(2))

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_Withdrawn() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted([
            withdrawn: true
        ])
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_OtherSeqTrack() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted([:], [
            seqTrack: SeqTrack.build(),
        ])
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_SmallerAlignmentPassIdentifier() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted([:], [
            identifier: 0,
        ])
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }



    private void createProcessedAlignmentFileService(ProcessedBamFile processedBamFile = null) {
        processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                assert processedAlignmentFileService == passService
                assert 'alignment' == passTypeName
                def data = passesFunc()
                if (!processedBamFile) {
                    assert []== data
                    return LENGTH_NO_FILE
                } else {
                    def expected = [
                        processedBamFile.alignmentPass
                    ]
                    assert expected == data
                    return LENGTH_ONE_FILE
                }
            }
        ] as DataProcessingFilesService
    }

    /**
     * keys of map:
     * <ul>
     * <li>seqTrackMap</li>
     * <li>bamFileMap</li>
     * <li>passOfLaterBamFileMap</li>
     * <li>laterBamFileMap</li>
     * </ul>
     * The values are a map to pass to the build method to override defaults.
     */
    private ProcessedBamFile createProcessedBamFileWithLaterProcessedPass(Map map = [:]) {
        Map laterBamFileMap = map['laterBamFileMap'] ?: [:]
        Map passOfLaterBamFileMap = map['passOfLaterBamFileMap'] ?: [:]
        Map bamFileMap = map['bamFileMap'] ?: [:]
        Map seqTrackMap = map['seqTrackMap'] ?: [:]
        Map alignmentPassMap = map['alignmentPassMap'] ?: [:]

        SeqTrack seqTrack = SeqTrack.build(seqTrackMap)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: TestData.createAndSaveAlignmentPass([
                seqTrack: seqTrack,
                identifier: 2,
                alignmentState: AlignmentState.FINISHED,
            ] + alignmentPassMap)
        ] + bamFileMap)

        ProcessedBamFile processedBamFileLaterPass = ProcessedBamFile.build([
            alignmentPass: TestData.createAndSaveAlignmentPass([
                seqTrack: seqTrack,
                identifier: 3
            ] + passOfLaterBamFileMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
        ] + laterBamFileMap)

        return processedBamFile
    }

    /**
     * keys of map:
     * <ul>
     * <li>seqTrackMap</li>
     * <li>bamFileMap</li>
     * <li>mergingSetMap</li>
     * <li>mergingSetAssignmentMap</li>
     * <li>mergingPassMap</li>
     * <li>mergingBamFileMap</li>
     * </ul>
     * The values are a map to pass to the build method to override defaults.
     */
    private ProcessedBamFile createProcessedBamFileWhichIsMerged(Map map = [:]) {
        Map seqTrackMap = map['seqTrackMap']?:[:]
        Map bamFileMap = map['bamFileMap']?:[:]
        Map mergingSetMap = map['mergingSetMap']?:[:]
        Map mergingSetAssignmentMap = map['mergingSetAssignmentMap']?:[:]
        Map mergingPassMap = map['mergingPassMap']?:[:]
        Map mergingBamFileMap = map['mergingBamFileMap']?:[:]

        SeqTrack seqTrack = SeqTrack.build(seqTrackMap)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: TestData.createAndSaveAlignmentPass([
                seqTrack: seqTrack,
                identifier: 2,
                alignmentState: AlignmentState.FINISHED,
            ]),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED
        ] + bamFileMap)

        MergingSet mergedSet = MergingSet.build([
            mergingWorkPackage: processedBamFile.mergingWorkPackage,
            status: MergingSet.State.PROCESSED
        ] + mergingSetMap)

        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build([
            mergingSet: mergedSet,
            bamFile: processedBamFile,
        ] + mergingSetAssignmentMap)

        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: MergingPass.build([
                mergingSet: mergedSet,
            ] + mergingPassMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            workPackage: mergedSet.mergingWorkPackage,
        ] + mergingBamFileMap)

        return processedBamFile
    }

    /**
     * keys of map:
     * <ul>
     * <li>seqTrackMap</li>
     * <li>bamFileMap</li>
     * <li>passOfLaterBamFileMap</li>
     * <li>laterBamFileMap</li>
     * <li>processedSaiFileMap</li>
     * </ul>
     * The values are a map to pass to the build method to override defaults.
     */
    private ProcessedBamFile createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(Map map = [:]) {
        Map processedSaiFileMap = map['processedSaiFileMap'] ?: [:]

        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(map)
        //ensure, that first hql do not match
        processedBamFile.deletionDate = new Date()
        processedBamFile.save()

        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build([
            alignmentPass: processedBamFile.alignmentPass
        ] + processedSaiFileMap)

        return processedBamFile
    }

    /**
     * keys of map:
     * <ul>
     * <li>seqTrackMap</li>
     * <li>bamFileMap</li>
     * <li>mergingSetMap</li>
     * <li>mergingSetAssignmentMap</li>
     * <li>mergingPassMap</li>
     * <li>mergingBamFileMap</li>
     * <li>processedSaiFileMap</li>
     * </ul>
     * The values are a map to pass to the build method to override defaults.
     */
    private ProcessedBamFile createProcessedBamFileWithSaiFileWhichIsMerged(Map map = [:]) {
        Map processedSaiFileMap = map['processedSaiFileMap'] ?: [:]

        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(map)
        //ensure, that first hql do not match
        processedBamFile.deletionDate = new Date()
        processedBamFile.save()

        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build([
            alignmentPass: processedBamFile.alignmentPass
        ] + processedSaiFileMap)

        return processedBamFile
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_NoProcessedMergedBamFile() {
        Date createdBeforeDate = new Date()
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass()
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_WrongAlignmentState() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        alignmentPassMap: [
                            alignmentState: AlignmentState.NOT_STARTED,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_WrongQAState() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        laterBamFileMap: [
                            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_DateIsToLate() {
        Date createdBeforeDate = new Date().minus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass()
        changeDateForBamFile(processedBamFile, new Date().minus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_LaterBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_CurrentBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            withdrawn: true
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_BothBamFilesIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true
                        ],
                        bamFileMap: [
                            withdrawn: true
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_OtherSeqTrack() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            seqTrack: SeqTrack.build()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_SmallerPassIdentifier() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            identifier: 0
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged()
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_BamFileHasWrongQAStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        bamFileMap: [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_BamFileHasWrongStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        bamFileMap: [status: AbstractBamFile.State.INPROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_MergingSetHasWrongStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingSetMap: [status: MergingSet.State.INPROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_MergingSetAssignmentHasWrongMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.mergingSet = DomainFactory.createMergingSet(msa.mergingSet.mergingWorkPackage)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_MergingSetAssignmentHasWrongBamFile() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedMergedBamFile(msa.mergingSet.mergingWorkPackage)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }


    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileHasWrongQaStats() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileIsTooYoung() {
        Date createdBeforeDate = new Date().minus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [status: AbstractBamFile.State.INPROGRESS]
                        )
        changeDateForBamFile(processedBamFile, new Date().minus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [withdrawn: true]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        bamFileMap: [withdrawn: true]
                        )
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_BothFileAreWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [withdrawn: true],
                        bamFileMap: [withdrawn: true],
                        )
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileHasWrongMergingPass() {
        Date createdBeforeDate = new Date().plus(1)
        MergingPass mergingPass = MergingPass.build()
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [mergingPass: mergingPass, workPackage: mergingPass.mergingWorkPackage]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileHasWrongMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = MergingSet.build()
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingPassMap: [mergingSet: mergingSet],
                        mergingBamFileMap: [workPackage: mergingSet.mergingWorkPackage],
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_FileExistIsTrue() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            fileExists: true
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_DeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            deletionDate: new Date()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_FileExistIsFalseAndDeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            fileExists: true,
                            deletionDate: new Date(),
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_DateIsTooLate() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithLaterProcessedPass()
        changeDateForBamFile(processedBamFile, new Date().plus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass()
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_WrongAlignmentState() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        alignmentPassMap: [
                            alignmentState: AlignmentState.NOT_STARTED,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_WrongQAState() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        laterBamFileMap: [
                            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_DateIsToLate() {
        Date createdBeforeDate = new Date().minus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass()
        changeDateForBamFile(processedBamFile, new Date().minus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_LaterBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_CurrentBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        bamFileMap: [
                            withdrawn: true
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_BothBamFilesIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true
                        ],
                        bamFileMap: [
                            withdrawn: true
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_OtherSeqTrack() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            seqTrack: SeqTrack.build()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_SmallerPassIdentifier() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            identifier: 0
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged()
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_BamFileHasWrongQAStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        bamFileMap: [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_BamFileHasWrongStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        bamFileMap: [status: AbstractBamFile.State.INPROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_MergingSetHasWrongStatus() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingSetMap: [status: MergingSet.State.INPROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_MergingSetAssignmentHasWrongMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.mergingSet = DomainFactory.createMergingSet(msa.mergingSet.mergingWorkPackage)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_MergingSetAssignmentHasWrongBamFile() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedMergedBamFile(msa.mergingSet.mergingWorkPackage)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileHasWrongQaStats() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileIsTooYoung() {
        Date createdBeforeDate = new Date().minus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [status: AbstractBamFile.State.INPROGRESS]
                        )
        changeDateForBamFile(processedBamFile, new Date().minus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [withdrawn: true]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        bamFileMap: [withdrawn: true]
                        )
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_BothFileAreWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [withdrawn: true],
                        bamFileMap: [withdrawn: true],
                        )
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileHasWrongMergingPass() {
        Date createdBeforeDate = new Date().plus(1)
        MergingPass mergingPass = MergingPass.build()
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [mergingPass: mergingPass, workPackage: mergingPass.mergingWorkPackage],
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileHasWrongMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = MergingSet.build()
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingPassMap: [mergingSet: mergingSet],
                        mergingBamFileMap: [workPackage: mergingSet.mergingWorkPackage],
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_WrongAlignmentPass() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        processedSaiFileMap: [alignmentPass: TestData.createAndSaveAlignmentPass()]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_FileExistIsTrue() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        processedSaiFileMap: [
                            fileExists: true
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_DeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        processedSaiFileMap: [
                            deletionDate: new Date()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition__FileExistIsFalseAndDeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        processedSaiFileMap: [
                            fileExists: true,
                            deletionDate: new Date(),
                        ])
        createProcessedAlignmentFileService(processedBamFile)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_DateIsTooLate() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass()
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.findByAlignmentPass(processedBamFile.alignmentPass)
        //The dateCreated is changed via hibernate update, since grails handle this property itself and do not allow to set the value.
        processedSaiFile.executeUpdate("update ProcessedSaiFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: new Date().plus(2),
            id: processedSaiFile.id
        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

}

