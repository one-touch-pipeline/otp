package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

class QualityAssessmentMergedPassServiceTests {

    QualityAssessmentMergedPass qualityAssessmentMergedPass
    ProcessedMergedBamFile processedMergedBamFile
    Realm realm

    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Before
    void setUp() {
        realm = DomainFactory.createRealm().save([flush: true])

        processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()

        processedMergedBamFile.project.realm = realm
        assert processedMergedBamFile.project.save(flush: true)

        qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                        abstractMergedBamFile: processedMergedBamFile,
                        identifier: 0,
                        description: 'QualtiyAssessmentMergedPassDescription'
                        )
        assertNotNull(qualityAssessmentMergedPass.save([flush: true]))
    }

    @After
    void tearDown() {
        qualityAssessmentMergedPass = null
        processedMergedBamFile = null
        realm = null
    }

    @Test(expected = IllegalArgumentException)
    void testPassStartedNullInput() {
        qualityAssessmentMergedPassService.passStarted(null)
    }

    @Test
    void testPassStarted() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedMergedBamFile.qualityAssessmentStatus)
        qualityAssessmentMergedPassService.passStarted(qualityAssessmentMergedPass)
        assertEquals(QaProcessingStatus.IN_PROGRESS, processedMergedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testPassFinishedNullInput() {
        qualityAssessmentMergedPassService.passFinished(null)
    }

    @Test
    void testPassFinished() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedMergedBamFile.qualityAssessmentStatus)
        assertEquals(FileOperationStatus.DECLARED, processedMergedBamFile.fileOperationStatus)
        qualityAssessmentMergedPassService.passFinished(qualityAssessmentMergedPass)
        assertEquals(QaProcessingStatus.FINISHED, processedMergedBamFile.qualityAssessmentStatus)
        assertEquals(FileOperationStatus.NEEDS_PROCESSING, processedMergedBamFile.fileOperationStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateStateNull() {
        qualityAssessmentMergedPassService.update(qualityAssessmentMergedPass, null)
    }

    @Test(expected = IllegalArgumentException)
    void testUpdatePassNull() {
        qualityAssessmentMergedPassService.update(null, QaProcessingStatus.UNKNOWN)
    }

    @Test
    void testUpdate() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedMergedBamFile.qualityAssessmentStatus)
        qualityAssessmentMergedPassService.update(qualityAssessmentMergedPass, QaProcessingStatus.NOT_STARTED)
        assertEquals(QaProcessingStatus.NOT_STARTED, processedMergedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmForDataProcessingPassNull() {
        qualityAssessmentMergedPassService.realmForDataProcessing(null)
    }

    @Test
    void testRealmForDataProcessing() {
        assertEquals(realm, qualityAssessmentMergedPassService.realmForDataProcessing(qualityAssessmentMergedPass))
    }

    @Test(expected = IllegalArgumentException)
    void testProjectPassNull() {
        qualityAssessmentMergedPassService.project(null)
    }

    @Test
    void testProject() {
        assertEquals(processedMergedBamFile.project, qualityAssessmentMergedPassService.project(qualityAssessmentMergedPass))
    }

    @Test(expected = IllegalArgumentException)
    void testAllQualityAssessmentMergedPassesBamFileNull() {
        qualityAssessmentMergedPassService.allQualityAssessmentMergedPasses(null)
    }

    @Test
    void testAllQualityAssessmentMergedPasses() {
        assertEquals([qualityAssessmentMergedPass], qualityAssessmentMergedPassService.allQualityAssessmentMergedPasses(processedMergedBamFile))
        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = new QualityAssessmentMergedPass(
                        abstractMergedBamFile: processedMergedBamFile,
                        identifier: 1,
                        description: 'QualtiyAssessmentMergedPassDescription'
                        )
        assertNotNull(qualityAssessmentMergedPass1.save([flush: true]))
        assertEquals([qualityAssessmentMergedPass1, qualityAssessmentMergedPass], qualityAssessmentMergedPassService.allQualityAssessmentMergedPasses(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testLatestQualityAssessmentMergedPassFileNull() {
        qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(null)
    }

    @Test
    void testLatestQualityAssessmentMergedPass() {
        assertEquals(qualityAssessmentMergedPass, qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(processedMergedBamFile))
        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = new QualityAssessmentMergedPass(
                        abstractMergedBamFile: processedMergedBamFile,
                        identifier: 1,
                        description: 'QualtiyAssessmentMergedPassDescription'
                        )
        assertNotNull(qualityAssessmentMergedPass1.save([flush: true]))
        assertEquals(qualityAssessmentMergedPass1, qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(processedMergedBamFile))
    }

    @Test
    void testCreatePassQualityAssessmentStatusWrong() {
        assertNull(qualityAssessmentMergedPassService.createPass(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testCreatePassBamTypeWrong() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        processedMergedBamFile.type = BamType.SORTED
        assert processedMergedBamFile.save(flush: true)
        assertNull(qualityAssessmentMergedPassService.createPass(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testCreatePassWithdrawn() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        processedMergedBamFile.withdrawn = true
        assert processedMergedBamFile.save(flush: true)
        assertNull(qualityAssessmentMergedPassService.createPass(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testCreatePass() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assert processedMergedBamFile.save(flush: true)
        assertEquals(1, QualityAssessmentMergedPass.list().size())
        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = qualityAssessmentMergedPassService.createPass(ProcessingPriority.NORMAL_PRIORITY)
        assertEquals(1, qualityAssessmentMergedPass1.identifier)
        assertEquals(2, QualityAssessmentMergedPass.list().size())
    }


    @Test
    void testCreatePass_FastTrackFirst() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assert processedMergedBamFile.save(flush: true)

        ProcessedMergedBamFile fastTrackBamFile =  processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        fastTrackBamFile.project.realm = realm
        fastTrackBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assert fastTrackBamFile.save(flush: true)

        fastTrackBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert fastTrackBamFile.project.save(flush: true)

        QualityAssessmentMergedPass fastTrackPass = qualityAssessmentMergedPassService.createPass(ProcessingPriority.NORMAL_PRIORITY)
        assert fastTrackPass.abstractMergedBamFile == fastTrackBamFile
    }

    @Test
    void testCreatePass_JobsReservedForFastTrack() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assert processedMergedBamFile.save(flush: true)
        assertEquals(1, QualityAssessmentMergedPass.list().size())

        assertNull(qualityAssessmentMergedPassService.createPass(ProcessingPriority.FAST_TRACK_PRIORITY))
        assertEquals(1, QualityAssessmentMergedPass.list().size())

        processedMergedBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert processedMergedBamFile.project.save(flush: true)

        qualityAssessmentMergedPassService.createPass(ProcessingPriority.FAST_TRACK_PRIORITY)
        assertEquals(2, QualityAssessmentMergedPass.list().size())
    }

}
