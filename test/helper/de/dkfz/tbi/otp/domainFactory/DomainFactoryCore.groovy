package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.dataprocessing.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils

trait DomainFactoryCore implements DomainFactoryHelper {

    Project createProject(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Project, [
                name                          : "project_${nextId}",
                phabricatorAlias              : "phabricatorAlias_${nextId}",
                dirName                       : "projectDirName_${nextId}",
                realm                         : { DomainFactory.createRealm() },
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling           : QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK
        ], properties, saveAndValidate)
    }

    Sample createSample(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Sample, [
                individual: { DomainFactory.createIndividual() },
                sampleType: { DomainFactory.createSampleType() },
        ], properties, saveAndValidate)
    }

    SeqType createSeqType(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(SeqType, getCustomSeqTypeProperties(properties), properties, saveAndValidate)
    }

    private Map getCustomSeqTypeProperties(Map properties = [:]) {
        String defaultName = "seqTypeName_${nextId}"
        return [
                name         : defaultName,
                displayName  : properties.displayName ?: properties.name ?: defaultName,
                dirName      : "seqTypeDirName_${nextId}",
                roddyName    : null,
                libraryLayout: LibraryLayout.SINGLE,
                singleCell   : false,
                importAlias  : [],
        ]
    }

    ReferenceGenome createReferenceGenome(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ReferenceGenome, [
                name                        : "referencegenome_name${nextId}",
                path                        : HelperUtils.uniqueString,
                fileNamePrefix              : "referencegenome-prefix_${nextId}",
                length                      : 1,
                lengthWithoutN              : 1,
                lengthRefChromosomes        : 1,
                lengthRefChromosomesWithoutN: 1,
                chromosomePrefix            : "",
                chromosomeSuffix            : "",
        ], properties, saveAndValidate)
    }

    ToolName createToolName(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ToolName, [
                name: "GENOME_STAR_INDEX_${nextId}",
                type: ToolName.Type.RNA,
                path: "path_${nextId}",
        ], properties, saveAndValidate)
    }

    ReferenceGenomeIndex createReferenceGenomeIndex(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ReferenceGenomeIndex, [
                referenceGenome : { createReferenceGenome() },
                toolName        : { createToolName() },
                path            : "path_${nextId}",
                indexToolVersion: "indexToolVersion_${nextId}",
        ], properties, saveAndValidate)
    }
}
