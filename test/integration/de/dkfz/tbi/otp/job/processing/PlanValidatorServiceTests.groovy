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
     */
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
        plan("test") {
        }
        List<String> errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NO_STARTJOB, errors[0])
    }

    @Test
    void testMissingStartJobBean() {
        plan("test") {
            start("test", "thisBeanDoesNotExist12345")
        }
        List<String> errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.STARTJOB_BEAN_MISSING, errors[0])
    }

    @Test
    void testStartJobImplementsInterface() {
        plan("test") {
            start("test", "testJob")
        }
        List<String> errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.STARTJOB_BEAN_NOT_IMPLEMENTING_STARTJOB, errors[0])
    }

    @Test
    void testMissingFirstJob() {
        plan("test") {
        }
        // neither start job nor first job
        List<String> errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NO_STARTJOB, errors[0])
        assertEquals(PlanValidatorService.NO_FIRSTJOB, errors[1])
        plan("test2") {
            start("test", "testStartJob")
        }
        // creating start job should not change
        errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.NO_FIRSTJOB, errors[0])
    }

    @Test
    void testMissingJobBean() {
        plan("test") {
            start("test", "testStartJob")
            job("testJob", "thisBeanDoesNotExist12345")
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        List<String> errors = planValidatorService.validate(jep)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${JobDefinition.findByNameAndPlan('testJob', jep).id}, thisBeanDoesNotExist12345", errors[0])
        // creating a second Job which exist should not change anything
        plan("test2") {
            start("test", "testStartJob")
            job("testJob", "thisBeanDoesNotExist12345")
            job("testJob2", "testJob")
        }
        jep = JobExecutionPlan.list().last()
        errors = planValidatorService.validate(jep)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${JobDefinition.findByNameAndPlan('testJob', jep).id}, thisBeanDoesNotExist12345", errors[0])
        plan("test3") {
            start("test", "testStartJob")
            job("testJob", "thisBeanDoesNotExist12345")
            job("testJob2", "testJob")
            job("testJob3", "thisBeanDoesNotExist12345")
        }
        // adding a third Job with incorrect bean should not change anything
        jep = JobExecutionPlan.list().last()
        errors = planValidatorService.validate(jep)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${JobDefinition.findByNameAndPlan('testJob', jep).id}, thisBeanDoesNotExist12345", errors[0])
        assertEquals(PlanValidatorService.JOB_BEAN_MISSING + "${JobDefinition.findByNameAndPlan('testJob3', jep).id}, thisBeanDoesNotExist12345", errors[1])
    }

    @Test
    void testAllJobsImplementJob() {
        plan("test") {
            start("test", "testStartJob")
            job("testJob", "planValidatorService")
            job("testJob2", "testJob")
            job("testJob3", "testStartJob")
        }

        JobExecutionPlan jep = JobExecutionPlan.list().last()
        List<String> errors = planValidatorService.validate(jep)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.JOB_BEAN_NOT_IMPLEMENTING_JOB + "${JobDefinition.findByNameAndPlan('testJob', jep).id}, planValidatorService", errors[0])
        assertEquals(PlanValidatorService.JOB_BEAN_NOT_IMPLEMENTING_JOB + "${JobDefinition.findByNameAndPlan('testJob3', jep).id}, testStartJob", errors[1])
    }

    @Test
    void testLastJobIsEndStateAware() {
        plan("test") {
            start("test", "testStartJob")
            job("testJob", "testJob")
        }

        List<String> errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.LAST_JOB_NOT_ENDSTATE_AWARE, errors[0])

        // add two further JobDefinitions
        plan("test2") {
            start("test", "testStartJob")
            job("testJob", "testJob")
            job("testJob2", "testJob")
            job("testJob3", "testJob")
        }
        // and it should still fail
        errors = planValidatorService.validate(JobExecutionPlan.list().last())
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.LAST_JOB_NOT_ENDSTATE_AWARE, errors[0])
    }

    @Test
    void testCircularDependency() {
        plan("test") {
            start("test", "testStartJob")
            job("testJob", "testJob")
            job("testJob2", "testJob")
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        JobDefinition jobDefinition = JobDefinition.findByNameAndPlan('testJob', jep)
        JobDefinition jobDefinition2 = JobDefinition.findByNameAndPlan('testJob2', jep)
        jobDefinition2.next = jobDefinition
        assertNotNull(jobDefinition2.save())
        List<String> errors = planValidatorService.validate(jep)
        assertFalse(errors.isEmpty())
        assertEquals(PlanValidatorService.CIRCULAR_JOBS, errors[0])
    }

    @Test
    void testMissingLinkdedJobDefiniton() {
        plan("test") {
            start("test", "testStartJob")
            job("testJob", "testEndStateAwareJob")
            job("testJob2", "testJob")
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        JobDefinition jobDefinition = JobDefinition.findByNameAndPlan('testJob', jep)
        jobDefinition.next = null
        assertNotNull(jobDefinition.save())
        List<String> errors = planValidatorService.validate(jep)
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
