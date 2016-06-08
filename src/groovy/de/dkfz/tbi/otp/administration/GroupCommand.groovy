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
            if (Role.findByAuthority("GROUP_" + value.toUpperCase().replace(' ', '_'))) {
                return "Group already exists."
            }
        })
        description(blank: true)
        writeProject(validator: { boolean value, GroupCommand object ->
            if (!object.readProject && value) {
                return 'Write without read'
            }
        })
        writeJobSystem(validator: { boolean value, GroupCommand object ->
            if (!object.readJobSystem && value) {
                return 'Write without read'
            }
        })
        writeSequenceCenter(validator: { boolean value, GroupCommand object ->
            if (!object.readSequenceCenter && value) {
                return 'Write without read'
            }
        })
    }
}
