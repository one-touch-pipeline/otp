package de.dkfz.tbi.otp.job.processing

class Parameter {
    ParameterType type
    String value

    static constraints = {
        value(size:0..1000)
    }
}
