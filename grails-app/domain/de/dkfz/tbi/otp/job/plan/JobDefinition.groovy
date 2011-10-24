package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType

class JobDefinition {
    static hasMany = [constantParameters: Parameter]
    /**
     * The JobExecutionPlan this JobDefinition belongs to.
     */
    JobExecutionPlan plan
    /**
     * The previous JobDefinition in the execution plan before this one.
     * For the first JobDefinition this is null.
     */
    JobDefinition previous
    /**
     * The next JobDefinition in the execution plan after this one.
     * For the last JobDefinition this is null.
     */
    JobDefinition next
    /**
     * A descriptive name for this JobDefinition
     */
    String name
    /**
     * The name of the Spring Bean which implements the Job to be executed for this JobDefinition
     */
    String bean
    /**
     * Mapping of parameters. As key the parameter type of the previous Job is used, as value
     * the Parameter to which it is mapped.
     */
    Map<ParameterType, ParameterType> parameterMapping

    static constraints = {
        name(nullable: false, blank: false, unique: 'plan')
        bean(nullable: false, blank: false)
        previous(nullable: true)
        next(nullable: true)
        plan(nullable: false)
    }
}
