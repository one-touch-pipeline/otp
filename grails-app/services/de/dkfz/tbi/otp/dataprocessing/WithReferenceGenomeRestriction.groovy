package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

trait WithReferenceGenomeRestriction implements BamFileAnalysisServiceTrait {

    @Override
    String checkReferenceGenome() {
        if (getReferenceGenomes()) {
            return 'AND sp.mergingWorkPackage1.referenceGenome in (:referenceGenomes)'
        } else {
            return "AND 1=0" //to avoid SQL grammar exception for `x IN ()`
        }
    }

    @Override
    Map<String, Object> checkReferenceGenomeMap() {
        List<String> referenceGenomeNames = getReferenceGenomes()
        if (referenceGenomeNames) {
            return [referenceGenomes: ReferenceGenome.findAllByNameInList(referenceGenomeNames)]
        } else {
            return [:]
        }
    }

    abstract List<String> getReferenceGenomes()
}
