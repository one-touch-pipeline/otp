package de.dkfz.tbi.otp.job.processing



import de.dkfz.tbi.otp.job.plan.JobDefinition
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ParameterMapping)
class ParameterMappingTests {

    void testConstraints() {
       mockForConstraintsTests(ParameterMapping, [])
       ParameterMapping mapping = new ParameterMapping()
       assertFalse(mapping.validate())
       assertEquals("nullable", mapping.errors["from"])
       assertEquals("nullable", mapping.errors["to"])
       assertEquals("nullable", mapping.errors["job"])
       
       // mock the JobDefinition
       JobDefinition jobDefinition = new JobDefinition()
       JobDefinition jobDefinition2 = new JobDefinition()
       mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
       mapping.job = jobDefinition
       assertFalse(mapping.validate())
       assertEquals("nullable", mapping.errors["from"])
       assertEquals("nullable", mapping.errors["to"])
       assertEquals("validator", mapping.errors["job"])

       // mock some Parameter Types
       ParameterType type = new ParameterType(jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
       ParameterType type2 = new ParameterType(jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
       ParameterType type3 = new ParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT)
       ParameterType type4 = new ParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.OUTPUT)
       mockDomain(ParameterType, [type, type2, type3, type4])

       // use the from
       mapping.from = type
       assertFalse(mapping.validate())
       assertEquals("nullable", mapping.errors["to"])
       assertEquals("validator", mapping.errors["job"])
       assertEquals("parameterUsage", mapping.errors["from"])
       // use the to - same as from
       mapping.to = type
       assertFalse(mapping.validate())
       assertEquals("jobDefinition", mapping.errors["from"])
       assertEquals("jobDefinition", mapping.errors["to"])
       assertNull(mapping.errors["job"])
       // change the to to a wrong jobDefinition
       mapping.to = type3
       assertFalse(mapping.validate())
       assertEquals("validator", mapping.errors["job"])
       assertEquals("parameterUsage", mapping.errors["from"])
       assertNull(mapping.errors["to"])
       // use correct type for from, but same jobDefinition for to
       mapping.from = type2
       mapping.to = type
       assertFalse(mapping.validate())
       assertEquals("jobDefinition", mapping.errors["from"])
       assertEquals("jobDefinition", mapping.errors["to"])
       assertNull(mapping.errors["job"])
       // finally something useful
       mapping.from = type4
       assertTrue(mapping.validate())
    }
}
