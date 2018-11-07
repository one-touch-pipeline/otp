package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * This trait provides the interface to generate testdata for all alignment-like workflows.
 * New pipelines should provide their own implementation. When implementing the trait methods,
 * implementations should return, wherever possible, the most specific subclass that applies to that pipeline,
 * not the Abstract base class.
 */
trait IsPipeline implements DomainFactoryCore {

    abstract AbstractMergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true)

    abstract AbstractBamFile createBamFile(Map properties = [:])

    abstract Map getSeqTypeProperties()

    abstract Map getConfigProperties(Map properties)

    abstract Class getConfigPerProjectAndSeqTypeClass()

    abstract Pipeline findOrCreatePipeline()

    ConfigPerProjectAndSeqType createConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(getConfigPerProjectAndSeqTypeClass(), getConfigProperties(properties), properties, saveAndValidate)
    }

    ConfigPerProjectAndSeqType findOrCreateConfig(Map properties = [:], boolean saveAndValidate = true) {
        return findOrCreateDomainObject(getConfigPerProjectAndSeqTypeClass(), getConfigProperties(properties), properties, saveAndValidate)
    }

    @Override
    SeqType createSeqType(Map properties = [:], boolean saveAndValidate = true) {
        findOrCreateDomainObject(SeqType, getSeqTypeProperties(), properties ?: getSeqTypeProperties(), saveAndValidate).refresh()
    }

    Pipeline findOrCreatePipeline(Pipeline.Name name, Pipeline.Type type) {
        return findOrCreateDomainObject(Pipeline, [:], [
                name: name,
                type: type,
        ])
    }

    Map<String, ?> baseMergingWorkPackageProperties(Map properties) {
        return DomainFactory.baseMergingWorkPackageProperties(properties)
    }

    Map<String, ?> bamFileDefaultProperties(Map properties, Collection<SeqTrack> seqTracks, AbstractMergingWorkPackage workPackage) {
        return [
                numberOfMergedLanes: seqTracks.size(),
                seqTracks          : seqTracks as Set,
                workPackage        : workPackage,
                md5sum             : {
                    (!properties.containsKey('fileOperationStatus') ||
                            properties.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED) ? HelperUtils.randomMd5sum : null
                },
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize           : 10000,
                comment            : {
                    [
                            AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED,
                            AbstractMergedBamFile.QcTrafficLightStatus.REJECTED,
                    ].contains(properties.qcTrafficLightStatus) ? DomainFactory.createComment() : null
                }
        ]
    }

    Map<String, ?> getDefaultValuesForAbstractQualityAssessment() {
        return [
                qcBasesMapped                  : 0,
                totalReadCounter               : 0,
                qcFailedReads                  : 0,
                duplicates                     : 0,
                totalMappedReadCounter         : 0,
                pairedInSequencing             : 0,
                pairedRead1                    : 0,
                pairedRead2                    : 0,
                properlyPaired                 : 0,
                withItselfAndMateMapped        : 0,
                withMateMappedToDifferentChr   : 0,
                withMateMappedToDifferentChrMaq: 0,
                singletons                     : 0,
                insertSizeMedian               : 0,
                insertSizeSD                   : 0,
                referenceLength                : 1,
        ].asImmutable()
    }
}
