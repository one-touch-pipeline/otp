/*
 * Copyright 2011-2022 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.security.user

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.LdapUserCreationException
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * @short Service for User administration.
 *
 * This service is meant for any kind of user management, such as changing password
 * and administrative tasks like enabling/disabling users, etc.
 */
@CompileDynamic
@Transactional
class UserService {

    IdentityProvider identityProvider
    SecurityService securityService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void editUser(User user, String email, String realName) {
        updateEmail(user, email)
        updateRealName(user, realName)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<User> allUsers() {
        return User.list()
    }

    /**
     * returns otp users of a list of ad accounts.
     * It is needed for autocompletion, so it needs accessible for normal users.
     */
    Set<String> getAllUserNamesOfOtpUsers(List<String> usernames) {
        return usernames ? User.findAllByUsernameInList(usernames)*.username : []
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    int userCount() {
        return User.count()
    }

    User createUser(String username, String email, String realName) {
        User user = new User([
                username: username,
                email   : email,
                enabled : true,
                password: "*",
                realName: realName,
        ])
        user.save(flush: true)
        return user
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(null, 'ADD_USER')")
    User createUser(String username, String email, String realName, List<Role> roles) {
        User user = createUser(username, email, realName)
        roles.each { Role role ->
            addRoleToUser(user, role)
        }
        return user
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    User updateRealName(User user, String newName) {
        assert newName: "the input newName '${newName}' must not be null"
        assert user: "the input user must not be null"
        user.realName = newName
        assert user.save(flush: true)
        return user
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    User updateEmail(User user, String email) {
        assert email: "the input Email '${email}' must not be null"
        assert user: "the input user must not be null"
        user.email = email
        assert user.save(flush: true)
        return user
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    User enableUser(User user, boolean enable) {
        assert user: "the input user must not be null"
        user.enabled = enable
        assert user.save(flush: true)
        return user
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> allRoles() {
        return Role.findAllByAuthorityLike("ROLE_%")
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> allGroups() {
        return Role.findAllByAuthorityLike("GROUP_%")
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> getRolesOfUser(User user) {
        List<Role> roles = allRoles()
        return roles ? UserRole.findAllByUserAndRoleInList(user, roles)*.role : []
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> getGroupsOfUser(User user) {
        List<Role> groups = allGroups()
        return groups ? UserRole.findAllByUserAndRoleInList(user, groups)*.role : []
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addRoleToUser(User user, Role role) {
        assert user: "User can not be null"
        assert role: "Role can not be null"
        if (!CollectionUtils.atMostOneElement(UserRole.findAllByUserAndRole(user, role))) {
            UserRole.create(user, role)
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void removeRoleFromUser(User user, Role role) {
        assert user: "User can not be null"
        assert role: "Role can not be null"
        UserRole userRole = CollectionUtils.atMostOneElement(UserRole.findAllByUserAndRole(user, role))
        if (userRole) {
            userRole.delete(flush: true)
        }
    }

    boolean isDepartmentHead(User user) {
        return Department.createCriteria().list {
            departmentHeads {
                eq("id", user.id)
            }
            maxResults(1)
        } as boolean
    }

    boolean isPrivacyPolicyAccepted() {
        // HACK: skip in  case we are not logged in yet, otherwise we will never get to the log-in page...
        // (this _should_ probably be solved in a different layer of OTP)
        if (!securityService.loggedIn) {
            return true
        }
        // switched users need not be checked, because before switching, they must have been a user that must
        // have already accepted the policy (or else they couldn't have reached the "switch user" page in the first place)
        if (securityService.switched) {
            return true
        }

        User user = securityService.currentUser as User
        return user.acceptedPrivacyPolicy
    }

    void acceptPrivacyPolicy() {
        if (securityService.loggedIn) {
            User user = securityService.currentUser as User
            setAcceptPrivacyPolicy(user, true)
        }
    }

    void setAcceptPrivacyPolicy(User user, boolean flag) {
        user.acceptedPrivacyPolicy = flag
        user.save(flush: true)
    }

    void setPlannedDeactivationDateOfUser(User user, Date date) {
        user.plannedDeactivationDate = date
        user.save(flush: true)
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
                        username: currentUser,
                        email   : "admin@dummy.de",
                        enabled : true,
                        password: "*", // need for plugin, but unused in OTP
                        realName: currentUser,
                ]).save(flush: true)

                [Role.ROLE_ADMIN].each {
                    new UserRole([
                            user: user,
                            role: CollectionUtils.exactlyOneElement(Role.findAllByAuthority(it)),
                    ]).save(flush: true)
                }
            }
        }
    }

    User findOrCreateUserWithLdapData(String username) {
        IdpUserDetails idpUserDetails = identityProvider.getIdpUserDetailsByUsername(username)
        if (!idpUserDetails) {
            throw new LdapUserCreationException("'${username}' can not be resolved to a user via LDAP")
        }
        if (!idpUserDetails.mail) {
            throw new LdapUserCreationException("Could not get a mail for '${username}' via LDAP")
        }

        return CollectionUtils.atMostOneElement(User.findAllByUsername(idpUserDetails.username)) ?:
                createUser(idpUserDetails.username, idpUserDetails.mail, idpUserDetails.realName)
    }
}
