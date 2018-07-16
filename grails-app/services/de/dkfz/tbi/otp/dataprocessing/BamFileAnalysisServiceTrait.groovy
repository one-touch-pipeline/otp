package de.dkfz.tbi.otp.dataprocessing

trait BamFileAnalysisServiceTrait {

    abstract String getConfigName()

    //Methods with default values
    String additionalConfigParameters() {
        return ""
    }

    String checkReferenceGenome() {
        return ''
    }

    Map<String, Object> checkReferenceGenomeMap() {
        return [:]
    }
}
