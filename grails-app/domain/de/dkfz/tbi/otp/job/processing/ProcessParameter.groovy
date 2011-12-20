package de.dkfz.tbi.otp.job.processing

class ProcessParameter {
    String value
    String className
    Process process

    static constraints = {
        className(nullable: true)
    }
}
