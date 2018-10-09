package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

trait DomainFactoryCore implements DomainFactoryHelper {

    Project createProject(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Project, [
                name                    : "project_${nextId}",
                phabricatorAlias        : "phabricatorAlias_${nextId}",
                dirName                 : "projectDirName_${nextId}",
                realm                   : { DomainFactory.createRealm() },
                alignmentDeciderBeanName: "DUMMY_BEAN_NAME",
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER
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
}
