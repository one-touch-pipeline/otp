package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory

trait AlignmentPipelineFactory {

    static class CellRangerFactoryInstance implements CellRangerFactory {

        static final INSTANCE = new CellRangerFactoryInstance()
    }

    static class RoddyRnaFactoryInstance implements RoddyRnaFactory {

        static final INSTANCE = new RoddyRnaFactoryInstance()
    }


    private static final Map<Pipeline.Name, Closure<AbstractBamFile>> alignmentCreation = [
            (Pipeline.Name.DEFAULT_OTP)        : { DomainFactory.createProcessedMergedBamFile() },
            (Pipeline.Name.PANCAN_ALIGNMENT)   : { DomainFactory.createRoddyBamFile() },
            (Pipeline.Name.RODDY_RNA_ALIGNMENT): { RoddyRnaFactoryInstance.INSTANCE.createBamFile() },
            (Pipeline.Name.CELL_RANGER)        : { CellRangerFactoryInstance.INSTANCE.createBamFile() },
    ].asImmutable()

    AbstractBamFile createBamFileForPipelineName(Pipeline.Name pipeline) {
        assert pipeline?.type == Pipeline.Type.ALIGNMENT
        assert alignmentCreation.containsKey(pipeline): "Bam file creation for Pipeline ${pipeline} is not configured"
        return alignmentCreation[pipeline]()
    }
}
