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
package de.dkfz.tbi.otp.project

import grails.validation.ValidationException
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ngsdata.LdapUserCreationException
import de.dkfz.tbi.otp.security.Department
import de.dkfz.tbi.otp.security.DeputyRelation
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.security.user.DeputyRelationService
import de.dkfz.tbi.otp.utils.exceptions.RightsNotGrantedException

@PreAuthorize("hasPermission(null, 'IS_DEPARTMENT_HEAD')")
class DepartmentConfigurationController implements CheckAndCall {
    DeputyRelationService deputyRelationService
    DepartmentService departmentService
    SecurityService securityService

    static allowedMethods = [
            index    : "GET",
            addDeputy: "POST",
            removeDeputy: "POST",
    ]

    Map index() {
        User currentUser = securityService.currentUser
        List<DeputyRelation> deputyRelationList = deputyRelationService.getAllDeputyRelationsForDepartmentHead(currentUser)
        List<DepartmentDeputyDisplayDTO> departmentDeputyList = deputyRelationList.collect { deputyRelation ->
            return new DepartmentDeputyDisplayDTO(deputyRelation)
        }
        List<Department> departmentList = departmentService.getDepartmentsForDepartmentHead(currentUser)

        return [
                departmentList      : departmentList,
                departmentDeputyList: departmentDeputyList,
        ]
    }

    def addDeputy(String deputyUsername) {
        try {
            this.deputyRelationService.grantDepartmentDeputyRights(securityService.currentUser, deputyUsername)
        } catch (AssertionError | RightsNotGrantedException | LdapUserCreationException e) {
            log.error(e.message, e)
            flash.message = new FlashMessage(g.message(code: "departmentConfig.departmentDeputy.addDeputyFailed") as String, [e.message])
        } catch (ValidationException e) {
            log.error(e.message, e)
            flash.message = new FlashMessage(g.message(code: "departmentConfig.departmentDeputy.addDeputyFailed") as String, e.errors)
        }
        redirect(action: "index")
    }

    def removeDeputy(DeputyRelation deputyRelationToDelete) {
        try {
            this.deputyRelationService.revokeDeputyRelation(deputyRelationToDelete)
        } catch (AssertionError e) {
            log.error(e.message, e)
            flash.message = new FlashMessage(g.message(code: "departmentConfig.departmentDeputy.addDeputyFailed") as String, [e.message])
        }
        redirect(action: "index")
    }
}

class DepartmentDeputyDisplayDTO {
    String deputyUsername
    Date dateDeputyGranted
    Long deputyRelationId

    DepartmentDeputyDisplayDTO(DeputyRelation deputyRelation) {
        this.deputyUsername = deputyRelation.deputyUser.toString()
        this.dateDeputyGranted = deputyRelation.dateDeputyGranted
        this.deputyRelationId = deputyRelation.id
    }
}

