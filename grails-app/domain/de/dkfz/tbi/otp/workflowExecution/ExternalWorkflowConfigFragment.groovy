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
import grails.databinding.BindUsing
import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.*

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.job.processing.ClusterJobSubmissionOptionsService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.utils.Deprecateable
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@ManagedEntity
@ToString(includeNames = true, includePackage = false)
class ExternalWorkflowConfigFragment implements Commentable, Deprecateable<ExternalWorkflowConfigFragment>, Entity {

    String name

    /** Nested maps in JSON format
     * @BindUsing closure forces the keys to be stored with quotes,
     * which is ignored by validation due to parsing.
     * */
    @BindUsing({ obj, source ->
        // Grails doesn't handle exception thrown here nicely
        // Let validator do its job by passing the (invalid) value back
        try {
            JSON.parse(source['configValues'] as String).toString()
        } catch (ConverterException e) {
            source['configValues']
        }
    })
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

    static Closure mapping = {
        name index: "external_workflow_config_fragment_name_idx"    // It is an unique constraints where 'deprecation_date IS NULL'
        previous index: "external_workflow_config_fragment_previous_idx"
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

    Optional<ExternalWorkflowConfigSelector> findSelector() {
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
        Set<String> occurredErrors = jsonElement.collectMany { key, value ->
            Type type = Type.valueOf(key as String)
            return type.validateConfig(value.toString())
        }.findAll()
        return occurredErrors ? ["errors.json", occurredErrors.join(", ")] : true
    }

    /**
     * The top-level JSON object in {@link ExternalWorkflowConfigFragment#configValues} must contain only the name() of these Types as keys
     * Optionally the content can be validated by setting validateConfig, if no validation is required set it to null
     */
    @TupleConstructor
    @SuppressWarnings("Indentation")
    enum Type {
        /** used for jobs that are submitted directly to cluster by OTP */
        OTP_CLUSTER({ String s ->
            String validation = ClusterJobSubmissionOptionsService.validateJsonObjectString(s)
            if (validation) {
                Set<String> validationSet = []
                validationSet.add(validation)
                return validationSet
            }
            return Collections.<String> emptySet()
        }),
        WORKFLOWS({ Collections.<String> emptySet() }),
        RODDY({ String s -> RoddyConfigService.validateRoddyConfig(s) }),
        RODDY_FILENAMES({ String s -> RoddyConfigService.validateRoddyFilenamesConfig(s) }),

        final Closure<Set<String>> validateConfig
    }
}
