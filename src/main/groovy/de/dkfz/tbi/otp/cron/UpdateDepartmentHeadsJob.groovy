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
package de.dkfz.tbi.otp.cron

import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.DepartmentCommand
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.security.user.PIUserService
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
    ConfigService configService

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    DepartmentService departmentService

    @Autowired
    PIUserService piUserService

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
            User departmentHead = userService.findOrCreateUserWithLdapData(it[dhKey])
            departmentCommands << new DepartmentCommand(
                    ouNumber: it[ouKey],
                    costCenter: it[ccKey],
                    departmentHead: departmentHead
            )
        }

        if (!departmentCommands) {
            log.info("Could not find any Departments via CRONJob")
            return
        }

        SystemUserUtils.useSystemUser {
            departmentService.updateDepartments(departmentCommands)
            piUserService.cleanUpPIUser()
        }
    }

    protected JSONArray getDepartmentInfo() {
        String call = processingOptionService.findOptionAsString(DEPARTMENT_WITH_ALL_INFO_SCRIPT)
        ProcessOutput output = remoteShellHelper.executeCommandReturnProcessOutput(configService.defaultRealm, call).assertExitCodeZeroAndStderrEmpty()
        return convertJsonArrayStringToList(output.stdout)
    }

    static JSONArray convertJsonArrayStringToList(String jsonString) {
        if (!jsonString) {
            return []
        }
        JSONArray jsonArray
        try {
            jsonArray = JSON.parse(jsonString) as JSONArray
        } catch (ConverterException e) {
            throw new IllegalArgumentException("The string is no valid JSON string", e)
        }
        return jsonArray
    }
}
