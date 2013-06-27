package de.dkfz.tbi.ngstools.coverage_plot_mfs

import javax.validation.*

/**
 * Utilities for setting and checking parameters.
 * It allows to set parameters by using reflection with automatic type conversion from String to
 * {@link String}, {@link Integer} or {@link Boolean}.
 * Also it allows to check object parameters against its validation annotation.
 *
 */
//the base version is copied from the quality assessment jar file
enum ParameterUtils {
    INSTANCE

    /**
     * go through all the given parameters and sets the corresponding value to the property of the same
     * name of the given object. It is required, that size of parameters and values are equal.
     *
     * @param object the object holding the parameters (properties)
     * @param parameters the names of the property to set
     * @param values the values to set to the properties (parameters) using one of the specific parseTo* methods
     */
    public void parse(Object object, List<String> parameters, String[] values) {
        if (parameters.size() != values.length) {
            new RuntimeException("Count of parameters and values are different")
        }
        parameters.eachWithIndex { String parameter, int i ->
            println "set ${parameter} to ${values[i]}"
            parse(object, parameter, values[i])
        }
    }

    /**
     * checking the existence of the property and call the parseTo* method for the type of the property.
     *
     * @param object the object holding the property
     * @param parameter the name of the property
     * @param value the value to set to the property using one of the specific parseTo* methods
     */
    public void parse(Object object, String parameter, String value) {
        MetaProperty metaProperty = object.getClass().getMetaClass().getMetaProperty(parameter)
        if (!metaProperty) {
            throw new MissingPropertyException("property ${parameter} is not defined")
        }
        this."parseTo${metaProperty.getType().getSimpleName()}"(object, parameter, value)
    }

    /**
     * set the value to the given property.
     *
     * @param object the object holding the property
     * @param property the name of the property
     * @param value the value, which has to be assigned to the property
     */
    private void parseToString(Object object, String property, String value) {
        object."${property}" = value
    }

    /**
     * parse the value to an integer and assign it to the given property.
     *
     * @param object the object holding the property
     * @param property the name of the property
     * @param value the value, which has to be assigned to the property as integer
     */
    private void parseToInteger(Object object, String property, String value) {
        try {
            object."${property}" = value as Integer
        } catch (MissingPropertyException e) {
            throw e
        } catch (Exception e) {
            throw new ValidationException("${property} must be a number, but was ${value}")
        }
    }

    /**
     * parse the value to an long and assign it to the given property.
     *
     * @param object the object holding the property
     * @param property the name of the property
     * @param value the value, which has to be assigned to the property as long
     */
    private void parseToLong(Object object, String property, String value) {
        try {
            object."${property}" = value as Long
        } catch (MissingPropertyException e) {
            throw e
        } catch (Exception e) {
            throw new ValidationException("${property} must be a number, but was ${value}")
        }
    }

    /**
     * parse the value to an boolean and assign it to the given property.
     *
     * @param object the object holding the property
     * @param property the name of the property
     * @param value the value, which has to be assigned to the property as boolean
     */
    private void parseToBoolean(Object object, String property, String value) {
        Boolean bool = null
        if (value.equalsIgnoreCase("true")) {
            bool = Boolean.TRUE
        } else if (value.equalsIgnoreCase("false")) {
            bool = Boolean.FALSE
        } else {
            throw new ValidationException("${property} must be a true or false, but was ${value}")
        }
        try {
            object."${property}" = bool
        } catch (MissingPropertyException e) {
            throw e
        } catch (Exception e) {
            throw new ValidationException("${property} must be a true or false, but was ${value}")
        }
    }

    /**
     * validates the given object against its validation constraint annotation of the object fields
     *
     * @throws ValidationException if one or more parameters kept by the object is not valid
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
     * Validate the given path as input parameter.
     * Therefore the following constraints are checked:
     * <ul>
     * <li>The path needs to exist</li>
     * <li>The path needs to be a normal file (directory or special files are not allowed)</li>
     * <li>The file needs to be readable</li>
     * <li>The file may not be empty</li>
     * </ul>
     *
     * @param path the path of the file to be checked as input file
     */
    public void validateInputFile(String path) {
        File file = new File(path)
        if (!file.exists()) {
            throw new ValidationException("file ${path} not exist")
        }
        if (!file.isFile()) {
            throw new ValidationException("file ${path} is no normal file")
        }
        if (!file.canRead()) {
            throw new ValidationException("can not read file ${path}")
        }
        if (file.size() == 0) {
            throw new ValidationException("file is empty: ${path}")
        }
    }

    /**
     * Validate the given path as output parameter.
     * Therefore the following constraints are checked:
     * <ul>
     * <li>The parent directory of the path needs to exist</li>
     * <li>The parent directory of the path needs to be readable</li>
     * <li>The parent directory of the path needs to be writable</li>
     * <li>If a path with the given name exist, the path needs to reference a normal file (directory or special files are not allowed)</li>
     * </ul>
     *
     * @param path the path of the file to be checked as output directory
     */
    public void validateOutputDirectory(String path) {
        File file = new File(path)
        if (file.exists() && !file.isFile()) {
            throw new ValidationException("${path} is not a normal file")
        }
        File dir = file.getParentFile()
        if (!dir.exists()) {
            throw new ValidationException("output directory ${dir} does not exist")
        }
        if (!dir.canRead()) {
            throw new ValidationException("can not read output directory ${dir}")
        }
        if (!dir.canWrite()) {
            throw new ValidationException("can not write output directory ${dir}")
        }
    }

    /**
     * Helper to manage already existing output files depends on the overrideOutput flag.
     * If the file exist and the flag is false, an exception is thrown, otherwise the file is deleted.
     * If the delete fails, an {@link Exception} is thrown
     *
     * @param path The path of the output file to manage
     * @param overrideOutput flag, if existing output file should be overridden
     */
    private static void manageOutputFile(String path, boolean overrideOutput) {
        File file = new File(path)
        if (!overrideOutput && file.exists()) {
            throw new RuntimeException("The output file exists and override output is set to FALSE: ${path}")
        }
        if (overrideOutput && file.exists()) {
            Boolean deleted = file.delete()
            if (!deleted) {
                throw new RuntimeException("Can not delete the old output file: ${path}")
            }
        }
    }
}
