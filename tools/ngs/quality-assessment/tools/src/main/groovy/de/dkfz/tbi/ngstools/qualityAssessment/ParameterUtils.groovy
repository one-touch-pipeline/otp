package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.*

/**
 * Utilities for setting and checking parameters.
 * It allows to set parameters of using reflection with automatic type conversion from String to
 * {@link String}, {@link Integer} or {@link Boolean}.
 * Also it allows to check object parameters against its validation annotation.
 */
enum ParameterUtils {
    INSTANCE

    /**
     * validates the given object against its validation constraint annotation of the object fields
     *
     * @throws ValidationException if the one or more parameters kept by the object is not valid
     */
    public void validate(Object object) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        Validator validator = factory.getValidator()
        Set<ConstraintViolation<?>> violations = validator.validate(object)
        if (violations) {
            StringBuilder failedMessage = new StringBuilder()
            violations.each { constraintViolation ->
                failedMessage.append("${constraintViolation.getPropertyPath()}: ${constraintViolation.getMessage()}\n\n")
            }
            throw new ValidationException(failedMessage.toString())
        }
    }

    /**
     * checking the existence of the property and calling the parseTo* method for the type of the property.
     *
     * @param object, the object holding the property
     * @param parameter, the name of the property
     * @param value, the value, which shall be assigned to the property using one of the specific parseTo* methods
     */
    public void parse(Object object, String parameter, String value) {
        MetaProperty metaProperty = object.getClass().getMetaClass().getMetaProperty(parameter)
        if (!metaProperty) {
            throw new IllegalArgumentException("The property '${parameter}' is not defined for the object '${object}'")
        }
        this."parseTo${metaProperty.getType().getSimpleName()}"(object, parameter, value)
    }

    /**
     * assign the value to the given property.
     *
     * @param object, the object holding the property
     * @param property, the name of the property
     * @param value, the value, which has to be assigned to the property
     */
    private void parseToString(Object object, String property, String value) {
        object."${property}" = value
    }

    /**
     * parse the value to an integer and assign it to the given property.
     *
     * @param object, the object holding the property
     * @param property, the name of the property
     * @param value, the value, which has to be assigned to the property as integer
     */
    private void parseToInteger(Object object, String property, String value) {
        try {
            object."${property}" = value as Integer
        } catch (Exception e) {
            throw new ValidationException("The property '${property}' must be a number, but was '${value}'")
        }
    }

    /**
     * parse the value to boolean and assign it to the given property.
     *
     * @param object, the object holding the property
     * @param property, the name of the property
     * @param value, the value, which has to be assigned to the property as boolean
     */
    private void parseToBoolean(Object object, String property, String value) {
        Boolean bool = null
        if (value.equalsIgnoreCase("true")) {
            bool = Boolean.TRUE
        } else if (value.equalsIgnoreCase("false")) {
            bool = Boolean.FALSE
        } else {
            throw new ValidationException("The property '${property}' must be true or false, but was '${value}'")
        }
        try {
            object."${property}" = bool
        } catch (Exception e) {
            throw new ValidationException("The property '${property}' must be true or false, but was '${value}'")
        }
    }
}
