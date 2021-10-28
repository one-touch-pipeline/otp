/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.apache.commons.lang.WordUtils

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StringUtils

@Secured('isFullyAuthenticated()')
class ProjectUserController implements CheckAndCall {

    ProjectSelectionService projectSelectionService
    UserService userService
    UserProjectRoleService userProjectRoleService
    LdapService ldapService
    SpringSecurityService springSecurityService
    ProcessingOptionService processingOptionService
    ProjectRoleService projectRoleService

    static allowedMethods = [
            index                    : "GET",
            addUserToProject         : "POST",
            addRoleToUserProjectRole : "POST",
            deleteProjectRole        : "POST",
            setAccessToOtp           : "POST",
            setAccessToFiles         : "POST",
            setManageUsers           : "POST",
            setManageUsersAndDelegate: "POST",
            setReceivesNotifications : "POST",
            setEnabled               : "POST",
            updateName               : "POST",
            updateEmail              : "POST",
    ]

    def index() {
        Project project = projectSelectionService.selectedProject

        List<UserProjectRole> userProjectRolesOfProject = UserProjectRole.findAllByProject(project)
        List<User> projectUsers = userProjectRolesOfProject*.user

        String groupDistinguishedName = ldapService.getDistinguishedNameOfGroupByGroupName(project.unixGroup)
        List<String> ldapGroupMemberUsernames = ldapService.getGroupMembersByDistinguishedName(groupDistinguishedName)

        List<User> ldapGroupMemberUsers = ldapGroupMemberUsernames ? User.findAllByUsernameInList(ldapGroupMemberUsernames) : []
        projectUsers.addAll(ldapGroupMemberUsers)
        List<String> nonDatabaseUsers = ldapGroupMemberUsernames - ldapGroupMemberUsers*.username - processingOptionService.findOptionAsList(
                ProcessingOption.OptionName.GUI_IGNORE_UNREGISTERED_OTP_USERS_FOUND
        )

        projectUsers.unique()
        projectUsers.sort { it.username }

        List<UserEntry> userEntries = []
        List<String> usersWithoutUserProjectRole = []
        projectUsers.each { User user ->
            LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(user.username)
            UserProjectRole userProjectRole = userProjectRolesOfProject.find { it.user == user }

            // force IS state from ldap in case of inconsistency
            if (!((ldapUserDetails?.username ?: false) && userProjectRole?.accessToFiles) &&
                    (userProjectRole?.project?.unixGroup in ldapUserDetails?.memberOfGroupList) &&
                    !userProjectRole?.fileAccessChangeRequested) {
                userProjectRole = userProjectRoleService.setAccessToFiles(userProjectRole, true, true)
            } else if (((ldapUserDetails?.username ?: false) && userProjectRole?.accessToFiles) &&
                    !(userProjectRole?.project?.unixGroup in ldapUserDetails?.memberOfGroupList) &&
                    !userProjectRole?.fileAccessChangeRequested) {
                userProjectRole = userProjectRoleService.setAccessToFiles(userProjectRole, false, true)
            }

            if (userProjectRole) {
                userEntries.add(new UserEntry(user, project, ldapUserDetails, userService.hasCurrentUserAdministrativeRoles()))
            } else {
                usersWithoutUserProjectRole.add(user.username)
            }
        }
        Map<Boolean, List<UserEntry>> userEntriesByEnabledStatus = userEntries.groupBy { it.userProjectRole.enabled }

        return [
                projectsOfUnixGroup        : Project.findAllByUnixGroup(project.unixGroup),
                enabledProjectUsers        : userEntriesByEnabledStatus[true], // available Roles for each projectUsers in UserEntry
                disabledProjectUsers       : userEntriesByEnabledStatus[false], // available Roles for each projectUsers in UserEntry
                usersWithoutUserProjectRole: usersWithoutUserProjectRole,
                unknownUsersWithFileAccess : nonDatabaseUsers,
                // available Roles for adding a new user to a project. Caution: Not the available Roles for each projectUser.
                availableRoles             : projectRoleService.listAvailableProjectRolesAuthenticatedByCurrentUser(),
                hasErrors                  : params.hasErrors,
                message                    : params.message,
                emails                     : userProjectRoleService.getEmailsForNotification(project),
                currentUser                : springSecurityService.currentUser as User,
        ]
    }

