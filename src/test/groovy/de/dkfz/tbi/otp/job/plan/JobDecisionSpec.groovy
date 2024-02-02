/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.job.plan

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class JobDecisionSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DecidingJobDefinition,
                JobDecision,
                JobExecutionPlan,
        ]
    }

    void "test validate, when jobDefinition, name and description are null, should fail"() {
        given:
        JobDecision decision = new JobDecision()

        when:
        boolean result = decision.validate()

        then:
        !result
        "nullable" == decision.errors["jobDefinition"].code
        "nullable" == decision.errors["name"].code
        "nullable" == decision.errors["description"].code
    }

    void "test validate, when name is blank, should fail"() {
        given:
        DecidingJobDefinition jobDefinition = createAndSaveDecidingJobDefinition()
        JobDecision decision = new JobDecision()
        decision.jobDefinition = jobDefinition
        decision.name = ""
        decision.description = ""

        when:
        boolean result = decision.validate()

        then:
        !result
        "blank" == decision.errors["name"].code
        decision.errors["jobDefinition"] == null
        decision.errors["description"] == null
    }

    void "test validate"() {
        given:
        DecidingJobDefinition jobDefinition = createAndSaveDecidingJobDefinition()
        JobDecision decision = new JobDecision()
        decision.jobDefinition = jobDefinition
        decision.name = "test"
        decision.description = "test"

        expect:
        decision.validate()
    }

    void "test validate, when name is not unique, should fail"() {
        given:
        DecidingJobDefinition jobDefinition = createAndSaveDecidingJobDefinition()
        JobDecision decision1 = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "arbitrary description")
        assert decision1.save(flush: true)
        JobDecision decision2 = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "arbitrary description")

        when:
        boolean result = decision2.validate()

        then:
        !result
        "unique" == decision2.errors["name"].code
    }

    void "test validate, when name is unique"() {
        given:
        DecidingJobDefinition jobDefinition = createAndSaveDecidingJobDefinition()
        JobDecision decision1 = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "arbitrary description")
        assert decision1.save(flush: true)
        JobDecision decision2 = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "arbitrary description")
        decision2.clearErrors()
        decision2.name = "test2"

        expect:
        decision2.validate()
    }

    private DecidingJobDefinition createAndSaveDecidingJobDefinition() {
        final JobExecutionPlan jep = DomainFactory.createJobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0)
        assert jep.save(flush: true)
        final DecidingJobDefinition decidingJobDefinition = new DecidingJobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert decidingJobDefinition.save(flush: true)
        return decidingJobDefinition
    }
}
