package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import grails.validation.*
import spock.lang.*

@Mock([
        JobDefinition,
        JobErrorDefinition,
        JobExecutionPlan,
])

class JobErrorDefinitionSpec extends Specification {


    def setup() {
        DomainFactory.createJobDefinition()

        JobErrorDefinition jobErrorDefinition = new JobErrorDefinition(errorExpression: "jobErrorDefinition", type: JobErrorDefinition.Type.MESSAGE, action: JobErrorDefinition.Action.STOP)
        jobErrorDefinition.save(flush: true)
    }

    void 'tries to add JobDefinition, succeeds'() {
        given:
        JobDefinition jobDefinition = CollectionUtils.exactlyOneElement(JobDefinition.findAll())
        JobErrorDefinition jobErrorDefinition = CollectionUtils.exactlyOneElement(JobErrorDefinition.findAll())

        when:
        jobErrorDefinition.addToJobDefinitions(jobDefinition)
        jobErrorDefinition.save(flush: true)
        jobErrorDefinition.addToJobDefinitions(jobDefinition)
        jobErrorDefinition.save(flush: true)

        then:
        jobErrorDefinition.jobDefinitions.contains(jobDefinition)
        jobErrorDefinition.jobDefinitions.size() == 1
    }

    void 'tries to add JobErrorDefinition, when action = furtherCheck, succeeds'() {
        given:
        JobErrorDefinition jobErrorDefinition = CollectionUtils.exactlyOneElement(JobErrorDefinition.findAll())
        JobErrorDefinition jobErrorDefinition1 = new JobErrorDefinition(errorExpression: "jobErrorDefinition1", type: JobErrorDefinition.Type.MESSAGE, action: JobErrorDefinition.Action.CHECK_FURTHER)

        when:
        jobErrorDefinition1.addToCheckFurtherJobErrors(jobErrorDefinition)
        jobErrorDefinition1.save(flush: true)

        then:
        jobErrorDefinition1.checkFurtherJobErrors.contains(jobErrorDefinition)
    }

    void 'create JobErrorDefinition with invalid errorExpression, should fail'() {
        given:
        JobErrorDefinition jobErrorDefinition = new JobErrorDefinition(errorExpression: "*", type: JobErrorDefinition.Type.MESSAGE, action: JobErrorDefinition.Action.STOP)

        when:
        jobErrorDefinition.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("* is not a valid REGEX")
    }

}
