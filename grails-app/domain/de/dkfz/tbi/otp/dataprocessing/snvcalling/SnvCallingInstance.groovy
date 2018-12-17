package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.hibernate.Hibernate

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.utils.Entity

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
