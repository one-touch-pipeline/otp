/*
 * Copyright 2011-2024 The OTP authors
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
import grails.validation.Validateable
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.security.AuditLog
import de.dkfz.tbi.otp.security.AuditLogService
import de.dkfz.tbi.otp.security.Department
import de.dkfz.tbi.otp.security.DeputyRelation
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class DepartmentService {

    AuditLogService auditLogService

    @CompileDynamic
    Map<String, String> getListOfPIForDepartment(String department) {
        List<User> departmentHeads = getListOfHeadsForDepartment(department)
        Map<String, String> result = [:]
        departmentHeads.each { User head ->
            result << [(head.username): head.realName]
            List<User> deputies = DeputyRelation.findAllByGrantingDeputyUser(head)*.deputyUser
            deputies.each { User deputy ->
                result << [(deputy.username): deputy.realName]
            }
        }
        return result.sort { a, b -> a.value <=> b.value }
    }

    @CompileDynamic
    Map<String, String> listOfAllUsers() {
        Map<String, String> result = [:]
        User.findAllByEnabled(true).each {
            result << [(it.username): it.realName]
        }
        return result.sort { a, b -> a.value <=> b.value }
    }

    @CompileDynamic
    List<User> getListOfHeadsForDepartment(String department) {
        List<User> departmentHeads = CollectionUtils.atMostOneElement(Department.findAllByOuNumber(department))?.departmentHeads as List<User> ?: []
        return departmentHeads.unique()
    }

    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateDepartments(List<DepartmentCommand> departmentCommands) {
        Department.all.each { Department department ->
            DepartmentCommand departmentCommand = departmentCommands.find { it.ouNumber == department.ouNumber }
            updateDepartment(department, departmentCommand)
            departmentCommands.remove(departmentCommand)
        }
        departmentCommands.each { DepartmentCommand departmentCommand ->
            createDepartment(departmentCommand)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    private void updateDepartment(Department department, DepartmentCommand departmentCommand) {
        if (!departmentCommand || !departmentCommand.departmentHeads || !departmentCommand.costCenter) {
            auditLogService.logAction(AuditLog.Action.DELETED_DEPARTMENT, "Deleted Department: ${department}")
            log.info("Deleted Department: ${department}")
            department.delete(flush: true)
            return
        }
        if (departmentCommand.equalDepartment(department)) {
            log.info("Kept Department: ${department}")
        } else {
            String logOutput = "Updated Department from: ${department}"
            department.costCenter = departmentCommand.costCenter
            department.departmentHeads = departmentCommand.departmentHeads
            auditLogService.logAction(AuditLog.Action.UPDATED_DEPARTMENT, "Updated Department: ${department}")
            log.info("${logOutput} to: ${department}")
            department.save(flush: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    void createDepartment(DepartmentCommand departmentCommand) {
        if (!departmentCommand || !departmentCommand.departmentHeads || !departmentCommand.costCenter) {
            log.info("Could not create ${departmentCommand}.")
            return
        }
        Department department = new Department(
                ouNumber: departmentCommand.ouNumber,
                costCenter: departmentCommand.costCenter,
                departmentHeads: departmentCommand.departmentHeads,
        )
        auditLogService.logAction(AuditLog.Action.CREATED_DEPARTMENT, "Created Department: ${department}")
        log.info("Created Department: ${department}")
        department.save(flush: true)
    }

    @CompileDynamic
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#departmentHead, 'IS_DEPARTMENT_HEAD')")
    @CompileDynamic
    List<Department> getDepartmentsForDepartmentHead(User departmentHead) {
        assert departmentHead: "Department Head has to be not null"
        return Department.createCriteria().list {
            departmentHeads {
                idEq(departmentHead.id)
            }
        } as List<Department>
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Department> listDepartments() {
        return Department.all
    }
}

class DepartmentCommand implements Validateable {
    String ouNumber
    String costCenter
    Set<User> departmentHeads

    boolean equalDepartment(Department department) {
        return department.ouNumber == ouNumber &&
                department.costCenter == costCenter &&
                department.departmentHeads == departmentHeads
    }

    @Override
    String toString() {
        return "${ouNumber} ${costCenter} -> [${departmentHeads*.realName.join(', ')}]"
    }
}
