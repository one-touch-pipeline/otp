package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.plan.ValidatingJobDefinition
import org.junit.*

class PlanValidatorServiceTests {

    /**
     * Dependency Injection of PlanValidatorService
     **/
    def planValidatorService

    @SuppressWarnings("EmptyMethod")
    @Before
    void setUp() {
    }

    @SuppressWarnings("EmptyMethod")
    @After
    void tearDown() {
    }

    @Test
    void testMissingStartJob() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", planVersion: 0, enabled: true)
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NO_STARTJOB, errors[0])
    }

    @Test
    void testMissingStartJobBean() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", planVersion: 0, enabled: true)
        assertNotNull(plan.save())
        StartJobDefinition startJob = new StartJobDefinition(name: "test", bean: "thisBeanDoesNotExist12345", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.STARTJOB_BEAN_MISSING, errors[0])
    }

    @Test
    void testStartJobImplementsInterface() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", planVersion: 0, enabled: true)
        assertNotNull(plan.save())
        StartJobDefinition startJob = new StartJobDefinition(name: "test", bean: "testJob", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.STARTJOB_BEAN_NOT_IMPLEMENTING_STARTJOB, errors[0])
    }

    @Test
    void testMissingFirstJob() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", planVersion: 0, enabled: true)
        assertNotNull(plan.save())
        // neither start job nor first job
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NO_STARTJOB, errors[0])
        assertEquals(PlanValidatorService.NO_FIRSTJOB, errors[1])
        // creating start job should not change
        StartJobDefinition startJob = new StartJobDefinition(name: "test", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save())
        plan.firstJob = null
        assertNotNull(plan.save())
        errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NO_FIRSTJOB, errors[0])
    }

    @Test
    void testMissingJobBean() {
        JobExecutionPlan plan = createTestPlan()
        // create a JobDefinition with missing bean
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "thisBeanDoesNotExist12345", plan: plan)
        assertNotNull(jobDefinition.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${jobDefinition.id}, thisBeanDoesNotExist12345", errors[0])
        // creating a second Job which exist should not change anything
        JobDefinition jobDefinition2 = new JobDefinition(name: "testJob2", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${jobDefinition.id}, thisBeanDoesNotExist12345", errors[0])
        // adding a third Job with incorrect bean should not change anything
        JobDefinition jobDefinition3 = new JobDefinition(name: "testJob3", bean: "thisBeanDoesNotExist12345", plan: plan)
        assertNotNull(jobDefinition3.save())
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${jobDefinition.id}, thisBeanDoesNotExist12345", errors[0])
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${jobDefinition3.id}, thisBeanDoesNotExist12345", errors[1])
    }

    @Test
    void testAllJobsImplementJob() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "planValidatorService", plan: plan)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "testJob2", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition3 = new JobDefinition(name: "testJob3", bean: "testStartJob", plan: plan)
        assertNotNull(jobDefinition3.save())
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())

        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_NOT_IMPLEMENTING_JOB + "${jobDefinition.id}, planValidatorService", errors[0])
        assertEquals(PlanValidatorService.JOB_BEAN_NOT_IMPLEMENTING_JOB + "${jobDefinition3.id}, testStartJob", errors[1])
    }

    @Test
    void testLastJobIsEndStateAware() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())

        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.LAST_JOB_NOT_ENDSTATE_AWARE, errors[0])

        // add two further JobDefinitions
        JobDefinition jobDefinition2 = new JobDefinition(name: "testJob2", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition3 = new JobDefinition(name: "testJob3", bean: "testJob", plan: plan)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        // and it should still failerrors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.LAST_JOB_NOT_ENDSTATE_AWARE, errors[0])
    }

    @Test
    void testCircularDependency() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "testJob2", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition
        assertNotNull(jobDefinition.save())
        jobDefinition2.next = jobDefinition
        assertNotNull(jobDefinition2.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.CIRCULAR_JOBS, errors[0])
    }

    @Test
    void testMissingLinkdedJobDefiniton() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testEndStateAwareJob", plan: plan)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "testJob2", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition2.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NOT_ALL_JOBS_LINKED, errors[0])
    }

    @Test
    void testValidatorBeanIsValidatingJob() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition.save())
        ValidatingJobDefinition validator = new ValidatingJobDefinition(name: "validator", bean: "testEndStateAwareJob", validatorFor: jobDefinition, plan: plan)
        assertNotNull(validator.save())
        jobDefinition.next = validator
        assertNotNull(jobDefinition.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.VALIDATOR_BEAN_NOT_IMPLEMENTING_INTERFACE + "${validator.id}, testEndStateAwareJob", errors[0])
    }

    @Test
    void testValidatorBeforeToValidateJobDefinition() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testEndStateAwareJob", plan: plan)
        assertNotNull(jobDefinition.save())
        ValidatingJobDefinition validatingJobDefinition = new ValidatingJobDefinition(name: "validator", bean: "validatingTestJob", validatorFor: jobDefinition, plan: plan)
        assertNotNull(validatingJobDefinition.save())
        validatingJobDefinition.next = jobDefinition
        assertNotNull(validatingJobDefinition.save())
        jobDefinition.next = null
        assertNotNull(jobDefinition.save())
        plan.firstJob = validatingJobDefinition
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.VALIDATOR_LOOP, errors[0])
    }

    @Test
    void testValidatedJobNotEndstateAware() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testEndStateAwareJob", plan: plan)
        assertNotNull(jobDefinition.save())
        ValidatingJobDefinition validatingJobDefinition = new ValidatingJobDefinition(name: "validator", bean: "validatingTestJob", validatorFor: jobDefinition, plan: plan)
        assertNotNull(validatingJobDefinition.save())
        jobDefinition.next = validatingJobDefinition
        assertNotNull(jobDefinition.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())
        List<String> errors = planValidatorService.validate(plan)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.VALIDATOR_ON_ENDSTATE + "${validatingJobDefinition.id}, validatingTestJob", errors[0])
    }

    @Test
    void testCorrectPlan() {
        JobExecutionPlan plan = createTestPlan()
        JobDefinition jobDefinition = new JobDefinition(name: "testJob", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "testJob2", bean: "testJob", plan: plan)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition3 = new JobDefinition(name: "testJob3", bean: "testEndStateAwareJob", plan: plan)
        assertNotNull(jobDefinition3.save())
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        plan.firstJob = jobDefinition
        assertNotNull(plan.save())

        assertTrue(planValidatorService.validate(plan).isEmpty())
    }

    private JobExecutionPlan createTestPlan() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", planVersion: 0, enabled: true)
        assertNotNull(plan.save())
        StartJobDefinition startJob = new StartJobDefinition(name: "test", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save())
        return plan
    }
}
