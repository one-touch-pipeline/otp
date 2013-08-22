package de.dkfz.tbi.otp.job.processing

class Parameter implements Serializable {
    ParameterType type
    String value

    static constraints = {
        value(size: 0..10000)
    }
}
