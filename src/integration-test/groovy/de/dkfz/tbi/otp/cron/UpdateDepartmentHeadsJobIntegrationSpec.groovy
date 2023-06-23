/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.cron

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.web.json.JSONArray
import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.security.Department
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.security.user.PIUserService
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.COST_CENTER_KEY_NAME
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.ORGANIZATIONAL_UNIT_KEY_NAME
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.DEPARTMENT_WITH_ALL_INFO_SCRIPT
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.DEPARTMENT_HEAD_KEY_NAME

@Rollback
@Integration
class UpdateDepartmentHeadsJobIntegrationSpec extends Specification implements CellRangerFactory, UserAndRoles, UserDomainFactory {

    private static final String OU = "organizational_unit"
    private static final String CC = "cost_center"
    private static final String DH = "department_head"
    private static final String INVALID_JSON = "[{]"
    private static final String DEPARTMENT_API = "bashScript"

    void setupData() {
        User systemUser = createUser()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, systemUser.username)
        findOrCreateProcessingOption(DEPARTMENT_WITH_ALL_INFO_SCRIPT, DEPARTMENT_API)
        findOrCreateProcessingOption(ORGANIZATIONAL_UNIT_KEY_NAME, OU)
        findOrCreateProcessingOption(COST_CENTER_KEY_NAME, CC)
        findOrCreateProcessingOption(DEPARTMENT_HEAD_KEY_NAME, DH)
    }

    void "getDepartmentInfo, valid api return value, returns JSONArray"() {
        given:
        setupData()
        List<Department> departments = [createDepartment(), createDepartment()]
        String validAPIResult = """[
${jsonLiteral(departments[0])}
${jsonLiteral(departments[1])}
]"""
        UpdateDepartmentHeadsJob job = createJob(validAPIResult)
        JSONArray departmentInfo = job.departmentInfo

        expect:
        departmentInfo.size() == 2
        departmentInfo.eachWithIndex { Map department, int i ->
            assert department[OU] == departments[i].ouNumber
            assert department[CC] == departments[i].costCenter
            assert department[DH] == departments[i].departmentHead.username
        }
    }

    void "getDepartmentInfo, invalid api return value, throws Exception"() {
        given:
        setupData()
        UpdateDepartmentHeadsJob job = createJob(INVALID_JSON)

        when:
        job.departmentInfo

        then:
        thrown(IllegalArgumentException)
    }

    void "wrappedExecute, keeps, removes, changes and adds department, succeeds"() {
        given:
        setupData()
        List<Department> departments = [createDepartment(), createDepartment(), createDepartment()]
        String costCenterForDep1 = "${nextId}"
        User departmentHeadForDep1 = createUser()
        String ouNumber = "OU${nextId}"
        String costCenter = "${nextId}"
        User departmentHead = createUser()
        String validDepartmentAPIResult = """[
${jsonLiteral(departments[0])}
{
"${OU}": "${departments[1].ouNumber}",
"${CC}": "${costCenterForDep1}",
"${DH}": "${departmentHeadForDep1.username}",
"key1": "${HelperUtils.uniqueString}",
"key2": "${HelperUtils.uniqueString}",
},
{
"${OU}": "${ouNumber}",
"${CC}": "${costCenter}",
"${DH}": "${departmentHead.username}",
"key1": "${HelperUtils.uniqueString}",
"key2": "${HelperUtils.uniqueString}",
},
]"""

        UpdateDepartmentHeadsJob job = createJob(validDepartmentAPIResult)
        job.userService = Mock(UserService) {
            1 * findOrCreateUserWithLdapData(departments[0].departmentHead.username) >> { departments[0].departmentHead }
            1 * findOrCreateUserWithLdapData(departmentHeadForDep1.username) >> { departmentHeadForDep1 }
            1 * findOrCreateUserWithLdapData(departmentHead.username) >> { departmentHead }
            0 * findOrCreateUserWithLdapData(_)
        }

        when:
        job.wrappedExecute()

        then:
        Department.all.size() == 3
        Department.findAllByOuNumberAndCostCenterAndDepartmentHead(departments[0].ouNumber, departments[0].costCenter, departments[0].departmentHead)
        Department.findAllByOuNumberAndCostCenterAndDepartmentHead(departments[1].ouNumber, costCenterForDep1, departmentHeadForDep1)
        Department.findAllByOuNumberAndCostCenterAndDepartmentHead(ouNumber, costCenter, departmentHead)
    }

    void "wrappedExecute, missing cost center, deleted"() {
        given:
        setupData()
        Department department = createDepartment()
        String validDepartmentAPIResult = """[
{
"${OU}": "${department.ouNumber}",
"${DH}": "${department.departmentHead.username}",
},
]"""

        UpdateDepartmentHeadsJob job = createJob(validDepartmentAPIResult)
        job.userService = Mock(UserService) {
            1 * findOrCreateUserWithLdapData(department.departmentHead.username) >> { department.departmentHead }
            0 * findOrCreateUserWithLdapData(_)
        }

        when:
        job.wrappedExecute()

        then:
        Department.all.size() == 0
    }

    void "wrappedExecute, missing department head, deleted"() {
        given:
        setupData()
        Department department = createDepartment()
        String validDepartmentAPIResult = """[
{
"${OU}": "${department.ouNumber}",
"${CC}": "${department.costCenter}",
},
]"""

        UpdateDepartmentHeadsJob job = createJob(validDepartmentAPIResult)
        job.userService = Mock(UserService) {
            1 * findOrCreateUserWithLdapData(null) >> { null }
            0 * findOrCreateUserWithLdapData(_)
        }

        when:
        job.wrappedExecute()

        then:
        Department.all.size() == 0
    }

    private static String jsonLiteral(Department department) {
        return """{
"${OU}": "${department.ouNumber}",
"${CC}": "${department.costCenter}",
"${DH}": "${department.departmentHead.username}",
"key1": "${HelperUtils.uniqueString}",
"key2": "${HelperUtils.uniqueString}",
},"""
    }

    private UpdateDepartmentHeadsJob createJob(String returnJSON) {
        return new UpdateDepartmentHeadsJob([
                processingOptionService: new ProcessingOptionService(),
                departmentService      : new DepartmentService(),
                remoteShellHelper      : Mock(RemoteShellHelper) {
                    1 * executeCommandReturnProcessOutput(_, _) >> { return new ProcessOutput(returnJSON, "", 0) }
                    0 * executeCommandReturnProcessOutput(_, _)
                },
                configService          : Mock(ConfigService) {
                    1 * getDefaultRealm() >> { createRealm() }
                },
                piUserService          : Mock(PIUserService),
        ])
    }
}
