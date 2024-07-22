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
package de.dkfz.tbi.otp.job.processing

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONElement
import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.SeqType

@Transactional
class ClusterJobSubmissionOptionsService {

    ProcessingOptionService processingOptionService

    /**
     * Method to read options for job submission to the cluster job scheduler from the database.
     * Options are read in the following order: from job-specific processing option,
     * from job- and seqType-specific processing option; if an options is specified more than once,
     * the last value is used.
     *
     * Options are Map<JobSubmissionOption, String> serialized as JSON
     */
    @Deprecated // old workflow system
    Map<JobSubmissionOption, String> readOptionsFromDatabase(ProcessingStep processingStep) {
        ProcessParameterObject parameterObject = processingStep.processParameterObject
        assert parameterObject
        SeqType seqType = parameterObject.seqType
        String jobClass = processingStep.nonQualifiedJobClass

        Map<JobSubmissionOption, String> options = [:]

        // options for the job class
        options.putAll(convertJsonObjectStringToMap(
                processingOptionService.findOptionAsString(OptionName.CLUSTER_SUBMISSIONS_OPTION, "${jobClass}"))
        )

        if (seqType) {
            assert seqType.processingOptionName
            // options for the job class and SeqType
            options.putAll(convertJsonObjectStringToMap(
                    processingOptionService.findOptionAsString(OptionName.CLUSTER_SUBMISSIONS_OPTION, "${jobClass}_${seqType.processingOptionName}"))
            )
        }

        return options
    }

    static Map<JobSubmissionOption, String> convertJsonObjectStringToMap(String jsonString) {
        if (!jsonString) {
            return [:]
        }
        JSONElement jsonElement
        try {
            jsonElement = JSON.parse(jsonString)
        } catch (ConverterException e) {
            throw new IllegalArgumentException("The string is no valid JSON string", e)
        }
        return convertJsonObjectStringToMap(jsonElement)
    }

    @CompileDynamic
    static Map<JobSubmissionOption, String> convertJsonObjectStringToMap(JSONElement jsonElement) {
        if (!(jsonElement instanceof JSONObject)) {
            throw new IllegalArgumentException("The JSON string doesn't contain a map")
        }
        if (jsonElement.isEmpty()) {
            throw new IllegalArgumentException("The JSON map is empty")
        }
        jsonElement.each { k, v ->
            if (!(v instanceof String)) {
                throw new IllegalArgumentException("Values of map are not strings")
            }
        }
        return jsonElement.collectEntries {
            k, v -> [(JobSubmissionOption.valueOf(k as String)): v]
        }
    }

    static String validateJsonObjectString(String s) {
        try {
            convertJsonObjectStringToMap(s)
            return null
        } catch (Exception e) {
            return e.message
        }
    }
}
