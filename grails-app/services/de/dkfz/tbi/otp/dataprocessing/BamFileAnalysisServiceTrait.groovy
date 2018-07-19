package de.dkfz.tbi.otp.dataprocessing

trait BamFileAnalysisServiceTrait {

    abstract String getConfigName()

    //Methods with default values
    String checkReferenceGenome() {
        return ''
    }

    Map<String, Object> checkReferenceGenomeMap() {
        return [:]
    }
}
