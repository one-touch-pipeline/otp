package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.administration.GroupCommand
import de.dkfz.tbi.otp.utils.Entity

/**
 * A Group defines a relationship between a group of users and
 * Access Control.
 *
 * The Group is linked with a given Role so the relationship to
 * the users is implied by the users having the given role. A
 * group role starts with "GROUP" instead of "ROLE".
 *
 * The Group defines how the ACL should be created when a new
 * domain object is created which is the base for ACL checks.
 * Therefore the Group has a number of fields which can be
 * audited when a new object is created to add the ACL for the
 * new objects.
 *
 * That said the Group only defines the ACL to create for new
 * objects but not for already existing objects. This needs to
 * be queried through the normal ACL mechanisms. Though when a
 * new Group is created the set information should also be used
 * for the existing data, but that is to be decided by the
 * service creating the domain object.
 */
class Group implements Entity {
    /**
     * The name of this Group. It is used as the base name for
     * the Role which is to be created by using: GROUP_name
     * with the name being uppercase and whitespaces replaced
     * by underscore.
     */
    String name
    /**
     * A description for this Group.
     * E.g. "all users who have read access to foo".
     */
    String description
    /**
     * The Role linked to this Group. Only one Group can be
     * assigned to a given Role.
     */
    Role role

    /**
     * Whether this Group gets read permission on each new created Project.
     */
    boolean readProject = false
    /**
     * Whether this Group gets write permission on each new created Project.
     * Requires to have read permission.
     * @see readProject
     */
    boolean writeProject = false
    /**
     * Whether this Group gets read permission on each new created JobExecutionPlan.
     */
    boolean readJobSystem = false
    /**
     * Whether this Group gets write permission on each new created JobExecutionPlan.
     * Requires to have read permission.
     * @see readJobSystem
     */
    boolean writeJobSystem = false
    /**
     * Whether this Group gets read permission on each new created SeqCenter.
     */
    boolean readSequenceCenter = false
    /**
     * Whether this Group gets write permission on each new created SeqCenter.
     * Requires to have read permission.
     * @see readSequenceCenter
     */
    boolean writeSequenceCenter = false

    static mapping = {
        table 'groups'
    }

    static constraints = {
        name(unique: true)
        role(unique: true, validator: { Role value, Group obj ->
            if (!value) {
                return false
            }
            if (!value.authority.startsWith("GROUP_")) {
                return false
            }
            return value.authority == "GROUP_" + obj.name.toUpperCase().replace(' ', '_')
        })
        description(blank: true)
        writeProject(validator: { boolean value, Group obj ->
            return obj.readProject || !value
        })
        writeJobSystem(validator: { boolean value, Group obj ->
            return obj.readJobSystem || !value
        })
        writeSequenceCenter(validator: { boolean value, Group obj ->
            return obj.readSequenceCenter || !value
        })
    }

    /**
     * Creates a Group from the given GroupCommand object.
     * The Group is not yet saved!
     * @param command The command object describing the Group
     * @param role The role for this Group
     * @return The created Group
     */
    static Group fromCommandObject(GroupCommand command, Role role) {
        Group group = new Group(name: command.name,
                                description: command.description,
                                role: role,
                                readProject: command.readProject,
                                writeProject: command.writeProject,
                                readJobSystem: command.readJobSystem,
                                writeJobSystem: command.writeJobSystem,
                                readSequenceCenter: command.readSequenceCenter,
                                writeSequenceCenter: command.writeSequenceCenter)
        return group
    }
}
