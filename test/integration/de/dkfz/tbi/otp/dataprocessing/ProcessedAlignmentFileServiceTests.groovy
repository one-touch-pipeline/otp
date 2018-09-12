package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProcessedAlignmentFileServiceTests {

    private static final int LENGTH_NO_FILE = 0

    private static final int LENGTH_ONE_FILE = 10

    ProcessedAlignmentFileService processedAlignmentFileService



    private void changeDateForBamFile(ProcessedBamFile processedBamFile, Date date) {
        //The dateCreated is changed via hibernate update, since grails handle this property itself and do not allow to set the value.
        processedBamFile.executeUpdate("update ProcessedBamFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: date,
            id: processedBamFile.id,
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
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass([
            seqTrack: seqTrack,
            identifier: 1,
        ])

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: alignmentPass,
        ])

        AlignmentPass alignmentPass2 = DomainFactory.createAlignmentPass([
            alignmentState: AlignmentState.FINISHED,
            seqTrack: seqTrack,
            identifier: 2,
        ] + secondAlignmentPassMap)

        ProcessedBamFile processedBamFile2 = ProcessedBamFile.build([
            alignmentPass: alignmentPass2,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            withdrawn: false,
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
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS,
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
            withdrawn: true,
        ])
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_SecondPass_OtherSeqTrack() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted([:], [
            seqTrack: DomainFactory.createSeqTrack(),
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



    private void createProcessedAlignmentFileService(List<ProcessedBamFile> processedBamFile = null) {
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
                    def expected = processedBamFile.alignmentPass
                    assert CollectionUtils.containSame(expected, data)
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
    private List<ProcessedBamFile> createProcessedBamFileWithLaterProcessedPass(Map map = [:]) {
        Map laterBamFileMap = map['laterBamFileMap'] ?: [:]
        Map passOfLaterBamFileMap = map['passOfLaterBamFileMap'] ?: [:]
        Map bamFileMap = map['bamFileMap'] ?: [:]
        Map seqTrackMap = map['seqTrackMap'] ?: [:]
        Map alignmentPassMap = map['alignmentPassMap'] ?: [:]

        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqTrackMap)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: DomainFactory.createAlignmentPass([
                seqTrack: seqTrack,
                identifier: 2,
                alignmentState: AlignmentState.FINISHED,
            ] + alignmentPassMap)
        ] + bamFileMap)

        ProcessedBamFile processedBamFileLaterPass = ProcessedBamFile.build([
            alignmentPass: DomainFactory.createAlignmentPass([
                seqTrack: seqTrack,
                identifier: 3,
            ] + passOfLaterBamFileMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
        ] + laterBamFileMap).save(flush: true)

        return [processedBamFile, processedBamFileLaterPass]
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

        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqTrackMap).save(flush: true)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: DomainFactory.createAlignmentPass([
                seqTrack: seqTrack,
                identifier: 2,
                alignmentState: AlignmentState.FINISHED,
            ]),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
        ] + bamFileMap).save(flush: true)

        MergingSet mergedSet = MergingSet.build([
            mergingWorkPackage: processedBamFile.mergingWorkPackage,
            status: MergingSet.State.PROCESSED,
        ] + mergingSetMap).save(flush: true)

        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build([
            mergingSet: mergedSet,
            bamFile: processedBamFile,
        ] + mergingSetAssignmentMap).save(flush: true)

        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.build([
            mergingPass: MergingPass.build([
                mergingSet: mergedSet,
            ] + mergingPassMap),
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            workPackage: mergedSet.mergingWorkPackage,
        ] + mergingBamFileMap).save(flush: true)

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
    private List<ProcessedBamFile> createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(Map map = [:]) {
        Map processedSaiFileMap = map['processedSaiFileMap'] ?: [:]

        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(map)
        //ensure, that first hql do not match
        processedBamFiles[0].deletionDate = new Date()
        processedBamFiles[0].save(flush: true)

        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build([
            alignmentPass: processedBamFiles[0].alignmentPass,
        ] + processedSaiFileMap)

        return processedBamFiles
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
        processedBamFile.save(flush: true)

        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build([
            alignmentPass: processedBamFile.alignmentPass,
        ] + processedSaiFileMap).save(flush: true)

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
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass()
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_WrongAlignmentState() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        alignmentPassMap: [
                            alignmentState: AlignmentState.NOT_STARTED,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_WrongQAState() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        laterBamFileMap: [
                            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_DateIsToLate() {
        Date createdBeforeDate = new Date().minus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass()
        changeDateForBamFile(processedBamFiles[0], new Date().minus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_LaterBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true,
                        ])
        createProcessedAlignmentFileService([processedBamFiles[1]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_CurrentBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            withdrawn: true,
                        ])
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_BothBamFilesIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true,
                        ],
                        bamFileMap: [
                            withdrawn: true,
                        ])
        createProcessedAlignmentFileService(processedBamFiles)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_OtherSeqTrack() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            seqTrack: DomainFactory.createSeqTrack()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_ConditionLaterPassHasProcessed_SmallerPassIdentifier() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            identifier: 0,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged()
        createProcessedAlignmentFileService([processedBamFile])

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
        msa.save(flush: true)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_MergingSetAssignmentHasWrongBamFile() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedMergedBamFile(msa.mergingSet.mergingWorkPackage)
        msa.save(flush: true)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }


    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileHasWrongQaStats() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createProcessedAlignmentFileService([processedBamFile])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
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
        createProcessedAlignmentFileService([processedBamFile])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_BothFileAreWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [withdrawn: true],
                        bamFileMap: [withdrawn: true],
                        )
        createProcessedAlignmentFileService([processedBamFile])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileHasWrongMergingPass() {
        Date createdBeforeDate = new Date().plus(1)
        MergingPass mergingPass = DomainFactory.createMergingPass()
        ProcessedBamFile processedBamFile = createProcessedBamFileWhichIsMerged(
                        mergingBamFileMap: [mergingPass: mergingPass, workPackage: mergingPass.mergingWorkPackage]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_ConditionMerged_ProcessedMergedBamFileHasWrongMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = DomainFactory.createMergingSet()
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
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            fileExists: true,
                        ])
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_DeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            deletionDate: new Date()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_FileExistIsFalseAndDeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass(
                        bamFileMap: [
                            fileExists: true,
                            deletionDate: new Date(),
                        ])
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_GeneralCondition_DateIsTooLate() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithLaterProcessedPass()
        changeDateForBamFile(processedBamFiles[0], new Date().plus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass()
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_WrongAlignmentState() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        alignmentPassMap: [
                            alignmentState: AlignmentState.NOT_STARTED,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_WrongQAState() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        laterBamFileMap: [
                            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_DateIsToLate() {
        Date createdBeforeDate = new Date().minus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass()
        changeDateForBamFile(processedBamFiles[0], new Date().minus(2))
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_LaterBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true,
                        ])
        createProcessedAlignmentFileService([processedBamFiles[1]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_CurrentBamFileIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        bamFileMap: [
                            withdrawn: true,
                        ])
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_BothBamFilesIsWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        laterBamFileMap: [
                            withdrawn: true,
                        ],
                        bamFileMap: [
                            withdrawn: true,
                        ])
        createProcessedAlignmentFileService(processedBamFiles)

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_OtherSeqTrack() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            seqTrack: DomainFactory.createSeqTrack()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionLaterPassHasProcessed_SmallerPassIdentifier() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        passOfLaterBamFileMap: [
                            identifier: 0,
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }



    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_Ok() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged()
        createProcessedAlignmentFileService([processedBamFile])

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
        msa.save(flush: true)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_MergingSetAssignmentHasWrongBamFile() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedMergedBamFile(msa.mergingSet.mergingWorkPackage)
        msa.save(flush: true)
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileHasWrongQaStats() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS]
                        )
        createProcessedAlignmentFileService([processedBamFile])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
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
        createProcessedAlignmentFileService([processedBamFile])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_BothFileAreWithdrawn() {
        Date createdBeforeDate = new Date().plus(1)
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [withdrawn: true],
                        bamFileMap: [withdrawn: true],
                        )
        createProcessedAlignmentFileService([processedBamFile])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileHasWrongMergingPass() {
        Date createdBeforeDate = new Date().plus(1)
        MergingPass mergingPass = DomainFactory.createMergingPass()
        ProcessedBamFile processedBamFile = createProcessedBamFileWithSaiFileWhichIsMerged(
                        mergingBamFileMap: [mergingPass: mergingPass, workPackage: mergingPass.mergingWorkPackage],
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_ConditionMerged_ProcessedMergedBamFileHasWrongMergingSet() {
        Date createdBeforeDate = new Date().plus(1)
        MergingSet mergingSet = DomainFactory.createMergingSet()
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
                        processedSaiFileMap: [alignmentPass: DomainFactory.createAlignmentPass()]
                        )
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_FileExistIsTrue() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        processedSaiFileMap: [
                            fileExists: true,
                        ])
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_DeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        processedSaiFileMap: [
                            deletionDate: new Date()
                        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition__FileExistIsFalseAndDeletionDateIsNotNull() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass(
                        processedSaiFileMap: [
                            fileExists: true,
                            deletionDate: new Date(),
                        ])
        createProcessedAlignmentFileService([processedBamFiles[0]])

        assert LENGTH_ONE_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    @Test
    void testDeleteOldAlignmentProcessingFiles_WithSaiFile_GeneralCondition_DateIsTooLate() {
        Date createdBeforeDate = new Date().plus(1)
        List<ProcessedBamFile> processedBamFiles = createProcessedBamFileWithSaiFileAndWithLaterProcessedPass()
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.findByAlignmentPass(processedBamFiles[0].alignmentPass)
        //The dateCreated is changed via hibernate update, since grails handle this property itself and do not allow to set the value.
        processedSaiFile.executeUpdate("update ProcessedSaiFile set dateCreated = :dateCreated where id = :id", [
            dateCreated: new Date().plus(2),
            id: processedSaiFile.id,
        ])
        createProcessedAlignmentFileService()

        assert LENGTH_NO_FILE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

}

