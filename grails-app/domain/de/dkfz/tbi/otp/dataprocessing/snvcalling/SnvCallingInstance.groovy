package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*

/**
 * @deprecated: succeeded by {@link RoddySnvCallingInstance}
 */
@Deprecated
class SnvCallingInstance extends AbstractSnvCallingInstance implements ProcessParameterObject, Entity {

    static constraints = {
        config validator: { val, obj ->
            SnvConfig.isAssignableFrom(Hibernate.getClass(val))
        }
    }
}
