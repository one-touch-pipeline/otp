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

import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.job.scheduler.SchedulerService

// ignored: will be removed with the old workflow system
@SuppressWarnings('ThrowRuntimeException')
@TestFor(CrashRecoveryController)
class CrashRecoveryControllerUnitTests {

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


    @Before
    void setUp() {
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


    @Test
    void test_markFailed_ShouldReturnOk_singleId() {
        controller.markFailed()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markFailed_ShouldReturnOk_multipleIds() {
        params.ids = IDS_MULTIPLE

        controller.markFailed()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markFailed_ShouldReturnErrorMessageForMissingMessage_MessageIsNull() {
        params.message = null

        controller.markFailed()
        assert ERROR_MESSAGE_FOR_MISSING_MESSAGE == response.text
    }

    @Test
    void test_markFailed_ShouldReturnErrorMessageForMissingIds_IdsIsNull() {
        params.ids = null

        controller.markFailed()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_markFailed_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty() {
        params.ids = ''

        controller.markFailed()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_markFailed_ShouldReturnErrorMessageForIdIsNotALong() {
        params.ids = IDS_WRONG_FORMAT

        controller.markFailed()
        assert ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    @Test
    void test_markFailed_ShouldReturnErrorMessageForServiceThrownException() {
        controller.crashRecoveryService = [
                markJobsAsFailed: { List<Long> ids, String reason -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        controller.markFailed()
        assert ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }


    @Test
    void test_restart_ShouldReturnOk_singleId() {
        controller.restart()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_restart_ShouldReturnOk_multipleIds() {
        params.ids = IDS_MULTIPLE

        controller.restart()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_restart_ShouldReturnErrorMessageForMissingMessage_MessageIsNull() {
        params.message = null

        controller.restart()
        assert ERROR_MESSAGE_FOR_MISSING_MESSAGE == response.text
    }

    @Test
    void test_restart_ShouldReturnErrorMessageForMissingIds_IdsIsNull() {
        params.ids = null

        controller.restart()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_restart_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty() {
        params.ids = ''

        controller.restart()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_restart_ShouldReturnErrorMessageForIdIsNotALong() {
        params.ids = IDS_WRONG_FORMAT

        controller.restart()
        assert ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    @Test
    void test_restart_ShouldReturnErrorMessageForServiceThrownException() {
        controller.crashRecoveryService = [
                restartJobs: { List<Long> ids, String reason -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        controller.restart()
        assert ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }


    @Test
    void test_markFinished_ShouldReturnOk_singleId_NoParameters() {
        controller.markFinished()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markFinished_ShouldReturnOk_multipleIds_NoParameters() {
        params.ids = IDS_MULTIPLE

        controller.markFinished()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markFinished_ShouldReturnOk_singleId_WithParameters() {
        params.parameters = PARAMETERS_FOR_SINGLE

        controller.markFinished()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markFinished_ShouldReturnOk_multipleIds_WithParameters() {
        params.ids = IDS_MULTIPLE
        params.parameters = PARAMETERS_FOR_MULTIPLE

        controller.markFinished()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForMissingIds_IdsIsNull_NoParameters() {
        params.ids = null

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty_NoParameters() {
        params.ids = ''

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForIdIsNotALong_NoParameters() {
        params.ids = IDS_WRONG_FORMAT

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForServiceThrownException_NoParameters() {
        controller.crashRecoveryService = [
                markJobsAsFinished: { List<Long> ids, Map parameters -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        controller.markFinished()
        assert ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersIsNull() {
        params.parameters = null

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_MISSING_PARAMETERS == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersContainsValuesForAdditionalProcessingStep() {
        params.parameters = PARAMETERS_FOR_MULTIPLE

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_PARAMETERS_FOR_UNKNOWN_IDS == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToLessParts() {
        params.parameters = PARAMETERS_FOR_TO_LESS_PARTS

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_LESS_PARTS_IN_KEY == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToManyParts() {
        params.parameters = PARAMETERS_FOR_TO_MUCH_PARTS

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_MUCH_PARTS_IN_KEY == response.text
    }

    @Test
    void test_markFinished_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_FirstPartIsNotALong() {
        params.parameters = PARAMETERS_FOR_FIRST_PART_IS_NOT_A_LONG

        controller.markFinished()
        assert ERROR_MESSAGE_FOR_PARAMETERS_WITH_FIRST_PART_IS_NOT_A_LONG == response.text
    }


    @Test
    void test_markSucceeded_ShouldReturnOk_singleId_NoParameters() {
        controller.markSucceeded()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnOk_multipleIds_NoParameters() {
        params.ids = IDS_MULTIPLE

        controller.markSucceeded()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnOk_singleId_WithParameters() {
        params.parameters = PARAMETERS_FOR_SINGLE

        controller.markSucceeded()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnOk_multipleIds_WithParameters() {
        params.ids = IDS_MULTIPLE
        params.parameters = PARAMETERS_FOR_MULTIPLE

        controller.markSucceeded()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForMissingIds_IdsIsNull_NoParameters() {
        params.ids = null

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForMissingIds_IdsIsEmpty_NoParameters() {
        params.ids = ''

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_MISSING_IDS == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForIdIsNotALong_NoParameters() {
        params.ids = IDS_WRONG_FORMAT

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_IDS_IS_NOT_A_LONG == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForServiceThrownException_NoParameters() {
        controller.crashRecoveryService = [
                markJobsAsSucceeded: { List<Long> ids, Map parameters -> throw new RuntimeException(EXCEPTION_MESSAGE) },
        ] as CrashRecoveryService

        controller.markSucceeded()
        assert ERROR_MESSAGE_EXCEPTION_THROWN == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersIsNull() {
        params.parameters = null

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_MISSING_PARAMETERS == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersContainsValuesForAdditionalProcessingStep() {
        params.parameters = PARAMETERS_FOR_MULTIPLE

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_PARAMETERS_FOR_UNKNOWN_IDS == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToLessParts() {
        params.parameters = PARAMETERS_FOR_TO_LESS_PARTS

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_LESS_PARTS_IN_KEY == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_ToManyParts() {
        params.parameters = PARAMETERS_FOR_TO_MUCH_PARTS

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_PARAMETERS_WITH_TO_MUCH_PARTS_IN_KEY == response.text
    }

    @Test
    void test_markSucceeded_ShouldReturnErrorMessageForInvalidParameters_ParametersHasInvalidKey_FirstPartIsNotALong() {
        params.parameters = PARAMETERS_FOR_FIRST_PART_IS_NOT_A_LONG

        controller.markSucceeded()
        assert ERROR_MESSAGE_FOR_PARAMETERS_WITH_FIRST_PART_IS_NOT_A_LONG == response.text
    }


    @Test
    void test_startScheduler_ShouldReturnOk() {
        final String SUCCESS_MESSAGE = '{"success":true}'
        boolean crashRecovery = true
        controller.crashRecoveryService = [
                isCrashRecovery: { -> return crashRecovery },
        ] as CrashRecoveryService
        controller.schedulerService = [
                startup: { -> crashRecovery = false }
        ] as SchedulerService

        controller.startScheduler()
        assert SUCCESS_MESSAGE == response.text
    }

    @Test
    void test_startScheduler_ShouldReturnFailure() {
        final String ERROR_MESSAGE = '{"success":false,"error":"Not in Crash Recovery"}'
        controller.crashRecoveryService = [
                isCrashRecovery: { -> return false },
        ] as CrashRecoveryService

        controller.startScheduler()
        assert ERROR_MESSAGE == response.text
    }


    @Test
    void test_parametersOfJob_ShouldReturnModel() {
        List modelDefiniation = [
                [
                        id       : 1,
                        jobName  : 'jobName1',
                        parameter: [],
                ],
        ]
        controller.crashRecoveryService = [
                getOutputParametersOfJobs: { List<Long> ids ->
                    assert [1] == ids
                    return modelDefiniation
                },
        ] as CrashRecoveryService

        def model = controller.parametersOfJob()
        assert [parametersPerJobs: modelDefiniation] == model
    }

    @Test
    void test_parametersOfJob_ShouldFailForIdsIsNull() {
        params.ids = null

        shouldFail(NullPointerException) { controller.parametersOfJob() }
    }

    @Test
    void test_parametersOfJob_ShouldFailForIdsIsEmpty() {
        params.ids = ''

        shouldFail(NumberFormatException) { controller.parametersOfJob() }
    }

    @Test
    void test_parametersOfJob_ShouldFailForIdsIsNotALong() {
        params.ids = IDS_WRONG_FORMAT

        shouldFail(NumberFormatException) { controller.parametersOfJob() }
    }
}
