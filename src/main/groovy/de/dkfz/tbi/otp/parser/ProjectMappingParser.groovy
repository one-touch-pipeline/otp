/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.parser

import groovy.json.JsonException
import groovy.json.JsonSlurper

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

trait ProjectMappingParser {

    abstract ProcessingOptionService getProcessingOptionService()

    Map<String, String> projectPrefixToNameMap(ProcessingOption.OptionName optionName) {
        String option = processingOptionService.findOptionAsString(optionName)
        if (option?.trim()) {
            try {
                return new JsonSlurper().parseText(option) as Map<String, String>
            } catch (JsonException ignored) {
                return [:]
            }
        }
        return [:]
    }

    String mapProjectName(ProcessingOption.OptionName optionName, String projectPrefix) {
        Map<String, String> prefixToName = projectPrefixToNameMap(optionName)
        return prefixToName[projectPrefix]?.trim() ?: null
    }
}
