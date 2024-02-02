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
package de.dkfz.tbi.otp.config

import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.transform.Canonical

import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.dataprocessing.*

@Transactional
class PropertiesValidationService {
    ConfigService configService
    ProcessingOptionService processingOptionService

    void validateStartUpProperties() {
        List<PropertyProblem> errorList = []

        Properties properties = configService.parsePropertiesFile()

        Collection<OtpProperty> used = OtpProperty.values().findAll { it.usedIn.contains(Environment.current.name.toUpperCase() as UsedIn) }
        used.each {
            if (!properties[it.key]) {
                if (it.defaultValue == null) {
                    errorList << new PropertyProblem("The required configuration property was not found: ${it.key}")
                } else {
                    log.info("Using default configuration '${it.defaultValue}' for '${it.key}'")
                }
            }
        }

        properties.each { key, value ->
            OtpProperty otpProperty = OtpProperty.getByKey(key as String)
            if (!otpProperty) {
                log.warn("Found unknown key '${key}' in the otp properties file")
            } else if (!otpProperty.validator.validate(value as String)) {
                errorList << new PropertyProblem("The value '${value}' for the key '${key}' is not valid for the check '${otpProperty.validator}'")
            }
        }

        if (!errorList.isEmpty()) {
            throw new OtpException("Configuration is invalid:\n${errorList*.toString().join("\n")}")
        }
    }

    OptionProblem validateProcessingOptionName(ProcessingOption.OptionName name, String type) {
        String existingOption = processingOptionService.findOptionAsString(name, type)

        if (name.deprecated) {
            return null
        }

        if (name.necessity == Necessity.REQUIRED && existingOption == null) {
            return new OptionProblem("Option '${name.name()}' with type '${type}' is not set", OptionProblem.ProblemType.MISSING)
        }
        if ((!name.validatorForType && type != null) || (name.validatorForType && !name.validatorForType?.validate(type))) {
            return new OptionProblem("Type '${type}' is invalid for '${name.name()}'", OptionProblem.ProblemType.TYPE_INVALID)
        }
        if (!name.validatorForValue.validate(existingOption)) {
            return new OptionProblem("Value '${existingOption}' is invalid for '${name.name()}', type '${type}'", OptionProblem.ProblemType.VALUE_INVALID)
        }
        return null
    }

    List<OptionProblem> validateProcessingOptions() {
        return ProcessingOption.OptionName.values().sort().collectMany { ProcessingOption.OptionName name ->
            List<String> types = name.validatorForType?.allowedValues != null ? name.validatorForType.allowedValues : []

            return types.collect { type ->
                validateProcessingOptionName(name, type as String)
            }.findAll()
        } as List<OptionProblem>
    }
}

@Canonical
class PropertyProblem {
    final String message

    @Override
    String toString() {
        return "${message}"
    }
}
@Canonical
class OptionProblem {
    final String message
    ProblemType type

    enum ProblemType {
        MISSING, TYPE_INVALID, VALUE_INVALID
    }

    @Override
    String toString() {
        return "${message} - ${type}"
    }
}
