package de.dkfz.tbi.otp.dataprocessing

class Workflow {

    enum Name {
        DEFAULT_OTP,
        RODDY
    }
    Name name

    enum Type {
        ALIGNMENT
    }
    Type type


    static constraints = {
        name unique: 'type'
    }

    @Override
    String toString() {
        return "${name} ${type}"
    }
}
