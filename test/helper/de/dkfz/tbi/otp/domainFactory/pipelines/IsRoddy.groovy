package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*

trait IsRoddy implements IsPipeline {

    @Override
    MergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        Pipeline pipeline = properties.pipeline ?: findOrCreatePipeline()
        return createDomainObject(MergingWorkPackage, baseMergingWorkPackageProperties(properties) + [
                seqType         : { createSeqType() },
                pipeline        : pipeline,
                statSizeFileName: {
                    pipeline?.name == Pipeline.Name.PANCAN_ALIGNMENT ? "statSizeFileName_${nextId}.tab" : null
                },
        ], properties, saveAndValidate)
    }

    @Override
    RoddyBamFile createBamFile(Map properties = [:]) {
        return createRoddyBamFile(properties, RoddyBamFile)
    }

    private <T> T createRoddyBamFile(Map properties = [:], Class<T> clazz) {
        MergingWorkPackage workPackage = properties.workPackage
        if (!workPackage) {
            workPackage = createMergingWorkPackage()
            DomainFactory.createReferenceGenomeProjectSeqType(
                    referenceGenome: workPackage.referenceGenome,
                    project: workPackage.project,
                    seqType: workPackage.seqType,
                    statSizeFileName: workPackage.statSizeFileName,
            )
        }
        return createRoddyBamFile(properties, workPackage, clazz)
    }

    private <T> T createRoddyBamFile(Map properties = [:], MergingWorkPackage workPackage, Class<T> clazz) {
        Collection<SeqTrack> seqTracks = properties.seqTracks ?: [DomainFactory.createSeqTrackWithDataFiles(workPackage)]
        workPackage.seqTracks = seqTracks
        DomainFactory.createMergingCriteriaLazy(
                project: workPackage.project,
                seqType: workPackage.seqType
        )
        workPackage.save(flush: true, failOnError: true)
        T bamFile = createDomainObject(clazz, bamFileDefaultProperties(properties, seqTracks, workPackage) +
                [
                workDirectoryName           : "${RoddyBamFile.WORK_DIR_PREFIX}_${nextId}",
                identifier                  : RoddyBamFile.nextIdentifier(workPackage),
                config                      : {
                    findOrCreateConfig(
                            pipeline: workPackage.pipeline,
                            project: workPackage.project,
                            seqType: workPackage.seqType,
                            adapterTrimmingNeeded: workPackage.seqType.isRna() || workPackage.seqType.isWgbs() || workPackage.seqType.isChipSeq(),
                    )
                },
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ], properties)
        return bamFile
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames.WHOLE_GENOME.seqTypeName,
                displayName  : 'WGS',
                dirName      : 'whole_genome_sequencing',
                roddyName    : 'WGS',
                libraryLayout: LibraryLayout.PAIRED,
                singleCell   : false,
        ]
    }

    @Override
    Map getConfigProperties(Map properties) {
        return DomainFactory.createRoddyWorkflowConfigMapHelper(properties)
    }

    @Override
    Class getConfigPerProjectAndSeqTypeClass() {
        return RoddyWorkflowConfig
    }

    @Override
    Pipeline findOrCreatePipeline() {
        return findOrCreatePipeline(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }
}
