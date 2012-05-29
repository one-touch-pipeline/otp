package de.dkfz.tbi.otp.ngsdata

class SeqPlatform {

    String name   // eg. solid, illumina
    String model

    static constraints = {
        name(blank: false)
        model(nullable: true)
    }

    String toString() {
        final int expressiveModelNameLimit = 4
        if (model.size() > expressiveModelNameLimit) {
            return model
        }
        return name + " " + model
    }
}