    def addUserToProject(AddUserToProjectCommand cmd) {
        Project project = projectSelectionService.requestedProject
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("An error occurred", cmd.errors)
        } else {
            try {
                if (cmd.addViaLdap) {
                    userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                            project,
                            ProjectRole.findAllByNameInList(cmd.projectRoleNameList) as Set<ProjectRole>,
                            cmd.searchString,
                            [
                                    accessToOtp           : cmd.accessToOtp,
                                    accessToFiles         : cmd.accessToFiles,
                                    manageUsers           : cmd.manageUsers,
                                    manageUsersAndDelegate: cmd.manageUsersAndDelegate,
                                    receivesNotifications : cmd.receivesNotifications,
                            ]
                    )
                } else {
                    userProjectRoleService.addExternalUserToProject(
                            project,
                            cmd.realName,
                            cmd.email,
                            ProjectRole.findAllByNameInList(cmd.projectRoleNameList) as Set<ProjectRole>,
                    )
                }
                flash.message = new FlashMessage("Data stored successfully")
            } catch (LdapUserCreationException | AssertionError e) {
                flash.message = new FlashMessage("An error occurred", e.message)
            }
        }
        redirect(controller: "projectUser")
    }

    JSON addRoleToUserProjectRole(UpdateProjectRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            userProjectRoleService.addProjectRolesToProjectUserRole(cmd.userProjectRole, ProjectRole.findAllByNameInList(cmd.newRoles))
        }) {
            UserProjectRole currentUserProjectRole = CollectionUtils.exactlyOneElement(UserProjectRole.findAllById(cmd.userProjectRole.id))
            List<Project> projectsOfUnixGroup = Project.findAllByUnixGroup(currentUserProjectRole.project.unixGroup)
            List<String> currentProjectRoles = (currentUserProjectRole.projectRoles)*.name.sort()
            List<String> newProjectRolesNodes = []
            cmd.newRoles.each { String newUserRole ->
                newProjectRolesNodes.add(otp.editorSwitch([
                        template    : "remove",
                        link        : g.createLink([
                                controller: "projectUser",
                                action    : "deleteProjectRole",
                                params    : ['userProjectRole.id': currentUserProjectRole.id, 'currentRole': newUserRole]
                        ]),
                        value       : newUserRole,
                        name        : newUserRole,
                        confirmation: projectsOfUnixGroup.size() > 1 ? g.message(
                                code: "projectUser.sharedUnixGroupConfirmation",
                                args: [projectsOfUnixGroup.sort { it.name }.collect { "\n  - " + it }.join("")]
                        ) : ''
                ]) as String)
            }
            return [
                    currentProjectRole  : currentProjectRoles,
                    newProjectRolesNodes: newProjectRolesNodes,
            ]
        }
    }

    JSON deleteProjectRole(DeleteProjectRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            userProjectRoleService.deleteProjectUserRole(cmd.userProjectRole, CollectionUtils.exactlyOneElement(ProjectRole.findAllByName(cmd.currentRole)))
        }) {
            return [currentRole: cmd.currentRole]
        }
    }

    JSON setAccessToOtp(SetFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userProjectRoleService.setAccessToOtp(cmd.userProjectRole, cmd.value)
        }
    }

    JSON setAccessToFiles(SetFlagCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            userProjectRoleService.setAccessToFilesWithUserNotification(cmd.userProjectRole, cmd.value)
        }) {
            LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(cmd.userProjectRole.user.username)
            UserEntry userEntry = new UserEntry(cmd.userProjectRole.user, cmd.userProjectRole.project, ldapUserDetails)
            [tooltip: g.message(code: userEntry.fileAccess.toolTipKey), permissionState: userEntry.fileAccess]
        }
    }

    JSON setManageUsers(SetFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userProjectRoleService.setManageUsers(cmd.userProjectRole, cmd.value)
        }
    }

    JSON setManageUsersAndDelegate(SetFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userProjectRoleService.setManageUsersAndDelegate(cmd.userProjectRole, cmd.value)
        }
    }

    JSON setReceivesNotifications(SetFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userProjectRoleService.setReceivesNotifications(cmd.userProjectRole, cmd.value)
        }
    }

    JSON setEnabled(SetFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userProjectRoleService.setEnabled(cmd.userProjectRole, cmd.value)
        }
    }

    JSON updateName(UpdateUserRealNameCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userService.updateRealName(cmd.user, cmd.newName)
        }
    }

    JSON updateEmail(UpdateUserEmailCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            userService.updateEmail(cmd.user, cmd.newEmail)
        }
    }
}

