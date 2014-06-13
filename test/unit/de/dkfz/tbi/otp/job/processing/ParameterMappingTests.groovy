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

    @Test
    void testConstraints() {
       ParameterMapping mapping = new ParameterMapping()
       assertFalse(mapping.validate())
       assertEquals("nullable", mapping.errors["from"].code)
       assertEquals("nullable", mapping.errors["to"].code)
       assertEquals("nullable", mapping.errors["job"].code)
       
       JobDefinition jobDefinition = new JobDefinition()
       JobDefinition jobDefinition2 = new JobDefinition()
       mapping.job = jobDefinition
       assertFalse(mapping.validate())
       assertEquals("nullable", mapping.errors["from"].code)
       assertEquals("nullable", mapping.errors["to"].code)
       assertEquals("validator.invalid", mapping.errors["job"].code)

       ParameterType type = new ParameterType(jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
       ParameterType type2 = new ParameterType(jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
       ParameterType type3 = new ParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT)
       ParameterType type4 = new ParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.OUTPUT)

       // use the from
       mapping.from = type
       assertFalse(mapping.validate())
       assertEquals("nullable", mapping.errors["to"].code)
       assertEquals("validator.invalid", mapping.errors["job"].code)
       assertEquals("parameterUsage", mapping.errors["from"].code)
       // use the to - same as from
       mapping.to = type
       assertFalse(mapping.validate())
       assertEquals("jobDefinition", mapping.errors["from"].code)
       assertEquals("jobDefinition", mapping.errors["to"].code)
       assertNull(mapping.errors["job"])
       // change the to to a wrong jobDefinition
       mapping.to = type3
       assertFalse(mapping.validate())
       assertEquals("validator.invalid", mapping.errors["job"].code)
       assertEquals("parameterUsage", mapping.errors["from"].code)
       assertNull(mapping.errors["to"])
       // use correct type for from, but same jobDefinition for to
       mapping.from = type2
       mapping.to = type
       assertFalse(mapping.validate())
       assertEquals("jobDefinition", mapping.errors["from"].code)
       assertEquals("jobDefinition", mapping.errors["to"].code)
       assertNull(mapping.errors["job"])
       // finally something useful
       mapping.from = type4
       assertTrue(mapping.validate())
    }
}
