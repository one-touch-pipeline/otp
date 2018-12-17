package de.dkfz.tbi.otp.job.plan

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class JobErrorDefinitionServiceSpec extends Specification {

    @Unroll
    void "getAllJobErrorDefinition with one JobErrorDefinition"() {
        given:
        DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        Map jobErrorDefinitions = service.getAllJobErrorDefinition()

        then:
        jobErrorDefinitions.size() == 1
    }

    @Unroll
    void "getAllJobErrorDefinition with multiple JobErrorDefinitions"() {
        given:
        DomainFactory.createJobErrorDefinition()
        DomainFactory.createJobErrorDefinition(
                type: JobErrorDefinition.Type.STACKTRACE,
                action: JobErrorDefinition.Action.RESTART_WF,
                errorExpression: "ERROR_EXPRESSION",
        )
        DomainFactory.createJobErrorDefinition(
                type: JobErrorDefinition.Type.CLUSTER_LOG,
                action: JobErrorDefinition.Action.RESTART_JOB,
                errorExpression: "ERROR_EXPRESSION",
        )
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        Map jobErrorDefinitions = service.getAllJobErrorDefinition()

        then:
        jobErrorDefinitions.size() == 3
    }

    @Unroll
    void "getJobDefinition with one JobErrorDefinition"() {
        given:
        DomainFactory.createJobErrorDefinition(
                jobDefinitions: DomainFactory.createJobDefinition().list()
        )
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        List<JobDefinition> jobDefinitions = service.getJobDefinition(service.getAllJobErrorDefinition())

        then:
        jobDefinitions.size() == 1
    }

    @Unroll
    void "getJobDefinition with multiple JobErrorDefinitions"() {
        given:
        DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        List<JobDefinition> jobDefinitions = service.getJobDefinition(service.getAllJobErrorDefinition())

        then:
        jobDefinitions.size() == 3
    }

    @Unroll
    void "add JobErrorDefinitions to first level"() {
        given:
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        service.addErrorExpressionFirstLevel(type, action, errorExpression)

        then:
        service.getAllJobErrorDefinition().get(JobErrorDefinition.findByErrorExpression(errorExpression)) == errorExpression

        where:
        type                            | action        | errorExpression
        JobErrorDefinition.Type.MESSAGE | "STOP"        | "ERROR_TEXT"
        JobErrorDefinition.Type.MESSAGE | "RESTART_JOB" | "ERROR_EXPRESSION"
        JobErrorDefinition.Type.MESSAGE | "RESTART_WF"  | "FAKE_TEXT"
    }

    @Unroll
    void "add JobErrorDefinitions to second level"() {
        given:
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition(
                action: JobErrorDefinition.Action.CHECK_FURTHER,
                checkFurtherJobErrors: [],
        )

        when:
        service.addErrorExpression(type, action, errorExpression, jobErrorDefinition)

        then:
        service.getAllJobErrorDefinition().get(jobErrorDefinition).get(JobErrorDefinition.findByErrorExpression(errorExpression)) == errorExpression

        where:
        type          | action        | errorExpression
        "CLUSTER_LOG" | "STOP"        | "ERROR_TEXT"
        "STACKTRACE"  | "RESTART_JOB" | "ERROR_EXPRESSION"
        "MESSAGE"     | "RESTART_WF"  | "FAKE_TEXT"
    }

    @Unroll
    void "UpdateErrorExpression with one JobErrorDefinition"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        String errorExpression = "ERROR_EXPRESSION"

        when:
        service.updateErrorExpression(jobErrorDefinition, errorExpression)

        then:
        jobErrorDefinition.getErrorExpression() == errorExpression
    }

    @Unroll
    void "UpdateErrorExpression with multiple JobErrorDefinitions"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        service.updateErrorExpression(jobErrorDefinition, errorExpression)

        then:
        jobErrorDefinition.getErrorExpression() == errorExpression

        where:
        errorExpression         | _
        "ERROR_EXPRESSION"      | _
        "ERROR_EXPRESSION_TWO"  | _
        "ERROR_EXPRESSION_MORE" | _
    }

    @Unroll
    void "addNewJob with one JobErrorDefinition"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(name: "NAME")

        when:
        service.addNewJob(jobErrorDefinition, jobDefinition)

        then:
        jobErrorDefinition.getJobDefinitions().contains(jobDefinition)
    }

    @Unroll
    void "addNewJob with multiple JobErrorDefinitions"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        service.addNewJob(jobErrorDefinition, jobDefinition)

        then:
        jobErrorDefinition.getJobDefinitions().contains(jobDefinition)

        where:
        jobDefinition                                       | _
        DomainFactory.createJobDefinition(name: "NAME")     | _
        DomainFactory.createJobDefinition(name: "NEW_NAME") | _
    }

}
