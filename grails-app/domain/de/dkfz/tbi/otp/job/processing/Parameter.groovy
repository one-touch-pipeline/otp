package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.utils.Entity

class Parameter implements Serializable, Entity {
    ParameterType type
    String value

    static mapping = {
        value type: 'text'
    }
}
