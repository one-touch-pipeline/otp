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
package de.dkfz.tbi.otp.dataswap.parameters

import grails.validation.Validateable

import de.dkfz.tbi.otp.dataswap.Swap

import java.nio.file.Path

class DataSwapParameters implements Validateable {

    static Closure constraints = {
        projectNameSwap nullable: false, validator: {
            if (!it.old || !it.new) {
                return "neither the old nor the new project name may be null or blank"
            }
        }
        pidSwap nullable: false, validator: {
            if (!it.old || !it.new) {
                return "neither the old nor the new pid name may be null or blank"
            }
        }
        dataFileSwaps nullable: false, validator: {
            if (it*.old.any { !it }) {
                return "None of the old file names may be null or blank"
            }
        }
        bashScriptName nullable: false, blank: false
        log nullable: false
        scriptOutputDirectory nullable: false
    }

    Swap<String> projectNameSwap
    Swap<String> pidSwap
    List<Swap<String>> dataFileSwaps
    String bashScriptName
    StringBuilder log
    boolean failOnMissingFiles = false
    Path scriptOutputDirectory
    boolean linkedFilesVerified = false
}
