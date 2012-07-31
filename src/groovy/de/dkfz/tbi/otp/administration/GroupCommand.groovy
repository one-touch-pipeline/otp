package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.Role

/**
 * Command object to perform data binding for creation of a Group.
 *
 * For further documentation about the various available fields,
 * please see documentation of Group.
 *
 * @see Group
 */
@grails.validation.Validateable
class GroupCommand {
    String name
    String description
    boolean readProject
    boolean writeProject
    boolean readJobSystem
    boolean writeJobSystem
    boolean readSequenceCenter
    boolean writeSequenceCenter

    static constraints = {
        name(blank: false, validator: { String value ->
            return Role.findByAuthority("GROUP_" + value.toUpperCase().replace(' ', '_')) == null
        })
        description(blank: true)
        writeProject(validator: { boolean value, GroupCommand object ->
            return object.readProject || !value
        })
        writeJobSystem(validator: { boolean value, GroupCommand object ->
            return object.readJobSystem || !value
        })
        writeSequenceCenter(validator: { boolean value, GroupCommand object ->
            return object.readSequenceCenter || !value
        })
    }
}
