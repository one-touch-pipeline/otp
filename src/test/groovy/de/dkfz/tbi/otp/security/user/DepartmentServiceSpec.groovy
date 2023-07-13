/*
 * Copyright 2011-2023 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.AuditLogService
import de.dkfz.tbi.otp.security.Department
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

class DepartmentServiceSpec extends Specification implements DataTest, UserDomainFactory {

    DepartmentService departmentService

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                User,
                Department,
        ]
    }

    void setup() {
        departmentService = new DepartmentService(auditLogService: Mock(AuditLogService))
    }

    void "updateDepartments, should update existing department and create non existing department"() {
        given:
        Department department1 = createDepartment()

        User departmentHead1 = createUser()
        User departmentHead2 = createUser()
        User departmentHead3 = createUser()
        List<User> newDepartmentHeads = [departmentHead1, departmentHead2]
        List<User> createDepartmentHeads = [departmentHead2, departmentHead3]

        DepartmentCommand updateDepartment = [
                ouNumber       : department1.ouNumber,
                costCenter     : "new_cost_center",
                departmentHeads: newDepartmentHeads,
        ] as DepartmentCommand
        DepartmentCommand createDepartment = [
                ouNumber       : "random_number",
                costCenter     : "create_cost_center",
                departmentHeads: createDepartmentHeads,
        ] as DepartmentCommand

        when:
        departmentService.updateDepartments([updateDepartment, createDepartment])

        then:
        Department.count == 2

        department1.ouNumber == updateDepartment.ouNumber
        department1.costCenter == "new_cost_center"
        CollectionUtils.containSame(department1.departmentHeads, newDepartmentHeads)

        Department department2 = Department.findByOuNumber(createDepartment.ouNumber)
        department2.ouNumber == createDepartment.ouNumber
        department2.costCenter == "create_cost_center"
        CollectionUtils.containSame(department2.departmentHeads, createDepartmentHeads)
    }

    void "createDepartment, should throw error when department already exists"() {
        given:
        Department department = createDepartment()
        List<User> createDepartmentHead = [createUser(), createUser()]

        DepartmentCommand createDepartment = [
                ouNumber       : department.ouNumber,
                costCenter     : "create_cost_center",
                departmentHeads: createDepartmentHead,
        ] as DepartmentCommand

        when:
        departmentService.createDepartment(createDepartment)

        then:
        thrown(grails.validation.ValidationException)
    }

    void "createDepartment, should create another department"() {
        given:
        createDepartment()
        List<User> createDepartmentHead = [createUser(), createUser()]

        DepartmentCommand createDepartment = [
                ouNumber       : "new_ou_number",
                costCenter     : "create_cost_center",
                departmentHeads: createDepartmentHead,
        ] as DepartmentCommand

        when:
        departmentService.createDepartment(createDepartment)

        then:
        Department.count == 2
    }
}
