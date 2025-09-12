/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.parser.ilp

import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.util.regex.Matcher

@Component
class IlpParser implements SampleIdentifierParser {

    static final String PROJECT = /(?<project>(\w{4}))/
    static final String PID_PART = /\w{6,8}/
    static final String SAMPLE_TYPE = /(?<sampleType>(\w\d-\w\d-seq\d))/
    static final String PID = "(?<pid>(${PROJECT}-${PID_PART}))"
    static final String REGEX = "^${PID}-${SAMPLE_TYPE}\$"

    private final ProcessingOptionService processingOptionService

    IlpParser(final ProcessingOptionService processingOptionService) {
        this.processingOptionService = processingOptionService
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        if (!sampleIdentifier) {
            return null
        }
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher) {
            assert matcher.matches()

            String projectName = mapProjectName(matcher.group('project'))

            if (!projectName) {
                return null
            }

            return new DefaultParsedSampleIdentifier(
                    projectName,
                    matcher.group('pid'),
                    matcher.group('sampleType').toLowerCase(),
                    sampleIdentifier,
                    null,
            )
        }
        return null
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        return null
    }

    private String mapProjectName(String projectNumber) {
        String option = processingOptionService.findOptionAsString(ProcessingOption.OptionName.ILP_PARSER_MAPPING)
        String projectName
        if (option?.trim()) {
            try {
                Map<String, String> text = new JsonSlurper().parseText(option) as Map<String, String>
                projectName = text[projectNumber]
            } catch (JsonException ignored) {
                projectName = ""
            }
            return projectName?.trim() ?: null
        }
        return null
    }
}
