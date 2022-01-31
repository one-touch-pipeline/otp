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
package de.dkfz.tbi.otp.administration

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.job.scheduler.SchedulerService

class CrashRecoveryControllerSpec extends Specification implements ControllerUnitTest<CrashRecoveryController> {

    static final String IDS_SINGLE = '1'
    static final String IDS_MULTIPLE = '1,2,3'
    static final String IDS_WRONG_FORMAT = 'No Number value'
    static final String SOME_MESSSAGE = 'A message'
    static final String EXCEPTION_MESSAGE = 'An exceptionMessage'
    static final String PARAMETERS_FOR_SINGLE = '{"1!key1":"some value 1","1!key 2":"some value 2"}'
    static final String PARAMETERS_FOR_MULTIPLE = '{"1!key1":"some value 1","1!key 2":"some value 2", "2!key3":"some value 3","3!key 4":"some value 4"}'
    static final String PARAMETERS_FOR_TO_LESS_PARTS = '{"onePart":"some value 1"}'
    static final String PARAMETERS_FOR_TO_MUCH_PARTS = '{"more!then!two!parts":"some value 1"}'
    static final String PARAMETERS_FOR_FIRST_PART_IS_NOT_A_LONG = '{"noLong!key1":"some value 1"}'

    static final String SUCCESS_MESSAGE = '{"success":true,"error":null}'

    static final String ERROR_MESSAGE_FOR_MISSING_IDS = '{"success":false,"error":"No ids given. Expression: params.ids"}'
    static final String ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG = '{"success":false,"error":"For input string: \\"' + IDS_WRONG_FORMAT + '\\""}'

    static final String ERROR_MESSAGE_FOR_MISSING_MESSAGE = '{"success":false,"error":"No message given. Expression: params.message"}'

    static final String ERROR_MESSAGE_FOR_MISSING_PARAMETERS = '{"success":false,"error":"No parameters given. Expression: params.parameters"}'
    static final String ERROR_MESSAGE_FOR_INVALID_PARAMETERS = '{"success":false,"error":"Error parsing JSON"}'
    static final String ERROR_MESSAGE_FOR_PARAMETERS_FOR_UNKNOWN_IDS = '{"success":false,"error":"Cannot invoke method put() on null object"}'
    static final String ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_LESS_PARTS_IN_KEY = '{"success":false,"error":"Expect 2 parts, found 1 parts, value: [onePart]. Expression: (keys.length == 2)"}'
    static final String ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_MUCH_PARTS_IN_KEY = '{"success":false,"error":"Expect 2 parts, found 4 parts, value: [more, then, two, parts]. Expression: (keys.length == 2)"}'
    static final String ERROR_MESSAGE_FOR_PARAMETERS_WITH_FIRST_PART_IS_NOT_A_LONG = '{"success":false,"error":"For input string: \\"noLong\\""}'

    static final String ERROR_MESSAGE_EXCEPTION_THROWN = '{"success":false,"error":"' + EXCEPTION_MESSAGE + '"}'

    void setupTest() {
        params.ids = IDS_SINGLE
        params.message = SOME_MESSSAGE
        params.parameters = '{}'

        controller.crashRecoveryService = [
                markJobsAsFailed   : { List<Long> ids, String reason -> },
                restartJobs        : { List<Long> ids, String reason -> },
                markJobsAsSucceeded: { List<Long> ids, Map parameters -> },
                markJobsAsFinished : { List<Long> ids, Map parameters -> },
        ] as CrashRecoveryService
    }

