package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import de.dkfz.tbi.otp.user.*
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.transaction.TransactionStatus

/**
 * @short Service for User administration.
 *
 * This service is meant for any kind of user management, such as changing password
 * and administrative tasks like enabling/disabling users, etc.
 * 
 */
class UserService {

    static transactional = true
    /**
     * Dependency injection of springSecurityService
     */
    def springSecurityService
    /**
     * Dependency injection of mail Service provided by the Mail plugin
     */
    def mailService
    /**
     * Dependency injection for Group Service
     */
    def groupService
    /**
     * Dependency injection of grails Application
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    /**
     * Random number generator for creating user validation ids.
     */
    private final Random random = new Random(System.currentTimeMillis())

    void changePassword(String oldPassword, String newPassword) throws BadCredentialsException {
        User user = (User)springSecurityService.getCurrentUser()
        if (user.password != springSecurityService.encodePassword(oldPassword, null)) {
            throw new BadCredentialsException("Cannot change password, old password is incorrect")
        }
        // TODO: verify password strength?
        user.password = springSecurityService.encodePassword(newPassword, null)
        user.passwordExpired = false
        user.save()
        springSecurityService.reauthenticate(user.username, newPassword)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or authentication.name==#user.username")
    void editUser(User user) throws UserInvalidException {
        User origUser = User.findByUsername(user.username)
        origUser.email = user.email
        if (!origUser.validate()) {
            throw new UserInvalidException(user.username)
        }
        origUser.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    User getCurrentUser() {
        return User.findByUsername(springSecurityService.authentication.principal.username as String)?.sanitizedUser()
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or authentication.name==#username")
    User getUser(String username) throws UserNotFoundException {
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        return user.sanitizedUser()
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    User getUser(Long id) throws UserNotFoundException {
        User user = User.get(id)
        if (!user) {
            throw new UserNotFoundException(id)
        }
        return user
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<User> getAllUsers() {
        return User.list()
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    int getUserCount() {
        return User.count()
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    User createUser(CreateUserCommand command) throws RegistrationException {
        User user = new User(username: command.username,
                             email: command.email,
                             enabled: true,
                             accountExpired: false,
                             accountLocked: false,
                             passwordExpired: false,
                             password: "*")
        if (!user.validate()) {
            throw new RegistrationException(command.username)
        }
        user = user.save()
        command.role.each {
            addRoleToUser(user.id, it)
        }
        command.group.each {
            Group group = groupService.getGroup(it)
            groupService.addUserToGroup(user, group)
        }
        return user
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean enableUser(Long userId, Boolean enable) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.enabled != enable) {
            user.enabled = enable
            user.save(flush: true)
            return (User.get(userId).enabled == enable)
        } else {
            return false
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean lockAccount(Long userId, Boolean lock) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.accountLocked != lock) {
            user.accountLocked = lock
            user.save(flush: true)
            return (User.get(userId).accountLocked == lock)
        } else {
            return false
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean expireAccount(Long userId, Boolean expire) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.accountExpired != expire) {
            user.accountExpired = expire
            user.save(flush: true)
            return (User.get(userId).accountExpired == expire)
        } else {
            return false
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean expirePassword(Long userId, Boolean expire) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.passwordExpired != expire) {
            user.passwordExpired = expire
            user.save(flush: true)
            return (User.get(userId).passwordExpired == expire)
        } else {
            return false
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> getAllRoles() {
        return Role.listOrderById().findAll { !it.authority.startsWith("GROUP_") }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> getRolesForUser(Long id) {
        return Role.executeQuery("SELECT role FROM UserRole AS userRole JOIN userRole.role AS role JOIN userRole.user AS user WHERE user.id=:id ORDER BY role.id", [id: id]).findAll { !it.authority.startsWith("GROUP_") }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addRoleToUser(Long userId, Long roleId) throws UserNotFoundException, RoleNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        Role role = Role.get(roleId)
        if (!role) {
            throw new RoleNotFoundException(roleId)
        }
        if (!UserRole.get(userId, roleId)) {
            UserRole.create(user, role, true)
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void removeRoleFromUser(Long userId, Long roleId) throws UserNotFoundException, RoleNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        Role role = Role.get(roleId)
        if (!role) {
            throw new RoleNotFoundException(roleId)
        }
        UserRole.remove(user, role, true)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Role getRoleByAuthority(String authority) throws RoleNotFoundException {
        Role role = Role.findByAuthority(authority)
        if (!role) {
            throw new RoleNotFoundException(authority)
        }
        return role
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    User getNextUser(User user) {
        return User.findByIdGreaterThan(user.id, [sort: "id", oder: "asc"])
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    User getPreviousUser(User user) {
        return User.findByIdLessThan(user.id, [sort: "id", order: "desc"])
    }

    /**
     * Retrieves all the Users a User with ROLE_SWITCH_USER is allowed to switch to.
     *
     * That is it filters out admin and operators.
     * @return List of Users the current user is allowed to switch to.
     */
    @PreAuthorize("hasRole('ROLE_SWITCH_USER')")
    List<User> switchableUsers() {
        String query = '''
SELECT u FROM UserRole AS ur
JOIN ur.user AS u
JOIN ur.role AS r
WHERE
r.authority in ('ROLE_ADMIN', 'ROLE_OPERATOR')
'''
        List<User> adminUsers = User.executeQuery(query)
        query = '''
SELECT u FROM User AS u
WHERE
u.id not in (:ids)
'''
        return User.executeQuery(query, [ids: adminUsers.collect { it.id } ])
    }

    /**
     * Resolves whether the current user can switch to the User identified by the given id.
     *
     * This method does not allow to switch to a user with ROLE_ADMIN or ROLE_OPERATOR, but
     * the framework in general allows to switch to such users. An admin user can just use the
     * Useradministration views to switch to any given user and a user with ROLE_SWITCH_USER could
     * bypass the protection provided by the SwitchUserController by using the spring security URL
     * directly. To a certain degree this is security by obscurity.
     * @param id The Id of the User to switch to
     * @return The username in case the User exists and is not an admin or operator. Null in that case.
     */
    @PreAuthorize("hasRole('ROLE_SWITCH_USER')")
    String switchableUser(Long id) {
        User user = User.get(id)
        if (!user) {
            return null
        }
        List<Role> userRoles = getRolesForUser(id)
        for (Role role in userRoles) {
            if (role.authority == 'ROLE_ADMIN') {
                return null
            }
            if (role.authority == 'ROLE_OPERATOR') {
                return null
            }
        }
        return user.username
    }

    boolean persistAdminWithRoles(User person) {
        boolean ok = true
        User.withTransaction { TransactionStatus status ->
            if (!person.save()) {
                ok = false
                status.setRollbackOnly()
            }
            if (!createRolesForAdmin(person)) {
                ok = false
                status.setRollbackOnly()
            }
        }
        return ok
    }

    boolean createRolesForAdmin(User user) {
        Role adminRole = new Role(authority: "ROLE_ADMIN")
        if (!adminRole.save(flush: true)) {
            return false
        }
        addRoleToUser(user.id, adminRole.id)
        Role userRole = new Role(authority: "ROLE_USER")
        if (!userRole.save(flush: true)) {
            return false
        }
        addRoleToUser(user.id, userRole.id)
        return true
    }
}
