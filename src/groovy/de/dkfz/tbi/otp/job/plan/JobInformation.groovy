package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.StartJob

/**
 * Class representing a serialized form of a JobDefinition and a
 * ProcessingStep for the JobDefinition.
 */
class JobInformation implements Serializable {
    Long id
    String name
    String bean
    boolean startJob
    boolean endStateAware
    List<ParameterInformation> constantParameters = []
    List<ParameterInformation> inputParameters = []
    List<ParameterInformation> outputParameters = []
    List<ParameterInformation> passthroughParameters = []

    // for executed Jobs
    Long processingStep
    Boolean created
    Boolean started
    Boolean finished
    Boolean succeeded
    Boolean failed

    /**
     * Creates a JobInformation from the given JobDefinition.
     * @param job
     * @return
     */
    static JobInformation fromJob(JobDefinition job) {
        JobInformation ret = new JobInformation()
        def bean = job.domainClass.grailsApplication.mainContext.getBean(job.bean)
        ret.startJob = (bean instanceof StartJob)
        ret.endStateAware = (bean instanceof EndStateAwareJob)
        ret.bean = job.bean
        ret.id = job.id
        ret.name = job.name
        job.constantParameters.each { param ->
            ret.constantParameters << new ParameterInformation(
                id: param.id,
                value: param.value,
                type: new ParameterTypeInformation(it)
            )
        }
        ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.INPUT).each {
            ret.inputParameters << new ParameterInformation(type: new ParameterTypeInformation(it), mapping: ParameterMapping.findByJobAndTo(job, it)?.from?.id)
        }
        ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.OUTPUT).each {
            ret.outputParameters << new ParameterInformation(type: new ParameterTypeInformation(it))
        }
        ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.PASSTHROUGH).each {
            ret.passthroughParameters << new ParameterInformation(type: new ParameterTypeInformation(it), mapping: ParameterMapping.findByJobAndTo(job, it)?.from?.id)
        }
        return ret
    }
}
