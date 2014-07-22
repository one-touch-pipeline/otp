package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.apache.commons.logging.Log
import static org.junit.Assert.*
import org.junit.*

@Mock([QualityAssessmentPass])
class AssignQaFlagJobUnitTests {

    private static final int PASS_ID = 1

    AssignQaFlagJob job

    @Before
    void setUp() {
        assert new QualityAssessmentPass(identifier: PASS_ID).save(validate: false)
    }

    @After
    void tearDown() {
        job == null
    }

    private void createJob(QualityAssessmentPassService qualityAssessmentPassService) {
        def processedBamFileService = [
            setNeedsProcessing: { processedBamFile -> },
        ] as ProcessedBamFileService
        job = new AssignQaFlagJob(
                qualityAssessmentPassService: qualityAssessmentPassService,
                processedBamFileService: processedBamFileService,
            )
        AssignQaFlagJob.metaClass.getProcessParameterValue = { -> PASS_ID as long }
    }

    @Test
    void testExecuteFailureByDifferentNumberOfReadsFromFastqc() {
        def qualityAssessmentPassService = [
            passFinished: { p -> },
            assertNumberOfReadsIsTheSameAsCalculatedWithFastqc: { p -> throw new QualityAssessmentException("counts are different...") }
        ] as QualityAssessmentPassService

        createJob(qualityAssessmentPassService)
        AssignQaFlagJob.metaClass.getEndState = { -> null }

        job.start()
        try {
            job.execute()
        } catch (QualityAssessmentException e) {
            job.end()
            boolean isJobFinished = job.getState() == AbstractJobImpl.State.FINISHED
            assertTrue(isJobFinished)
            boolean isJobStateSucessfull = job.endState == ExecutionState.SUCCESS
            assertTrue(!isJobStateSucessfull)
        }
    }

    @Test
    void testExecuteSucess() {
        def qualityAssessmentPassService = [
            passFinished: { p -> },
            assertNumberOfReadsIsTheSameAsCalculatedWithFastqc: { p -> }
        ] as QualityAssessmentPassService
        createJob(qualityAssessmentPassService)

        job.start()
        job.execute()
        job.end()

        boolean isJobFinished = job.getState() == AbstractJobImpl.State.FINISHED
        boolean isJobStateSucessfull = job.endState == ExecutionState.SUCCESS
        assertTrue(isJobFinished)
        assertTrue(isJobStateSucessfull)
    }
}
