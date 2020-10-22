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
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.*

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.utils.Deprecateable
import de.dkfz.tbi.otp.utils.Entity

class ExternalWorkflowConfigFragment implements Commentable, Deprecateable<ExternalWorkflowConfigFragment>, Entity {

    String name
    String configValues
    ExternalWorkflowConfigFragment previous

    static constraints = {
        comment nullable: true
        previous nullable: true
        deprecationDate nullable: true
        configValues validator: {
            validateJsonString(it)
        }
    }

    static mapping = {
        configValues type: "text"
        comment cascade: "all-delete-orphan"
    }

    Map configValuesToMap() {
        ObjectMapper mapper = new ObjectMapper()
        return mapper.readValue(configValues, HashMap)
    }


    @SuppressWarnings("Instanceof")
    static Object validateJsonString(String s) {
        JSONElement jsonElement
        try {
            jsonElement = JSON.parse(s)
        } catch (JSONException | ConverterException ignored) {
            return "json"
        }
        if (!(jsonElement instanceof JSONObject)) {
            return "map"
        }
        return jsonElement.isEmpty() ? "empty" : true
    }
}
