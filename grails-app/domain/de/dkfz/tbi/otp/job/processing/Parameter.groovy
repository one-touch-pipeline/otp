package de.dkfz.tbi.otp.job.processing

class Parameter implements Serializable {
    ParameterType type
    String value

    static mapping = {
        value type: 'text'
    }
}
