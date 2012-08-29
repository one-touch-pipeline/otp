package de.dkfz.tbi.otp.job.processing

class ProcessParameter {
    String value
    String className
    Process process

    static constraints = {
        className(nullable: true)
    }

    /**
     * Retrieves the domain object instance this ProcessParameter points to in case className is not null.
     *
     * If className is null, this method returns null.
     * @return The domain object instance or null
     */
    def toObject() {
        if (className) {
            return ProcessParameter.executeQuery("FROM " + className + " WHERE id=" + value).first()
        }
        return null
    }
}
