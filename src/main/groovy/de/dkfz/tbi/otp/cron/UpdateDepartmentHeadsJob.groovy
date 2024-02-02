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
package de.dkfz.tbi.otp.cron

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.DepartmentCommand
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.security.user.DeputyRelationService
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.SystemUserUtils

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

/**
 * Scheduled job that Updates the Heads for each Department.
 */
@CompileDynamic
@Component
@Slf4j
class UpdateDepartmentHeadsJob extends AbstractScheduledJob {

    @Autowired
    UserService userService

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    DepartmentService departmentService

    @Autowired
    DeputyRelationService deputyRelationService

    @Override
    boolean isAdditionalRunConditionMet() {
        return [
                DEPARTMENT_WITH_ALL_INFO_SCRIPT,
                ORGANIZATIONAL_UNIT_KEY_NAME,
                COST_CENTER_KEY_NAME,
                DEPARTMENT_HEAD_KEY_NAME,
        ].every { processingOptionService.findOptionAsString(it) }
    }

    @Override
    void wrappedExecute() {
        String ouKey = processingOptionService.findOptionAsString(ORGANIZATIONAL_UNIT_KEY_NAME)
        String ccKey = processingOptionService.findOptionAsString(COST_CENTER_KEY_NAME)
        String dhKey = processingOptionService.findOptionAsString(DEPARTMENT_HEAD_KEY_NAME)
        List<DepartmentCommand> departmentCommands = []

        departmentInfo.each {
            JSONArray departmentHeadsUsernames = it[dhKey] ?: []
            List<User> departmentHeads = departmentHeadsUsernames.collect { departmentHeadUsername ->
                userService.findOrCreateUserWithLdapData(departmentHeadUsername as String)
            }
            departmentCommands << new DepartmentCommand(
                    ouNumber: it[ouKey],
                    costCenter: it[ccKey],
                    departmentHeads: departmentHeads
            )
        }

        if (!departmentCommands) {
            log.info("Could not find any Departments via CRONJob")
            return
        }

        SystemUserUtils.useSystemUser {
            departmentService.updateDepartments(departmentCommands)
            deputyRelationService.cleanUpDeputyRelations()
        }
    }

    protected JSONArray getDepartmentInfo() {
        String call = processingOptionService.findOptionAsString(DEPARTMENT_WITH_ALL_INFO_SCRIPT)
        ProcessOutput output = remoteShellHelper.executeCommandReturnProcessOutput(call).assertExitCodeZeroAndStderrEmpty()
        return convertJsonArrayStringToList(output.stdout)
    }

    static JSONArray convertJsonArrayStringToList(String jsonString) {
        if (!jsonString) {
            return new JSONArray([])
        }
        JSONArray jsonArray
        try {
            jsonArray = new JSONArray(jsonString)
        } catch (JSONException e) {
            throw new IllegalArgumentException("The string is no valid JSON string", e)
        }
        return jsonArray
    }
}
