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
package de.dkfz.tbi.otp.utils.logging

import ch.qos.logback.core.PropertyDefinerBase
import grails.util.BuildSettings
import grails.util.Environment

class EnvironmentDefiner extends PropertyDefinerBase {
    private String key

    void setKey(String key) {
        this.key = key
    }

    @Override
    String getPropertyValue() {
        switch (key) {
            case "isDevelopmentMode":
                return current == Environment.DEVELOPMENT && BuildSettings.GRAILS_APP_DIR_PRESENT
            case "isProductionEnvironment":
                return current == Environment.PRODUCTION
            case "isWorkflowTestEnvironment":
                return current.name == "WORKFLOW_TEST"
        }
    }

    /**
     * In Grails 5, {@link Environment#getCurrent()} indirectly creates a log when reading application.yml. This causes an exception when configuring the
     * log system.
     * So the following code is copied from {@link Environment#getCurrent()}, with the call to Metadata.getCurrent() removed.
     */
    Environment getCurrent() {
        String envName = environmentInternal

        Environment env
        if (!isBlank(envName)) {
            env = Environment.getEnvironment(envName)
            if (env != null) {
                return env
            }
        }
        return resolveCurrentEnvironment()
    }

    private Environment resolveCurrentEnvironment() {
        String envName = environmentInternal

        if (isBlank(envName)) {
            return Environment.DEVELOPMENT
        }

        Environment env = Environment.getEnvironment(envName)
        if (env == null) {
            try {
                env = Environment.valueOf(envName.toUpperCase())
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        if (env == null) {
            env = Environment.CUSTOM
            env.name = envName
        }
        return env
    }

    private String getEnvironmentInternal() {
        String envName = System.getProperty(Environment.KEY)
        return isBlank(envName) ? System.getenv(Environment.ENV_KEY) : envName
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0
    }
}
