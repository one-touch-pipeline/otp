package de.dkfz.tbi.otp.config

import grails.util.Environment
import groovy.transform.Canonical

import de.dkfz.tbi.otp.dataprocessing.*

class PropertiesValidationService {
    ConfigService configService
    ProcessingOptionService processingOptionService

    void validateStartUpProperties() {
        List<PropertyProblem> errorList = []

        Properties properties = configService.parsePropertiesFile()

        List<OtpProperty> used = OtpProperty.values().findAll { it.usedIn.contains(Environment.current.getName().toUpperCase() as UsedIn) }
        used.each {
            if (!properties[it.key]) {
                if (it.defaultValue == null) {
                    errorList << new PropertyProblem("The required configuration property was not found: ${it.key}")
                } else {
                    log.info("Using default configuration '${it.defaultValue}' for '${it.key}'")
                }
            }
        }

        properties.each { String key, String value ->
            OtpProperty otpProperty = OtpProperty.findByKey(key)
            if (!otpProperty) {
                log.warn("Found unknown key '${key}' in the otp properties file")
            } else if (!otpProperty.validator.validate(value)) {
                errorList << new PropertyProblem("The value '${value}' for the key '${key}' is not valid for the check '${otpProperty.validator}'")
            }
        }

        if (!errorList.isEmpty()) {
            throw new Exception("Configuration is invalid: ${errorList*.toString().join("\n")}")
        }
    }

    OptionProblem validateProcessingOptionName(ProcessingOption.OptionName name, String type) {
        String existingOption = processingOptionService.findOptionAsString(name, type)

        if (name.necessity == Necessity.REQUIRED && existingOption == null) {
            return new OptionProblem("Option '${name.name()}' with type '${type}' is not set", OptionProblem.ProblemType.MISSING)
        }
        if ((!name.validatorForType && type != null) || (name.validatorForType && !name.validatorForType?.validate(type))) {
            return new OptionProblem("Type '${type}' is invalid for '${name.name()}'", OptionProblem.ProblemType.TYPE_INVALID)
        }
        if (!name.isDeprecated() && !name.validatorForValue.validate(existingOption)) {
            return new OptionProblem("Value '${existingOption}' is invalid for '${name.name()}', type '${type}'", OptionProblem.ProblemType.VALUE_INVALID)
        }
        return null
    }

    List<OptionProblem> validateProcessingOptions() {
        return ProcessingOption.OptionName.values().sort().collectMany { ProcessingOption.OptionName name ->
            List<String> types = name.validatorForType?.allowedValues != null ? name.validatorForType.allowedValues : [null]

            return types.collect { String type ->
                validateProcessingOptionName(name, type)
            }.findAll()
        }
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
