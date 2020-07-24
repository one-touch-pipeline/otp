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
package de.dkfz.tbi.otp.project

import grails.gorm.transactions.Transactional
import grails.util.Pair
import grails.validation.ValidationException
import grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*

import java.time.LocalDate

@Transactional
class ProjectRequestService {
    @Autowired
    LinkGenerator linkGenerator
    LdapService ldapService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    @Autowired
    ProcessingOptionService processingOptionService
    SecurityService securityService
    UserProjectRoleService userProjectRoleService
    UserService userService

    List<ProjectRequest> getCreatedByUserAndResolved() {
        ProjectRequest.createCriteria().list {
            eq("requester", securityService.currentUserAsUser)
            ne("status", ProjectRequest.Status.WAITING_FOR_PI)
        } as List<ProjectRequest>
    }

    List<ProjectRequest> getResolvedWithUserAsPi() {
        ProjectRequest.createCriteria().list {
            eq("pi", securityService.currentUserAsUser)
            ne("status", ProjectRequest.Status.WAITING_FOR_PI)
        } as List<ProjectRequest>
    }

    List<ProjectRequest> getWaitingForCurrentUser() {
        ProjectRequest.findAllByPiAndStatus(securityService.currentUserAsUser, ProjectRequest.Status.WAITING_FOR_PI)
    }

    List<ProjectRequest> getUnresolvedRequestsOfUser() {
        ProjectRequest.findAllByRequesterAndStatus(securityService.currentUserAsUser, ProjectRequest.Status.WAITING_FOR_PI)
    }

    private List<User> findOrCreateUsers(List<String> usernames) {
        return usernames?.findAll()?.collect { userProjectRoleService.createUserWithLdapData(it) }
    }

    ProjectRequest create(ProjectRequestCreationCommand cmd) throws SwitchedUserDeniedException {
        securityService.ensureNotSwitchedUser()

        ProjectRequest req = new ProjectRequest(
                name                        : cmd.name,
                description                 : cmd.description,
                keywords                    : cmd.keywords as Set,
                organizationalUnit          : cmd.organizationalUnit,
                costCenter                  : cmd.costCenter,
                grantId                     : cmd.grantId,
                fundingBody                 : cmd.fundingBody,
                endDate                     : cmd.endDate,
                storageUntil                : resolveStoragePeriodToLocalDate(cmd.storagePeriod, cmd.storageUntil),
                relatedProjects             : cmd.relatedProjects,
                tumorEntity                 : cmd.tumorEntity,
                speciesWithStrain           : cmd.speciesWithStrain,
                customSpeciesWithStrain     : cmd.customSpeciesWithStrain,
                projectType                 : cmd.projectType,
                sequencingCenter            : cmd.sequencingCenter,
                approxNoOfSamples           : cmd.approxNoOfSamples,
                seqTypes                    : cmd.seqTypes,
                comments                    : cmd.comments,

                pi                          : findOrCreateUsers([cmd.pi]).first(),
                requester                   : securityService.currentUserAsUser,
                leadBioinformaticians       : findOrCreateUsers(cmd.leadBioinformaticians),
                bioinformaticians           : findOrCreateUsers(cmd.bioinformaticians),
                submitters                  : findOrCreateUsers(cmd.submitters),
        )
        req.save(flush: true)
        sendEmailOnCreation(req)
        return req
    }

    void edit(EditProjectRequestCommand cmd) {
        ProjectRequest projectRequest = cmd.request
        projectRequest.with {
            name                         = cmd.name
            description                  = cmd.description
            keywords                     = cmd.keywords as Set
            organizationalUnit           = cmd.organizationalUnit
            costCenter                   = cmd.costCenter
            grantId                      = cmd.grantId
            fundingBody                  = cmd.fundingBody
            endDate                      = cmd.endDate
            storageUntil                 = resolveStoragePeriodToLocalDate(cmd.storagePeriod, cmd.storageUntil)
            relatedProjects              = cmd.relatedProjects
            tumorEntity                  = cmd.tumorEntity
            speciesWithStrain            = cmd.speciesWithStrain
            customSpeciesWithStrain      = cmd.customSpeciesWithStrain
            projectType                  = cmd.projectType
            sequencingCenter             = cmd.sequencingCenter
            approxNoOfSamples            = cmd.approxNoOfSamples
            seqTypes                     = cmd.seqTypes
            comments                     = cmd.comments

            pi                           = findOrCreateUsers([cmd.pi]).first()
            leadBioinformaticians        = findOrCreateUsers(cmd.leadBioinformaticians)
            bioinformaticians            = findOrCreateUsers(cmd.bioinformaticians)
            submitters                   = findOrCreateUsers(cmd.submitters)
        }
        projectRequest.save(flush: true)
    }

    private static LocalDate resolveStoragePeriodToLocalDate(StoragePeriod storagePeriod, LocalDate given) {
        switch (storagePeriod) {
            case StoragePeriod.USER_DEFINED:
                return given
            case StoragePeriod.TEN_YEARS:
                return LocalDate.now().plusYears(10)
            case StoragePeriod.INFINITELY:
                return null
            default:
                return null
        }
    }

