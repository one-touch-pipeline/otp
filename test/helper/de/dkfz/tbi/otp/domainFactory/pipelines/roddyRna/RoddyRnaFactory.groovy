package de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.*
import de.dkfz.tbi.otp.ngsdata.*

trait RoddyRnaFactory implements IsAlignment, IsRoddy {

    @Override
    RnaRoddyBamFile createBamFile(Map properties = [:]) {
        return IsRoddy.super.createRoddyBamFile(properties, RnaRoddyBamFile)
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames.RNA.seqTypeName,
                displayName  : 'RNA',
                dirName      : 'rna_sequencing',
                roddyName    : 'RNA',
                libraryLayout: LibraryLayout.PAIRED,
                singleCell   : false
        ]
    }

    @Override
    Pipeline findOrCreatePipeline() {
        findOrCreatePipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }
}
