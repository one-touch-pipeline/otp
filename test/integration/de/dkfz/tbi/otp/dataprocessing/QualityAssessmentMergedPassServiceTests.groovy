package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.TestData

class QualityAssessmentMergedPassServiceTests {

    QualityAssessmentMergedPass qualityAssessmentMergedPass
    ProcessedMergedBamFile processedMergedBamFile
    Project project
    Realm realm

    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Before
    void setUp() {
        realm = DomainFactory.createRealmDataProcessingDKFZ([
            rootPath: '/tmp/otp-unit-test/pmfs/root',
            processingRootPath: '/tmp/otp-unit-test/pmbfs/processing',
            ]).save([flush: true])

        project = TestData.createProject(
                        name: "project",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        MergingWorkPackage mergingWorkPackage = new TestData().createMergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.UNKNOWN,
                        fileOperationStatus: FileOperationStatus.DECLARED,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED,
                        numberOfMergedLanes: 1
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))

        qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                        processedMergedBamFile: processedMergedBamFile,
                        identifier: 0,
                        description: 'QualtiyAssessmentMergedPassDescription'
                        )
        assertNotNull(qualityAssessmentMergedPass.save([flush: true]))
    }

    @After
    void tearDown() {
        qualityAssessmentMergedPass = null
        project = null
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
        assertEquals(project, qualityAssessmentMergedPassService.project(qualityAssessmentMergedPass))
    }

    @Test(expected = IllegalArgumentException)
    void testAllQualityAssessmentMergedPassesBamFileNull() {
        qualityAssessmentMergedPassService.allQualityAssessmentMergedPasses(null)
    }

    @Test
    void testAllQualityAssessmentMergedPasses() {
        assertEquals([qualityAssessmentMergedPass], qualityAssessmentMergedPassService.allQualityAssessmentMergedPasses(processedMergedBamFile))
        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = new QualityAssessmentMergedPass(
                        processedMergedBamFile: processedMergedBamFile,
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
                        processedMergedBamFile: processedMergedBamFile,
                        identifier: 1,
                        description: 'QualtiyAssessmentMergedPassDescription'
                        )
        assertNotNull(qualityAssessmentMergedPass1.save([flush: true]))
        assertEquals(qualityAssessmentMergedPass1, qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(processedMergedBamFile))
    }

    @Test
    void testCreatePassQualityAssessmentStatusWrong() {
        assertNull(qualityAssessmentMergedPassService.createPass())
    }

    @Test
    void testCreatePassBamTypeWrong() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        processedMergedBamFile.type = BamType.SORTED
        assertNull(qualityAssessmentMergedPassService.createPass())
    }

    @Test
    void testCreatePassWithdrawn() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        processedMergedBamFile.withdrawn = true
        assertNull(qualityAssessmentMergedPassService.createPass())
    }

    @Test
    void testCreatePass() {
        processedMergedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assertEquals(1, QualityAssessmentMergedPass.list().size())
        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = qualityAssessmentMergedPassService.createPass()
        assertEquals(1, qualityAssessmentMergedPass1.identifier)
        assertEquals(2, QualityAssessmentMergedPass.list().size())
    }
}