@TupleConstructor
enum PermissionStatus {
    APPROVED("projectUser.table.fileAccess.approval.tooltip"),
    PENDING_APPROVAL("projectUser.table.fileAccess.pending.approval.tooltip"),
    PENDING_DENIAL("projectUser.table.fileAccess.pending.denial.tooltip"),
    DENIED("projectUser.table.fileAccess.denial.tooltip"),
    INCONSISTENCY_APPROVAL("projectUser.table.fileAccess.inconsistency.approval.tooltip"),
    INCONSISTENCY_DENIAL("projectUser.table.fileAccess.inconsistency.denial.tooltip"),

    final String toolTipKey

    @Override
    String toString() {
        return WordUtils.uncapitalize(WordUtils.capitalizeFully(this.name(), ['_'] as char[]).replaceAll("_", ""))
    }

    boolean toBoolean() {
        return this in [APPROVED, PENDING_APPROVAL, INCONSISTENCY_APPROVAL]
    }

    boolean isInconsistency() {
        return this in [INCONSISTENCY_APPROVAL, INCONSISTENCY_DENIAL]
    }
}

@Slf4j
class UserEntry {
    boolean inLdap
    User user
    String realName
    String thumbnailPhoto
    String department
    List<String> availableRoles
    List<String> projectRoleNames
    PermissionStatus otpAccess
    PermissionStatus fileAccess
    PermissionStatus manageUsers
    PermissionStatus manageUsersAndDelegate
    PermissionStatus receivesNotifications
    boolean deactivated
    UserProjectRole userProjectRole

    /**
     * Class to describe a userEntry in the projectUser view
     * @param hasAdministrativeRole is used to describe whether the user logged in has an administrative role. (caution: not ProjectRole)!
     */
    UserEntry(User user, Project project, LdapUserDetails ldapUserDetails, boolean hasAdministrativeRole = false) {
        this.user = user
        this.userProjectRole = CollectionUtils.exactlyOneElement(UserProjectRole.findAllByUserAndProject(user, project))

        this.inLdap = ldapUserDetails?.username ?: false
        this.realName = inLdap ? ldapUserDetails.realName : user.realName
        this.thumbnailPhoto = inLdap ? ldapUserDetails.thumbnailPhoto.encodeAsBase64() : ""
        this.department = inLdap ? ldapUserDetails.department : ""
        this.projectRoleNames = userProjectRole.projectRoles*.name.sort()
        // each UserEntry might have different Roles available dependent on the assigned Roles to the userProjectRole
        this.availableRoles = fetchAvailableRoles(userProjectRole, hasAdministrativeRole)
        this.deactivated = inLdap ? ldapUserDetails.deactivated : false

        this.otpAccess = getPermissionStatus(inLdap && userProjectRole.accessToOtp)
        this.fileAccess = getFilePermissionStatus(inLdap && userProjectRole.accessToFiles,
                project.unixGroup in ldapUserDetails?.memberOfGroupList, userProjectRole.fileAccessChangeRequested)
        this.manageUsers = getPermissionStatus(inLdap && userProjectRole.manageUsers)
        this.manageUsersAndDelegate = getPermissionStatus(inLdap && userProjectRole.manageUsersAndDelegate)
        this.receivesNotifications = getPermissionStatus(userProjectRole.receivesNotifications)
    }

    private static List<String> fetchAvailableRoles(UserProjectRole userProjectRole, boolean hasAdministrativeRole) {
        if (!hasAdministrativeRole) {
            return (ProjectRole.findAll() - userProjectRole.projectRoles - CollectionUtils.exactlyOneElement(
                    ProjectRole.findAllByName(ProjectRole.Basic.PI.name())
            ))*.name.sort()
        }
        return (ProjectRole.findAll() - userProjectRole.projectRoles)*.name.sort()
    }

