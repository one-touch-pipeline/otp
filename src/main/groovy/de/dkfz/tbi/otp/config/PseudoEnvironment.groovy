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
package de.dkfz.tbi.otp.config

import grails.util.Environment
import groovy.transform.TupleConstructor

/**
 * This enum provides a loose extension to the actual {@link Environment} by providing more specific environments.
 *
 * The extended environments are PRODUCTION_TEST and WORKFLOW_TEST. Additionally each environment has environment
 * names and a favicon mapped to it.
 *
 * Use this enum sparingly and only if you really need to. Environment names should not be relied upon.
 */
@TupleConstructor
enum PseudoEnvironment {
    /** the actual production environment */
    PRODUCTION(["production"], "favicon.ico"),

    /** production-like environment, used for the test systems like beta */
    PRODUCTION_TEST(["alpha", "beta", "gamma"], "favicon_light_goldenrod.ico"),

    /** typical development environment */
    DEVELOPMENT(["development"], "favicon_pale_green.ico"),

    /** environment for unit- and integration tests */
    TEST(["test"], null),

    /** environment for workflow tests */
    WORKFLOW_TEST(["WORKFLOW_TEST"], null),

    final List<String> environmentNames
    final String ico

    static PseudoEnvironment resolveByEnvironmentName(String name) {
        return values().find {
            name in it.environmentNames
        }
    }

    String getIco() {
        return this.ico ?: "favicon_light_slate_blue.ico"
    }
}
