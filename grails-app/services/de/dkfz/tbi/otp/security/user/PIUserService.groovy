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
import de.dkfz.tbi.otp.security.PIUser
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.exceptions.RightsNotGrantedException

@Transactional
class PIUserService {

    AuditLogService auditLogService
    UserService userService

    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void cleanUpPIUser() {
        List<User> departmentHeads = Department.all*.departmentHeads.flatten().unique() as List<User>
        if (departmentHeads) {
            PIUser.findAllByPiNotInList(departmentHeads)?.each { PIUser piUser ->
                revokeDeputyPIRightsForHead(piUser.pi)
            }
        } else {
            PIUser.all?.each { PIUser piUser ->
                revokeDeputyPIRightsForHead(piUser.pi)
            }
        }
    }

    @CompileDynamic
    boolean isPI(User user) {
        return userService.isDepartmentHead(user) || !PIUser.findAllByDeputyPI(user).isEmpty()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    void grantDeputyPIRights(User departmentHead, User deputyPI) {
        assert departmentHead: "Department Head cannot be null"
        assert deputyPI: "Deputy PI cannot be null"
        if (userService.isDepartmentHead(departmentHead)) {
            new PIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()]).save(flush: true)
            String message = "${deputyPI} was granted deputy PI rights with department head as ${departmentHead}"
            auditLogService.logAction(AuditLog.Action.GRANT_DEPUTY_PI_RIGHTS, message)
        } else {
            throw new RightsNotGrantedException('The deputy PI was not granted rights because the supplied department head is not the head of any department')
        }
    }

    // deletes records from PIUser table if a department head revokes rights for a deputyPI
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    void revokeDeputyPIRights(User departmentHead, User deputyPI) {
        assert departmentHead: "Department Head cannot be null"
        assert deputyPI: "Deputy PI cannot be null"
        String message = "Deputy PI rights have been revoked for ${deputyPI} by department head ${departmentHead}"
        auditLogService.logAction(AuditLog.Action.REVOKE_DEPUTY_PI_RIGHTS, message)
        PIUser.findAllByPiAndDeputyPI(departmentHead, deputyPI)*.delete(flush: true)
    }

    // deletes all records that match the deputy PI from PIUser table
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void revokeDeputyPIRights(User deputyPI) {
        assert deputyPI: "Deputy PI cannot be null"
        String message = "Deputy PI rights(All) have been revoked for ${deputyPI}"
        auditLogService.logAction(AuditLog.Action.REVOKE_DEPUTY_PI_RIGHTS, message)
        PIUser.findAllByDeputyPI(deputyPI)*.delete(flush: true)
    }

    // deletes all records from PIUser table if the department head is no longer the head of the department
    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    void revokeDeputyPIRightsForHead(User departmentHead) {
        assert departmentHead: "Department Head cannot be null"
        PIUser.findAllByPi(departmentHead)*.delete(flush: true)
    }
}
