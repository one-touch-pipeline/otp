package de.dkfz.tbi.otp.job.processing

/**
 * Enum specifying the usage for a parameter type. That is whether
 * the parameter type is for input parameters, output parameters or
 * passthrough parameters.
 *
 * @see ParameterType
 * @see Parameter
 */
enum ParameterUsage {
    /**
     * Used as Input Parameter
     */
    INPUT,
    /**
     * Used as Output Parameter
     */
    OUTPUT,
    /**
     * Parameter passed through the Job without usage.
     */
    PASSTHROUGH
}
