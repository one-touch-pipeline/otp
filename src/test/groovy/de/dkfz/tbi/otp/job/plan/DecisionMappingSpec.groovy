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
package de.dkfz.tbi.otp.job.plan

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class DecisionMappingSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DecisionMapping,
                JobDecision,
                JobDefinition,
                JobExecutionPlan,
        ]
    }

    void "test validate, when decision and definition are null, should fail"() {
        given:
        DecisionMapping mapping = new DecisionMapping()

        when:
        boolean result = mapping.validate()

        then:
        !result
        "nullable" == mapping.errors["decision"].code
        "nullable" == mapping.errors["definition"].code
    }

    void "test validate, when definition is null, should fail"() {
        given:
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping(decision: decision)

        when:
        boolean result = mapping.validate()

        then:
        !result
        "nullable" == mapping.errors["definition"].code
        mapping.errors["decision"] == null
    }

    void "test validate, when definition is recursive, should fail"() {
        given:
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping(decision: decision, definition: jobDefinition)

        when:
        boolean result = mapping.validate()

        then:
        !result
        "recursive" == mapping.errors["definition"].code
    }

    void "test validate, when decision and definition are valid"() {
        given:
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan()
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition(plan: plan)
        JobDefinition jobDefinition2 = new JobDefinition(name: "name", plan: plan, bean: "bean")
        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping(decision: decision, definition: jobDefinition2)

        expect:
        mapping.validate()
    }

    void "test validate, when jobExecutionPlan is invalid, should fail"() {
        given:
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan()
        JobExecutionPlan plan1 = DomainFactory.createJobExecutionPlan()

        DecidingJobDefinition jobDefinition = new DecidingJobDefinition(plan: plan)

        JobDefinition jobDefinition2 = new JobDefinition(name: "name", plan: plan1, bean: "bean")
        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping(decision: decision, definition: jobDefinition2)

        when:
        boolean result = mapping.validate()

        then:
        !result
        "plan" == mapping.errors["definition"].code
    }

    void "test validate, when jobExecutionPlan is valid"() {
        given:
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan()

        DecidingJobDefinition jobDefinition = new DecidingJobDefinition(plan: plan)

        JobDefinition jobDefinition3 = new JobDefinition(name: "name", plan: plan, bean: "bean")
        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping(decision: decision, definition: jobDefinition3)

        expect:
        mapping.validate()
    }

    void "test validation, when decision is not unique, should fail"() {
        given:
        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan(name: "some name", planVersion: 0)

        DecidingJobDefinition decidingJobDefinition1 = new DecidingJobDefinition(name: "some name", bean: "someBean", plan: jep).save(flush: true)
        DecidingJobDefinition decidingJobDefinition2 = new DecidingJobDefinition(name: "other name", bean: "someOtherBean", plan: jep).save(flush: true)

        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: decidingJobDefinition1).save(flush: true)

        DecisionMapping mapping1 = new DecisionMapping(decision: decision, definition: decidingJobDefinition2)
        assert mapping1.save(flush: true)

        DecisionMapping mapping2 = new DecisionMapping(decision: decision, definition: decidingJobDefinition2)

        when:
        boolean result = mapping2.validate()

        then:
        !result
        "unique" == mapping2.errors["decision"].code
    }

    void "test validation, when decision is unique"() {
        given:
        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan(name: "some name", planVersion: 0)

        DecidingJobDefinition decidingJobDefinition1 = new DecidingJobDefinition(name: "some name", bean: "someBean", plan: jep).save(flush: true)
        DecidingJobDefinition decidingJobDefinition2 = new DecidingJobDefinition(name: "other name", bean: "someOtherBean", plan: jep).save(flush: true)

        JobDecision decision = new JobDecision(name: "name", description: "", jobDefinition: decidingJobDefinition1).save(flush: true)
        JobDecision decision2 = new JobDecision(name: "name2", description: "", jobDefinition: decidingJobDefinition1).save(flush: true)

        DecisionMapping mapping1 = new DecisionMapping(decision: decision, definition: decidingJobDefinition2)
        assert mapping1.save(flush: true)

        DecisionMapping mapping2 = new DecisionMapping(decision: decision2, definition: decidingJobDefinition2)

        expect:
        mapping2.validate()
    }
}
