package de.dkfz.tbi.ngstools.qualityAssessment

import java.lang.reflect.Field
import javax.validation.*

abstract class FileValidator {

    public static void validate(Object object) {
        object.getClass().getDeclaredFields().each { Field field ->
            if (field.isAnnotationPresent(FileCanRead.class)) {
                String fieldName = field.getName()
                canRead(object.getAt(fieldName), fieldName)
            }
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
     * @throws ValidationException if the path is not valid
     */
    public static void canRead(String filePath, String fieldName) {
        if (!filePath) {
            throw new ValidationException("$fieldName: path to the file must be provided")
        }
        File file = new File(filePath)
        if (!file.canRead()) {
            throw new ValidationException("$fieldName: the file '${filePath}' can not be read")
        }
        if (file.size() == 0) {
            throw new ValidationException("$fieldName: there is no content in the file ${filePath}")
        }
        if (!file.isFile()) {
            throw new ValidationException("$fieldName: ${filePath} is not a normal file")
        }
    }
}
