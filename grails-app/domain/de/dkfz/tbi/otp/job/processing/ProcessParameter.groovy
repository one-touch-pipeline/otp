package de.dkfz.tbi.otp.job.processing

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProcessParameter {
    String value
    String className
    Process process

    static constraints = {
        process(nullable: false, unique: true)
        className(nullable: false, validator: { ProcessParameterObject.isAssignableFrom(Class.forName(it, true, getClass().getClassLoader())) })
    }

    static mapping = {
        process index: "process_parameter_process_idx"
    }

    /**
     * Retrieves the domain object instance this ProcessParameter points to in case className is not null.
     *
     * If the object does not exists, this method returns null.
     * @return The domain object instance or null
     */
    ProcessParameterObject toObject() {
        if (className) {
            List resultList = ProcessParameter.executeQuery("FROM " + className + " WHERE id=" + value)
            if (resultList) {
                return exactlyOneElement(resultList)
            }
        }
        return null
    }
}
