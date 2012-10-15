package de.dkfz.tbi.otp.job.plan

/**
 * Representation for a Parameter.
 */
class ParameterInformation implements Serializable {
    Long id
    String value
    ParameterTypeInformation type
    /**
     * Id of the ParameterType this Parameter is mapped from.
     */
    Long mapping
}
