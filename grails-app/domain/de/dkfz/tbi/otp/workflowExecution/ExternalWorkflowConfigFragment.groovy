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
package de.dkfz.tbi.otp.workflowExecution

import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import groovy.transform.TupleConstructor
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.*

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.job.processing.ClusterJobSubmissionOptionsService
import de.dkfz.tbi.otp.utils.Deprecateable
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class ExternalWorkflowConfigFragment implements Commentable, Deprecateable<ExternalWorkflowConfigFragment>, Entity {

    String name
    /** nested maps in JSON format */
    String configValues
    ExternalWorkflowConfigFragment previous

    static constraints = {
        comment nullable: true
        previous nullable: true
        deprecationDate nullable: true
        configValues validator: {
            validateConfigValues(it)
        }
    }

    static mapping = {
        configValues type: "text"
        comment cascade: "all-delete-orphan"
    }

    Map configValuesToMap() {
        ObjectMapper mapper = new ObjectMapper()
        if (deprecationDate) {
            return [:]
        }
        return mapper.readValue(configValues, HashMap)
    }

    Optional<ExternalWorkflowConfigSelector> getSelector() {
        ExternalWorkflowConfigFragment f = this
        while (true) {
            ExternalWorkflowConfigFragment previous = atMostOneElement(ExternalWorkflowConfigFragment.findAllByPrevious(f))
            if (previous) {
                f = previous
            } else {
                break
            }
        }
        return Optional.ofNullable(atMostOneElement(ExternalWorkflowConfigSelector.findAllByExternalWorkflowConfigFragment(f)))
    }

    @SuppressWarnings("Instanceof")
    static Object validateConfigValues(String s) {
        JSONElement jsonElement
        try {
            jsonElement = JSON.parse(s)
        } catch (JSONException | ConverterException ignored) {
            return "invalid.json"
        }
        if (!(jsonElement instanceof JSONObject)) {
            return "no.object"
        }
        List<String> invalidTypes = jsonElement.collect { k, v ->
            return (k as String) in Type.values()*.name() ? "" : k as String
        }.findAll()
        if (invalidTypes) {
            return ["wrong.type", invalidTypes.join(", "), Type.values()*.name().join(", ")]
        }
        List<String> invalidConfigs = jsonElement.collect { k, v ->
            Type t = Type.valueOf(k as String)
            return (t.validateConfig && !t.validateConfig(v)) ? k as String : ""
        }.findAll()
        return invalidConfigs ? ["invalid.configs", invalidConfigs.join(",")] : true
    }

    /**
     * The top-level JSON object in {@link ExternalWorkflowConfigFragment#configValues} must contain only the name() of these Types as keys
     * Optionally the content can be validated by setting validateConfig, if no validation is required set it to null
     */
    @TupleConstructor enum Type {
        /** used for jobs that are submitted directly to cluster by OTP */
        OTP_CLUSTER({ ClusterJobSubmissionOptionsService.validateJsonString(it.toString()) }),
        WORKFLOWS({ true }),

        final Closure<Boolean> validateConfig
    }
}
