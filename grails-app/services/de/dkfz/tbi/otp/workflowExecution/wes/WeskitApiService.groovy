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
package de.dkfz.tbi.otp.workflowExecution.wes

import io.swagger.client.wes.api.WorkflowExecutionServiceApi

import de.dkfz.tbi.otp.config.ConfigService

/**
 * Helper service for {@link WeskitAccessService} to provide the {@link WorkflowExecutionServiceApi} to access WESkit.
 *
 * The service:
 * - creates the object
 * - configure the url to weskit
 * - request an access token for weskit
 *
 * The returned instance is ready to use.
 */
class WeskitApiService {

    ConfigService configService

    WeskitAuthService weskitAuthService

    /**
     * returns the {@link WorkflowExecutionServiceApi} to access WESKit.
     *
     * The instance is configured and has a valid token.
     *
     * Please note, that {@link WorkflowExecutionServiceApi} is not thread safe.
     */
    WorkflowExecutionServiceApi createApiInstance() {
        WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi()
        apiInstance.apiClient.basePath = configService.wesUrl
        apiInstance.apiClient.accessToken = weskitAuthService.requestWeskitAccessToken()

        return apiInstance
    }
}
