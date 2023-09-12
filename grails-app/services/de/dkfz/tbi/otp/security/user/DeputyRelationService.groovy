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

import de.dkfz.tbi.otp.security.AuditLog
import de.dkfz.tbi.otp.security.AuditLogService
import de.dkfz.tbi.otp.security.Department
import de.dkfz.tbi.otp.security.DeputyRelation
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.exceptions.RightsNotGrantedException

@Transactional
class DeputyRelationService {

    AuditLogService auditLogService
    UserService userService

    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void cleanUpDeputyRelations() {
        List<User> departmentHeads = Department.all*.departmentHeads.flatten().unique() as List<User>
        if (departmentHeads) {
            DeputyRelation.findAllByGrantingDeputyUserNotInList(departmentHeads)?.each { DeputyRelation deputyRelation ->
                revokeAllDeputiesForDepartmentHead(deputyRelation.grantingDeputyUser)
            }
        } else {
            DeputyRelation.all?.each { DeputyRelation deputyRelation ->
                revokeAllDeputiesForDepartmentHead(deputyRelation.grantingDeputyUser)
            }
        }
    }

    @CompileDynamic
    boolean hasDepartmentRights(User user) {
        return userService.isDepartmentHead(user) || !DeputyRelation.findAllByDeputyUser(user).isEmpty()
    }

    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    void grantDepartmentDeputyRights(User departmentHead, String departmentDeputyUsername) throws RightsNotGrantedException, AssertionError {
        assert departmentHead: "Department Head cannot be null"
        assert departmentDeputyUsername: "Department Deputy username cannot be null"
        if (userService.isDepartmentHead(departmentHead)) {
            User departmentDeputy = userService.findOrCreateUserWithLdapData(departmentDeputyUsername)
            new DeputyRelation([grantingDeputyUser: departmentHead, deputyUser: departmentDeputy, dateDeputyGranted: new Date()]).save(flush: true)
            String message = "${departmentDeputy} was granted deputy rights for department head ${departmentHead}"
            auditLogService.logAction(AuditLog.Action.GRANT_DEPUTY_PI_RIGHTS, message)
        } else {
            throw new RightsNotGrantedException("The deputy right has not been granted because the supplied department head is not the head of any department")
        }
    }

    /**
     * deletes records from {@link DeputyRelation} table if a department head revokes rights for a department deputy
     */
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#deputyRelation.grantingDeputyUser, 'IS_DEPARTMENT_HEAD')")
    void revokeDeputyRelation(DeputyRelation deputyRelation) {
        assert deputyRelation: "Deputy Relation cannot be null"
        String message = "Deputy relation rights have been revoked for ${deputyRelation.deputyUser} by department head ${deputyRelation.grantingDeputyUser}"
        auditLogService.logAction(AuditLog.Action.REVOKE_DEPARTMENT_DEPUTY_RIGHTS, message)
        deputyRelation.delete(flush: true)
    }

    /**
     * deletes all records that match the department deputy from {@link DeputyRelation} table
     */
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void revokeDepartmentDeputyRights(User departmentDeputy) {
        assert departmentDeputy: "Department Deputy cannot be null"
        String message = "All Department Deputy rights have been revoked for ${departmentDeputy}"
        auditLogService.logAction(AuditLog.Action.REVOKE_DEPARTMENT_DEPUTY_RIGHTS, message)
        DeputyRelation.findAllByDeputyUser(departmentDeputy)*.delete(flush: true)
    }

    /**
     *  deletes all records from {@link DeputyRelation} table if the department head is no longer the head of the department
     */
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    void revokeAllDeputiesForDepartmentHead(User departmentHead) {
        assert departmentHead: "Department Head cannot be null"
        DeputyRelation.findAllByGrantingDeputyUser(departmentHead).each {
            String message = "Department Deputy rights for ${it} have been revoked, since ${departmentHead} is no longer head of any department"
            auditLogService.logAction(AuditLog.Action.REVOKE_DEPARTMENT_DEPUTY_RIGHTS, message)
            it.delete(flush: true)
        }
    }

    /**
     * @returns all deputies that are registered for a department head
     */
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    List<DeputyRelation> getAllDeputiesForDepartmentHead(User departmentHead) {
        assert departmentHead: "Department Head cannot be null"
        return DeputyRelation.findAllByGrantingDeputyUser(departmentHead)
    }
}
