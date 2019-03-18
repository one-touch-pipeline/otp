/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.otp.job.plan.JobDefinition

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
