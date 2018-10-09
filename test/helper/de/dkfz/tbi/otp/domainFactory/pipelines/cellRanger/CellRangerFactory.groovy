package de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.*
import de.dkfz.tbi.otp.ngsdata.*

trait CellRangerFactory implements IsAlignment {

    @Override
    Pipeline findOrCreatePipeline() {
        findOrCreatePipeline(Pipeline.Name.CELL_RANGER, Pipeline.Type.ALIGNMENT)
    }

    @Override
    CellRangerMergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        Pipeline pipeline = properties.pipeline ?: findOrCreatePipeline()
        SeqType seqType = properties.seqType ?: createSeqType()
        Sample sample = properties.sample ?: createSample()
        return createDomainObject(CellRangerMergingWorkPackage, baseMergingWorkPackageProperties(properties) + [
                seqType : seqType,
                pipeline: pipeline,
                sample  : sample,
                config  : {
                    findOrCreateConfig([
                            pipeline: pipeline,
                            seqType : seqType,
                            project : sample.project,
                    ])
                },
        ], properties, saveAndValidate)
    }

    @Override
    AbstractBamFile createBamFile(Map properties = [:]) {
        CellRangerMergingWorkPackage workPackage = properties.workPackage
        if (!workPackage) {
            workPackage = createMergingWorkPackage()
            DomainFactory.createReferenceGenomeProjectSeqType(
                    referenceGenome: workPackage.referenceGenome,
                    project: workPackage.project,
                    seqType: workPackage.seqType,
                    statSizeFileName: workPackage.statSizeFileName,
            )
        }
        Collection<SeqTrack> seqTracks = properties.seqTracks ?: [DomainFactory.createSeqTrackWithDataFiles(workPackage)]
        workPackage.seqTracks = seqTracks
        workPackage.save(flush: true, failOnError: true)
        SingleCellBamFile bamFile = createDomainObject(SingleCellBamFile, bamFileDefaultProperties(properties, seqTracks, workPackage) +
                [
                        workDirectoryName  : "singleCell_${nextId}",
                        identifier         : SingleCellBamFile.nextIdentifier(workPackage),
                        type               : AbstractBamFile.BamType.MDUP,
                        fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                        fileSize           : 10000,
                ], properties)
        return bamFile
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames._10X_SCRNA.seqTypeName,
                displayName  : '10x_scRNA',
                dirName      : '10x_scRNA_sequencing',
                roddyName    : null,
                libraryLayout: LibraryLayout.PAIRED,
                singleCell   : true
        ]
    }

    @Override
    Map getConfigProperties(Map properties) {
        return [
                pipeline      : findOrCreatePipeline(),
                seqType       : { properties.seqType ?: createSeqType() },
                project       : { properties.project ?: createProject() },
                programVersion: "programmVersion${nextId}",
                dateCreated   : { new Date() },
                lastUpdated   : { new Date() },
        ]
    }

    @Override
    Class getConfigPerProjectAndSeqTypeClass() {
        return CellRangerConfig
    }
}