    private static PermissionStatus getPermissionStatus(boolean access) {
        return access ? PermissionStatus.APPROVED : PermissionStatus.DENIED
    }

    private static PermissionStatus getFilePermissionStatus(boolean should, boolean is, boolean changeRequested) {
        if (should && is) {
            return PermissionStatus.APPROVED
        } else if (!should && !is) {
            return PermissionStatus.DENIED
        } else if (should && !is && changeRequested) {
            return PermissionStatus.PENDING_APPROVAL
        } else if (!should && is && changeRequested) {
            return PermissionStatus.PENDING_DENIAL
        }
        throw new OtpRuntimeException("Case should not occur: should: ${should}, is: ${is}, changeRequested: ${changeRequested}")
    }
}

class UpdateUserRealNameCommand implements Validateable {
    User user
    String newName

    static constraints = {
        newName(blank: false, validator: { val, obj ->
            if (val == obj.user?.realName) {
                return 'no.change'
            }
        })
    }

    void setValue(String value) {
        this.newName = StringUtils.trimAndShortenWhitespace(value)
    }
}

class UpdateUserEmailCommand implements Validateable {
    User user
    String newEmail

    static constraints = {
        newEmail(nullable: false, email: true, blank: false, validator: { val, obj ->
            if (val == obj.user?.email) {
                return 'no.change'
            } else if (!obj.user.username && User.findAllByEmailAndUsernameIsNullAndIdNotEqual(val, obj.user.id)) {
                return 'user.email.unique.extern'
            }
        })
    }

    void setValue(String value) {
        this.newEmail = StringUtils.trimAndShortenWhitespace(value)
    }
}

class SetFlagCommand implements Validateable {
    UserProjectRole userProjectRole
    boolean value

    void setValue(String value) {
        this.value = value.toBoolean()
    }
}

class UpdateProjectRoleCommand implements Validateable {
    UserProjectRole userProjectRole
    String currentRole
    List<String> newRoles

    void setValue(String value) {
        this.newRoles = JSON.parse(value) as List<String>
    }

    static constraints = {
        newRoles(nullable: false, validator: { val, obj ->
            (val.size() == 0) || val.any { !(it in ProjectRole.findAll()*.name) } ? "Input not a valid ProjectRole" : true
        })
    }
}

class DeleteProjectRoleCommand implements Validateable {
    UserProjectRole userProjectRole
    String currentRole

    static constraints = {
        userProjectRole(nullable: false)
        currentRole(nullable: false)
    }
}

class AddUserToProjectCommand implements Serializable {
    boolean addViaLdap = true

    String searchString
    List<String> projectRoleNameList
    boolean accessToOtp = true
    boolean accessToFiles = false
    boolean manageUsers = false
    boolean manageUsersAndDelegate = false
    boolean receivesNotifications = true

    String realName
    String email

    void setProjectRoleNames(String value) {
        this.projectRoleNameList = value.split(',')*.trim()
    }

    static constraints = {
        addViaLdap(blank: false)
        searchString(nullable: true, validator: { val, obj ->
            if (obj.addViaLdap && !obj.searchString?.trim()) {
                return "empty"
            }
        })
        projectRoleNameList(nullable: true, validator: { val, obj ->
            if (!obj.projectRoleNameList) {
                return "empty"
            }
        })
        accessToOtp(blank: false)
        accessToFiles(blank: false)
        manageUsers(blank: false)
        manageUsersAndDelegate(blank: false)
        receivesNotifications(blank: false)
        realName(nullable: true, validator: { val, obj ->
            if (!obj.addViaLdap && !obj.realName?.trim()) {
                return "empty"
            }
        })
        email(nullable: true, email: true, validator: { val, obj ->
            if (!obj.addViaLdap && !obj.email?.trim()) {
                return "empty"
            }
        })
    }

    void setAddViaLdap(String selectedMethod) {
        this.addViaLdap = (selectedMethod == 'true')
    }

    void setSearchString(String searchString) {
        this.searchString = StringUtils.trimAndShortenWhitespace(searchString)
    }

    void setRealName(String realName) {
        this.realName = StringUtils.trimAndShortenWhitespace(realName)
    }

    void setEmail(String email) {
        this.email = StringUtils.trimAndShortenWhitespace(email)
    }
}
