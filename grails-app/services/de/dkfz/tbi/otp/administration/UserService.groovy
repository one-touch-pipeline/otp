package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.user.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.*
import org.springframework.security.authentication.*

/**
 * @short Service for User administration.
 *
 * This service is meant for any kind of user management, such as changing password
 * and administrative tasks like enabling/disabling users, etc.
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void editUser(EditUserCommand cmd) {
        User origUser = cmd.user
        updateEmail(origUser, cmd.email)
        updateRealName(origUser, cmd.realName)
        updateAsperaAccount(origUser, cmd.asperaAccount)
    }

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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(null, 'ADD_USER')")
    User createUser(CreateUserCommand command) throws RegistrationException {
        User user = new User(username: command.username,
                             email: command.email,
                             enabled: true,
                             accountExpired: false,
                             accountLocked: false,
                             passwordExpired: false,
                             password: "*",
                             realName: command.realName,
                             asperaAccount: null)
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    User updateRealName(User user, String newName) {
        assert newName: "the input newName '${newName}' must not be null"
        assert user: "the input user must not be null"
        user.realName = newName
        assert user.save(flush: true, failOnError: true)
        return user
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    User updateEmail(User user, String email) {
        assert email: "the input Email '${email}' must not be null"
        assert user: "the input user must not be null"
        user.email = email
        assert user.save(flush: true, failOnError: true)
        return user
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    User updateAsperaAccount(User user, String aspera) {
        assert user: "the input user must not be null"
        user.asperaAccount = aspera
        assert user.save(flush: true, failOnError: true)
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

    boolean isPrivacyPolicyAccepted() {
        if (!springSecurityService.isLoggedIn()) {
            return true
        }
        if (SpringSecurityUtils.isSwitched()) {
            return true
        }
        User user = springSecurityService.getCurrentUser()
        return user.acceptedPrivacyPolicy
    }

    void acceptPrivacyPolicy() {
        if (springSecurityService.isLoggedIn()) {
            User user = springSecurityService.getCurrentUser()
            user.acceptedPrivacyPolicy = true
            user.save(flush: true)
        }
    }

    /**
     * In case that no user exists, a user with admin rights is created.
     *
     * The user name is taken from the property 'user.name'.
     *
     * If already a user exists, nothing is done.
     */
    static void createFirstAdminUserIfNoUserExists() {
        if (User.count == 0) {
            User.withTransaction {
                String currentUser = System.properties.getProperty('user.name')
                log.debug """\n\n
--------------------------------------------------------------------------------
No user exists yet, create user ${currentUser} with admin rights.
--------------------------------------------------------------------------------
\n"""
                User user = new User([
                        username       : currentUser,
                        email          : "admin@dummy.de",
                        enabled        : true,
                        accountExpired : false,
                        accountLocked  : false,
                        passwordExpired: false,
                        password       : "*", //need for plugin, but unused in OTP
                        realName       : currentUser,
                        asperaAccount  : null,
                ]).save(flush: true)

                [Role.ROLE_ADMIN].each {
                    new UserRole([
                            user: user,
                            role: CollectionUtils.exactlyOneElement(Role.findAllByAuthority(it))
                    ]).save(flush: true)
                }
            }
        }
    }
}
