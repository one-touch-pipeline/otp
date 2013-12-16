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

    AssignQaFlagJob job

    @Before
    void setUp() {
    }

    @After
    void tearDown() {
        job == null
    }

    @Test
    void testExecuteFailureByDifferentNumberOfReadsFromFastqc() {
        def qualityAssessmentPassService = [
            passFinished: { p -> },
            assertNumberOfReadsIsTheSameAsCalculatedWithFastqc: { p -> throw new QualityAssessmentException("counts are different...") }
        ] as QualityAssessmentPassService

        job = new AssignQaFlagJob(
                qualityAssessmentPassService: qualityAssessmentPassService
            )
        AssignQaFlagJob.metaClass.getProcessParameterValue = { -> 1 as long }
        AssignQaFlagJob.metaClass.getEndState = { -> null }

        job.start()
        try {
            job.execute()
        } catch (QualityAssessmentException e) {
            job.end()
            boolean isJobFinished = job.getState() == ExecutionState.FINISHED
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
        job = new AssignQaFlagJob(
                qualityAssessmentPassService: qualityAssessmentPassService
            )
        AssignQaFlagJob.metaClass.getProcessParameterValue = { -> 1 as long }

        job.start()
        job.execute()
        job.end()

        boolean isJobFinished = job.getState() == ExecutionState.FINISHED
        boolean isJobStateSucessfull = job.endState == ExecutionState.SUCCESS
        assertTrue(isJobFinished)
        assertTrue(isJobStateSucessfull)
    }
}