    void test_markFailed_ShouldReturnOk_singleId() {
        given:
        setupTest()

        when:
        controller.markFailed()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markFailed_ShouldReturnOk_multipleIds() {
        given:
        setupTest()
        params.ids = IDS_MULTIPLE

        when:
        controller.markFailed()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markFailed_ShouldReturnErrorMessageForMissingMessage_MessageIsNull() {
        given:
        setupTest()
        params.message = null

        when:
        controller.markFailed()

        then:
        ERROR_MESSAGE_FOR_MISSING_MESSAGE == response.text
    }

    void test_markFailed_ShouldReturnErrorMessageForMissingIds_IdsIsNull() {
        given:
        setupTest()
        params.ids = null

        when:
        controller.markFailed()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_markFailed_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty() {
        given:
        setupTest()
        params.ids = ''

        when:
        controller.markFailed()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_markFailed_ShouldReturnErrorMessageForIdIsNotALong() {
        given:
        setupTest()
        params.ids = IDS_WRONG_FORMAT

        when:
        controller.markFailed()

        then:
        ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    void test_markFailed_ShouldReturnErrorMessageForServiceThrownException() {
        given:
        setupTest()
        controller.crashRecoveryService = [
                markJobsAsFailed: { List<Long> ids, String reason -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        when:
        controller.markFailed()

        then:
        ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }

    void test_restart_ShouldReturnOk_singleId() {
        given:
        setupTest()

        when:
        controller.restart()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_restart_ShouldReturnOk_multipleIds() {
        given:
        setupTest()
        params.ids = IDS_MULTIPLE

        when:
        controller.restart()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_restart_ShouldReturnErrorMessageForMissingMessage_MessageIsNull() {
        given:
        setupTest()
        params.message = null

        when:
        controller.restart()

        then:
        ERROR_MESSAGE_FOR_MISSING_MESSAGE == response.text
    }

    void test_restart_ShouldReturnErrorMessageForMissingIds_IdsIsNull() {
        given:
        setupTest()
        params.ids = null

        when:
        controller.restart()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_restart_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty() {
        given:
        setupTest()
        params.ids = ''

        when:
        controller.restart()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_restart_ShouldReturnErrorMessageForIdIsNotALong() {
        given:
        setupTest()
        params.ids = IDS_WRONG_FORMAT

        when:
        controller.restart()

        then:
        ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    void test_restart_ShouldReturnErrorMessageForServiceThrownException() {
        given:
        setupTest()
        controller.crashRecoveryService = [
                restartJobs: { List<Long> ids, String reason -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        when:
        controller.restart()

        then:
        ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }

    void test_markFinished_ShouldReturnOk_singleId_NoParameters() {
        given:
        setupTest()

        when:
        controller.markFinished()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markFinished_ShouldReturnOk_multipleIds_NoParameters() {
        given:
        setupTest()
        params.ids = IDS_MULTIPLE

        when:
        controller.markFinished()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markFinished_ShouldReturnOk_singleId_WithParameters() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_SINGLE

        when:
        controller.markFinished()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markFinished_ShouldReturnOk_multipleIds_WithParameters() {
        given:
        setupTest()
        params.ids = IDS_MULTIPLE
        params.parameters = PARAMETERS_FOR_MULTIPLE

        when:
        controller.markFinished()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForMissingIds_IdsIsNull_NoParameters() {
        given:
        setupTest()
        params.ids = null

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty_NoParameters() {
        given:
        setupTest()
        params.ids = ''

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForIdIsNotALong_NoParameters() {
        given:
        setupTest()
        params.ids = IDS_WRONG_FORMAT

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForServiceThrownException_NoParameters() {
        given:
        setupTest()
        controller.crashRecoveryService = [
                markJobsAsFinished: { List<Long> ids, Map parameters -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersIsNull() {
        given:
        setupTest()
        params.parameters = null

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_MISSING_PARAMETERS == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersContainsValuesForAdditionalProcessingStep() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_MULTIPLE

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_FOR_UNKNOWN_IDS == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToLessParts() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_TO_LESS_PARTS

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_LESS_PARTS_IN_KEY == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToManyParts() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_TO_MUCH_PARTS

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_MUCH_PARTS_IN_KEY == response.text
    }

    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_FirstPartIsNotALong() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_FIRST_PART_IS_NOT_A_LONG

        when:
        controller.markFinished()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_WITH_FIRST_PART_IS_NOT_A_LONG == response.text
    }

    void test_markSucceeded_ShouldReturnOk_singleId_NoParameters() {
        given:
        setupTest()

        when:
        controller.markSucceeded()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markSucceeded_ShouldReturnOk_multipleIds_NoParameters() {
        given:
        setupTest()
        params.ids = IDS_MULTIPLE

        when:
        controller.markSucceeded()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markSucceeded_ShouldReturnOk_singleId_WithParameters() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_SINGLE

        when:
        controller.markSucceeded()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markSucceeded_ShouldReturnOk_multipleIds_WithParameters() {
        given:
        setupTest()
        params.ids = IDS_MULTIPLE
        params.parameters = PARAMETERS_FOR_MULTIPLE

        when:
        controller.markSucceeded()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForMissingIds_IdsIsNull_NoParameters() {
        given:
        setupTest()
        params.ids = null

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty_NoParameters() {
        given:
        setupTest()
        params.ids = ''

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForIdIsNotALong_NoParameters() {
        given:
        setupTest()
        params.ids = IDS_WRONG_FORMAT

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForServiceThrownException_NoParameters() {
        given:
        setupTest()
        controller.crashRecoveryService = [
                markJobsAsSucceeded: { List<Long> ids, Map parameters -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersIsNull() {
        given:
        setupTest()
        params.parameters = null

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_MISSING_PARAMETERS == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersContainsValuesForAdditionalProcessingStep() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_MULTIPLE

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_FOR_UNKNOWN_IDS == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToLessParts() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_TO_LESS_PARTS

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_LESS_PARTS_IN_KEY == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToManyParts() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_TO_MUCH_PARTS

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_MUCH_PARTS_IN_KEY == response.text
    }

    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_FirstPartIsNotALong() {
        given:
        setupTest()
        params.parameters = PARAMETERS_FOR_FIRST_PART_IS_NOT_A_LONG

        when:
        controller.markSucceeded()

        then:
        ERROR_MESSAGE_FOR_PARAMETERS_WITH_FIRST_PART_IS_NOT_A_LONG == response.text
    }

    void test_startScheduler_ShouldReturnOk() {
        given:
        setupTest()
        final String SUCCESS_MESSAGE = '{"success":true}'
        boolean crashRecovery = true
        controller.crashRecoveryService = [
                isCrashRecovery: { -> return crashRecovery },
        ] as CrashRecoveryService
        controller.schedulerService = [
                startup: { -> crashRecovery = false }
        ] as SchedulerService

        when:
        controller.startScheduler()

        then:
        SUCCESS_MESSAGE == response.text
    }

    void test_startScheduler_ShouldReturnFailure() {
        given:
        setupTest()
        final String ERROR_MESSAGE = '{"success":false,"error":"Not in Crash Recovery"}'
        controller.crashRecoveryService = [
                isCrashRecovery: { -> return false },
        ] as CrashRecoveryService

        when:
        controller.startScheduler()

        then:
        ERROR_MESSAGE == response.text
    }

    void test_parametersOfJob_ShouldReturnModel() {
        given:
        setupTest()
        List modelDefiniation = [
                [
                        id       : 1,
                        jobName  : 'jobName1',
                        parameter: [],
                ],
        ]

        when:
        controller.crashRecoveryService = [
                getOutputParametersOfJobs: { List<Long> ids ->
                    [1] == ids
                    return modelDefiniation
                },
        ] as CrashRecoveryService

        then:
        [parametersPerJobs: modelDefiniation] == controller.parametersOfJob()
    }

    void test_parametersOfJob_ShouldFailForIdsIsNull() {
        given:
        setupTest()
        params.ids = null

        when:
        controller.parametersOfJob()

        then:
        thrown NullPointerException
    }

    void test_parametersOfJob_ShouldFailForIdsIsEmpty() {
        given:
        setupTest()
        params.ids = ''

        when:
        controller.parametersOfJob()

        then:
        thrown NumberFormatException
    }

    void test_parametersOfJob_ShouldFailForIdsIsNotALong() {
        given:
        setupTest()
        params.ids = IDS_WRONG_FORMAT

        when:
        controller.parametersOfJob()

        then:
        thrown NumberFormatException
    }
}