    ProjectRequest get(Long l) {
        ProjectRequest req = ProjectRequest.get(l)
        if (req && securityService.currentUserAsUser in [req.requester, req.pi]) {
            return req
        }
        return null
    }

    void assertApprovalEligible(ProjectRequest request) {
        if (!(securityService.currentUserAsUser == request.pi && request.status == ProjectRequest.Status.WAITING_FOR_PI)) {
            throw new AccessDeniedException("User '${securityService.currentUserAsUser}' not eligible to approve this request")
        }
    }

    void assertTermsAndConditions(boolean confirmConsent, boolean confirmRecordOfProcessingActivities) {
        if ([confirmConsent, confirmRecordOfProcessingActivities].any { !it }) {
            throw new OtpRuntimeException("Invalid state, conditions were not accepted")
        }
    }

    Errors approveRequest(ProjectRequest request, boolean confirmConsent, boolean confirmRecordOfProcessingActivities) throws SwitchedUserDeniedException {
        try {
            securityService.assertNotSwitchedUser()
            assertApprovalEligible(request)
            assertTermsAndConditions(confirmConsent, confirmRecordOfProcessingActivities)

            request.status = ProjectRequest.Status.APPROVED_BY_PI_WAITING_FOR_OPERATOR
            request.save(flush: true)
            sendEmailOnApproval(request)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    Errors denyRequest(ProjectRequest request) throws SwitchedUserDeniedException {
        try {
            securityService.assertNotSwitchedUser()
            assertApprovalEligible(request)

            request.status = ProjectRequest.Status.DENIED_BY_PI
            request.save(flush: true)
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

    boolean requesterIsEligibleToAccept(ProjectRequest projectRequest) {
        return projectRequest.requester == projectRequest.pi
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addUserRolesAndPermissions(ProjectRequest projectRequest) {
        Map<ProjectRequestRole, Set<User>> usersByRole = [
                (ProjectRequestRole.PI)                   : [projectRequest.pi] as Set<User>,
                (ProjectRequestRole.LEAD_BIOINFORMATICIAN): projectRequest.leadBioinformaticians,
                (ProjectRequestRole.BIOINFORMATICIAN)     : projectRequest.bioinformaticians,
                (ProjectRequestRole.SUBMITTER)            : projectRequest.submitters,
        ]

        Set<Pair<User, ProjectRequestRole>> pairSet = usersByRole.collectMany { ProjectRequestRole projectRequestRole, Set<User> users ->
            users.collect {
                return new Pair<User, ProjectRequestRole>(it, projectRequestRole)
            }
        }

        addUserAndRolesFromProjectRequest(pairSet, projectRequest.project)
    }

    private void addUserAndRolesFromProjectRequest(Set<Pair<User, ProjectRequestRole>> pairSet, Project project) {
        pairSet.groupBy { it.aValue }.each { User user, List<Pair<User, ProjectRequestRole>> projectRequestRoleList ->
            UserProjectRole upr = userProjectRoleService.createUserProjectRole(
                    user,
                    project,
                    ProjectRole.findAllByNameInList(projectRequestRoleList.collect { it.bValue.name() }) as Set<ProjectRole>,
            )

            List<ProjectRequestRole> permissionValueList = projectRequestRoleList*.bValue
            userProjectRoleService.with {
                setAccessToOtp(upr, permissionValueList*.accessToOtp.any())
                setAccessToFiles(upr, permissionValueList*.accessToFiles.any())
                setManageUsers(upr, permissionValueList*.manageUsers.any())
                setManageUsersAndDelegate(upr, permissionValueList*.manageUsersAndDelegate.any())
                setReceivesNotifications(upr, permissionValueList*.receivesNotifications.any())
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectRequest findProjectRequestByProject(Project project) {
        return CollectionUtils.atMostOneElement(ProjectRequest.findAllByProject(project))
    }

    private void sendEmailOnCreation(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectRequest",
                action: "view",
                absolute: true,
                id: request.id,
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest1", [
                requester  : request.requester.realName,
                projectName: request.name,
                pi         : request.pi.realName,
                link       : link,
        ])
        String title = messageSourceService.createMessage("notification.template.projectRequest.title", [
                projectName: request.name,
        ])
        List<String> ccs = [processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)]
        mailHelperService.sendEmail(title, message, request.pi.email, ccs)
    }

    protected void sendEmailOnApproval(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectCreation",
                action: "index",
                absolute: true,
                params: [
                        'projectRequest.id': request.id,
                ]
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest2", [
                requester  : request.requester.realName,
                projectName: request.name,
                pi         : request.pi.realName,
                link       : link,
        ])
        String title = messageSourceService.createMessage("notification.template.projectRequest.title", [
                projectName: request.name,
        ])
        mailHelperService.sendEmail(title, message, processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION))
    }
}
