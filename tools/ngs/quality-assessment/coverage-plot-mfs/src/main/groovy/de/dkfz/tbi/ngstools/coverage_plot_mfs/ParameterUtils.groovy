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
     * go through all given parameters and set the corresponding value to the property of the same
     * name of the given object. It is required, that size of parameters and values are equal.
     *
     * @param object, the object holding the parameters (properties)
     * @param parameters, the names of the property to set
     * @param values, the values, which shall be assigned to the properties (parameters) using one of the specific parseTo* methods
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
     * checking the existence of the property and calling the parseTo* method for the type of the property.
     *
     * @param object, the object holding the property
     * @param parameter, the name of the property
     * @param value, the value, which shall be assigned to the property using one of the specific parseTo* methods
     */
    public void parse(Object object, String parameter, String value) {
        MetaProperty metaProperty = object.getClass().getMetaClass().getMetaProperty(parameter)
        if (!metaProperty) {
            throw new MissingPropertyException("The property '${parameter}' is not defined for the object '${object}'")
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
        } catch (MissingPropertyException e) {
            throw e
        } catch (Exception e) {
            throw new ValidationException("The property '${property}' must be a number, but was '${value}'")
        }
    }

    /**
     * parse the value to a long and assign it to the given property.
     *
     * @param object, the object holding the property
     * @param property, the name of the property
     * @param value, the value, which has to be assigned to the property as long
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
        } catch (MissingPropertyException e) {
            throw e
        } catch (Exception e) {
            throw new ValidationException("The property '${property}' must be true or false, but was '${value}'")
        }
    }

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
     * Validate the given path as input parameter.
     * Therefore the following constraints are checked:
     * <ul>
     * <li>The path needs to exist</li>
     * <li>The path needs to be a normal file (directory or special files are not allowed)</li>
     * <li>The file needs to be readable</li>
     * <li>The file must not be empty</li>
     * </ul>
     *
     * @param path, the path of the file to be checked as input file
     */
    public void validateInputFile(String path) {
        File file = new File(path)
        if (!file.exists()) {
            throw new ValidationException("The file '${path}' does not exist")
        }
        if (!file.isFile()) {
            throw new ValidationException("The file '${path}' is no normal file")
        }
        if (!file.canRead()) {
            throw new ValidationException("The file '${path}' can not be read")
        }
        if (file.size() == 0) {
            throw new ValidationException("There is no content in the file ${path}")
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
     * @param path, the path of the file to be checked as output directory
     */
    public void validateOutputDirectory(String path) {
        File file = new File(path)
        if (file.exists() && !file.isFile()) {
            throw new ValidationException("The file '${path}' exists, but is not a normal file")
        }
        File dir = file.getParentFile()
        if (!dir.exists()) {
            throw new ValidationException("The output directory '${dir}', where the file '${path}' should be put, does not exist")
        }
        if (!dir.canRead()) {
            throw new ValidationException("The output directory '${dir}', where the file '${path}' should be put, can not be read")
        }
        if (!dir.canWrite()) {
            throw new ValidationException("The file '${path}' can not be put to the directory '${dir}', since the directory is not writeable")
        }
    }

    /**
     * Helper to manage already existing output files, depending on the overrideOutput flag.
     * If the file exist and the flag is false, an exception is thrown, otherwise the file is deleted.
     * If the delete fails, an {@link Exception} is thrown
     *
     * @param path, the path of the output file to manage
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
                throw new RuntimeException("The old output file can not be deleted: ${path}")
            }
        }
    }
}
