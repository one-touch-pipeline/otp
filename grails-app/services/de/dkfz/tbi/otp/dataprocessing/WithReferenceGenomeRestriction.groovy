package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

trait WithReferenceGenomeRestriction implements BamFileAnalysisServiceTrait {

    @Override
    String checkReferenceGenome() {
        return 'AND sp.mergingWorkPackage1.referenceGenome in (:referenceGenomes)'
    }

    @Override
    Map<String, Object> checkReferenceGenomeMap() {
        List<String> referenceGenomeNames = getReferenceGenomes()
        return [referenceGenomes: ReferenceGenome.findAllByNameInList(referenceGenomeNames)]
    }

    abstract List<String> getReferenceGenomes()
}
