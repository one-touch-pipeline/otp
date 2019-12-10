/*
 * Copyright 2011-2019 The OTP authors
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

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.validation.ValidationException
import grails.web.mapping.LinkGenerator
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.LocalDate

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Transactional
class ProjectRequestService {
    @Autowired
    LinkGenerator linkGenerator
    LdapService ldapService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    @Autowired
    PluginAwareResourceBundleMessageSource messageSource
    ProcessingOptionService processingOptionService
    SpringSecurityService springSecurityService
    UserProjectRoleService userProjectRoleService
    UserService userService


    List<ProjectRequest> getCreatedAndApproved() {
        ProjectRequest.createCriteria().list {
            or {
                and {
                    eq("pi", springSecurityService.currentUser)
                    ne("status", ProjectRequest.Status.WAITING_FOR_PI)
                }
                eq("requester", springSecurityService.currentUser)
            }
        } as List<ProjectRequest>
    }

    List<ProjectRequest> getWaiting() {
        ProjectRequest.findAllByPiAndStatus(springSecurityService.currentUser as User, ProjectRequest.Status.WAITING_FOR_PI)
    }

    private List<User> findOrCreateUsers(List<String> usernames) {
        return usernames?.findAll()?.collect { userProjectRoleService.createUserWithLdapData(it) }
    }

    void create(ProjectRequestCreationCommand cmd) {
        ProjectRequest req = new ProjectRequest(
                name: cmd.name,
                description: cmd.description,
                keywords: cmd.keywords as Set,
                organizationalUnit: cmd.organizationalUnit,
                costCenter: cmd.costCenter,
                grantId: cmd.grantId,
                fundingBody: cmd.fundingBody,
                endDate: cmd.endDate,
                storageUntil: cmd.storagePeriod == StoragePeriod.USER_DEFINED ? cmd.storageUntil :
                        cmd.storagePeriod == StoragePeriod.TEN_YEARS ? LocalDate.now().plusYears(10) :
                                null,
                predecessorProject: cmd.predecessorProject,
                tumorEntity: cmd.tumorEntity,
                speciesWithStrain: cmd.speciesWithStrain,
                projectType: cmd.projectType,
                sequencingCenter: cmd.sequencingCenter,
                approxNoOfSamples: cmd.approxNoOfSamples,
                seqTypes: cmd.seqType,
                forceCopyFiles: cmd.forceCopyFiles,
                furtherDataProcessing: cmd.furtherDataProcessing,
                comments: cmd.comments,

                pi: findOrCreateUsers([cmd.pi]).first(),
                requester: springSecurityService.currentUser as User,
                deputyPis: findOrCreateUsers(cmd.deputyPis),
                responsibleBioinformaticians: findOrCreateUsers(cmd.responsibleBioinformaticians),
                bioinformaticians: findOrCreateUsers(cmd.bioinformaticians),
                submitters: findOrCreateUsers(cmd.submitters),
        )
        req.save(flush: true)
        sendEmailOnCreation(req)
    }

    ProjectRequest get(Long l) {
        ProjectRequest req = ProjectRequest.get(l)
        if (req && (springSecurityService.currentUser as User) in [req.requester, req.pi]) {
            return req
        }
        return null
    }

    Errors update(ProjectRequest request, ProjectRequest.Status status, boolean confirmConsent, boolean confirmRecordOfProcessingActivities) {
        try {
            if (springSecurityService.currentUser == request.pi && request.status == ProjectRequest.Status.WAITING_FOR_PI) {
                if (confirmConsent && confirmRecordOfProcessingActivities &&
                        status == ProjectRequest.Status.APPROVED_BY_PI_WAITING_FOR_OPERATOR) {
                    request.status = ProjectRequest.Status.APPROVED_BY_PI_WAITING_FOR_OPERATOR
                    request.save(flush: true)
                    sendEmailOnApproval(request)
                } else if (status == ProjectRequest.Status.DENIED_BY_PI) {
                    request.status = ProjectRequest.Status.DENIED_BY_PI
                    request.save(flush: true)
                } else {
                    throw new RuntimeException("Requested status is invalid")
                }
            } else {
                throw new AccessDeniedException("No access for this project request")
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void update(ProjectRequest request, Project project) {
        request.status = ProjectRequest.Status.PROJECT_CREATED
        request.project = project
        request.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addUserRolesAndPermissions(ProjectRequest projectRequest) {
        addUserAndRolesFromProjectRequest([projectRequest.pi], ProjectRequestRole.PI, projectRequest.project)
        addUserAndRolesFromProjectRequest(projectRequest.deputyPis, ProjectRequestRole.DEPUTY_PI, projectRequest.project)
        addUserAndRolesFromProjectRequest(projectRequest.responsibleBioinformaticians, ProjectRequestRole.RESPONSIBLE_BIOINFORMATICIAN, projectRequest.project)
        addUserAndRolesFromProjectRequest(projectRequest.bioinformaticians, ProjectRequestRole.BIOINFORMATICIAN, projectRequest.project)
        addUserAndRolesFromProjectRequest(projectRequest.submitters, ProjectRequestRole.SUBMITTER, projectRequest.project)
    }

    private addUserAndRolesFromProjectRequest(Collection<User> users, ProjectRequestRole role, Project project) {
        users.each { User user ->
            UserProjectRole upr = userProjectRoleService.createUserProjectRole(
                    user,
                    project,
                    exactlyOneElement(ProjectRole.findAllByName(role.name())),
            )
            userProjectRoleService.setAccessToOtp(upr, role.accessToOtp)
            userProjectRoleService.setAccessToFiles(upr, role.accessToFiles)
            userProjectRoleService.setManageUsers(upr, role.manageUsers)
            userProjectRoleService.setManageUsersAndDelegate(upr, role.manageUsersAndDelegate)
            userProjectRoleService.setReceivesNotifications(upr, role.receivesNotifications)
        }
    }

    private void sendEmailOnCreation(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectRequest",
                action: "view",
                absolute: true,
                id: request.id,
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest1", [
                requester: request.requester.realName,
                pi       : request.pi.realName,
                link     : link,
        ])
        String title = messageSource.getMessage("notification.template.projectRequest.title", [].toArray(), LocaleContextHolder.locale)
        mailHelperService.sendEmail(title, message, request.pi.email)
    }

    protected void sendEmailOnApproval(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectCreation",
                action: "index",
                absolute: true,
                params: [
                        'fromRequest.id': request.id,
                ]
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest2", [
                requester: request.requester.realName,
                pi       : request.pi.realName,
                link     : link,
        ])
        String title = messageSource.getMessage("notification.template.projectRequest.title", [].toArray(), LocaleContextHolder.locale)
        mailHelperService.sendEmail(title, message, processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION))
    }
}