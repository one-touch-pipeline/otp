package de.dkfz.tbi.otp.job.plan

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Test

import static org.junit.Assert.*

@TestMixin(ControllerUnitTestMixin)
@TestFor(JobDecision)
@Build([JobExecutionPlan,DecidingJobDefinition])
class JobDecisionTests {

    @Test
    void testNullableAndBlank() {
       DecidingJobDefinition jobDefinition = createAndSaveDecidingJobDefinition()

       JobDecision decision = new JobDecision()
       assertFalse(decision.validate())
       assertEquals("nullable", decision.errors["jobDefinition"].code)
       assertEquals("nullable", decision.errors["name"].code)
       assertEquals("nullable", decision.errors["description"].code)

       decision.clearErrors()
       decision.jobDefinition = jobDefinition
       decision.name = ""
       decision.description = ""
       assertFalse(decision.validate())
       assertEquals("blank", decision.errors["name"].code)
       assertNull(decision.errors["jobDefinition"])
       assertNull(decision.errors["description"])

       decision.clearErrors()
       decision.name = "test"
       decision.description = "test"
       assertTrue(decision.validate())
    }

    @Test
    void testUniqueness() {
       DecidingJobDefinition jobDefinition = createAndSaveDecidingJobDefinition()
       JobDecision decision1 = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "arbitrary description")
       assert decision1.save(flush: true)
       JobDecision decision2 = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "arbitrary description")

       assertFalse(decision2.validate())
       assertEquals("unique", decision2.errors["name"].code)

       decision2.clearErrors()
       decision2.name = "test2"
       assertTrue(decision2.validate())
    }

   private DecidingJobDefinition createAndSaveDecidingJobDefinition() {
      final JobExecutionPlan jep = JobExecutionPlan.build(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0)//, startJobBean: "DontCare")
      assert jep.save(flush: true)
      final DecidingJobDefinition decidingJobDefinition = DecidingJobDefinition.build(name: "DontCare", bean: "DontCare", plan: jep)
      assert decidingJobDefinition.save(flush: true)
      return decidingJobDefinition
   }

}
