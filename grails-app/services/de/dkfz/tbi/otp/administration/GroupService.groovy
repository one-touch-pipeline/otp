package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.model.Sid

/**
 * The GroupService is responsible for handling User Groups.
 *
 *
 */
class GroupService {
    /**
     * Dependency Injection of ACL util service
     */
    def aclUtilService

    /**
     * Retrieves the Group with the given Id.
     * @param id
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Group getGroup(long id) {
        return Group.get(id)
    }

    /**
     *
     * @return List of all available Groups
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Group> availableGroups() {
        return Group.list(sort: 'name')
    }

    /**
     * Retrieves all Groups the User is member of
     * @param user The user for whom the Groups should be retrieved
     * @return List of Groups the User is member of
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Group> groupsForUser(User user) {
        List<Group> groups = []
        UserRole.findAllByUser(user).collect { it.role }.each { Role role ->
            if (!role.authority.startsWith("GROUP_")) {
                return
            }
            Group group = Group.findByRole(role)
            if (group) {
                groups << group
            }
        }
        return groups
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Map<Group, List<User>> usersByGroup() {
        List<Group> groups = availableGroups()
        def groupUsers = [:]
        groups.each { Group group ->
            List users = []
            UserRole.findAllByRole(group.role).collect { it.user }.sort { it.username }.each { User user ->
                users.add(user)
            }
            groupUsers.put(group, users)
        }
        return groupUsers
    }

    /**
     * Creates a Group for the given GroupCommand if possible.
     * Together with the Group the corresponding Role is created and
     * the ACL is adjusted for the requested ACL settings.
     *
     * @param command The command object describing this Group
     * @return The created Group
     * @throws GroupCreationException In case error occurs during creation of Group
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Group createGroup(GroupCommand command) throws GroupCreationException {
        if (command.hasErrors()) {
            throw new GroupCreationException("Data does not validate")
        }
        Role role = new Role(authority: "GROUP_" + command.name.toUpperCase().replace(' ', '_'))
        if (!role.validate()) {
            throw new GroupCreationException("Group name is not unique")
        }
        if (!role.save()) {
            throw new GroupCreationException("Role for group could not be saved to database")
        }
        Group group = Group.fromCommandObject(command, role)
        if (!group.validate()) {
            throw new GroupCreationException("New Group does not validate")
        }
        if (!group.save()) {
            throw new GroupCreationException("Group could not be saved to database")
        }
        // create ACL definitions
        Sid sid = new GrantedAuthoritySid(role.authority)
        if (group.readProject) {
            grantPermissionToProjects(sid, BasePermission.READ)
            if (group.writeProject) {
                grantPermissionToProjects(sid, BasePermission.WRITE)
            }
        }
        if (group.readJobSystem) {
            grantPermissionToJobSystem(sid, BasePermission.READ)
            if (group.writeProject) {
                grantPermissionToJobSystem(sid, BasePermission.WRITE)
            }
        }
        if (group.readSequenceCenter) {
            grantPermissionToSequenceCenter(sid, BasePermission.READ)
            if (group.writeSequenceCenter) {
                grantPermissionToSequenceCenter(sid, BasePermission.WRITE)
            }
        }
        return group
    }

    /**
     * Adds the given User to the given Group.
     * Basically just a wrapper around creating the given user Role.
     * @param user The User who should be added to a Group
     * @param group The Group to which the User should be added
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addUserToGroup(User user, Group group) {
        if (UserRole.get(user.id, group.role.id)) {
            // user already part of group
            return
        }
        UserRole.create(user, group.role, true)
    }

    /**
     * Removes the given User from the given Group.
     * Basically just a wrapper around removing the given user Role.
     * @param user The User who should be removed from the Group
     * @param group The Group from which the User should be removed
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void removeUserFromGroup(User user, Group group) {
        UserRole.remove(user, group.role, true)
    }

    /**
     * Grants the given permission to the given role for all Projects.
     * @param sid
     * @param permission
     */
    private void grantPermissionToProjects(Sid sid, BasePermission permission) {
        Project.list().each { Project project ->
            aclUtilService.addPermission(project, sid, permission)
        }
    }

    /**
     * Grants the given permission to the given role for all JobExecutionPlans.
     * @param sid
     * @param permission
     */
    private void grantPermissionToJobSystem(Sid sid, BasePermission permission) {
        JobExecutionPlan.list().each { JobExecutionPlan plan ->
            aclUtilService.addPermission(plan, sid, permission)
        }
    }

    /**
     * Grants the given permission to the given role for all SeqCenter.
     * @param sid
     * @param permission
     */
    private void grantPermissionToSequenceCenter(Sid sid, BasePermission permission) {
        SeqCenter.list().each { SeqCenter center ->
            aclUtilService.addPermission(center, sid, permission)
        }
    }
}
