package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType

/**
 * Representation for a ParameterType.
 *
 */
class ParameterTypeInformation implements Serializable {
    Long id
    String name
    String description
    String className

    ParameterTypeInformation(Parameter param) {
        this.id = param.type.id
        this.name = param.type.name
        this.description = param.type.description
        this.className = param.type.className
    }

    ParameterTypeInformation(ParameterType type) {
        this.id = type.id
        this.name = type.name
        this.description = type.description
        this.className = type.className
    }

}
