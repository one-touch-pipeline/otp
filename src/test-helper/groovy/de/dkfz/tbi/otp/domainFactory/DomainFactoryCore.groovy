/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
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
                qcThresholdHandling           : QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK,
        ], properties, saveAndValidate)
    }

    Individual createIndividual(Map properties = [:]) {
        return createDomainObject(Individual, [
                pid         : "pid_${nextId}",
                mockPid     : "mockPid_${nextId}",
                mockFullName: "mockFullName_${nextId}",
                type        : Individual.Type.REAL,
                project     : { createProject() },
        ], properties)
    }

    SampleType createSampleType(Map properties = [:]) {
        return createDomainObject(SampleType, [
                name                   : "sampleTypeName-${nextId}",
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ], properties)
    }

    Sample createSample(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Sample, [
                individual: { createIndividual() },
                sampleType: { createSampleType() },
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

    LibraryPreparationKit createLibraryPreparationKit(Map properties = [:]) {
        return createDomainObject(LibraryPreparationKit, [
                name            : "library-preperation-kit-name_${nextId}",
                shortDisplayName: "library-preperation-kit-short-name_${nextId}",
                importAlias     : [],
        ], properties)
    }

    SoftwareTool createSoftwareTool(Map properties = [:]) {
        return createDomainObject(SoftwareTool, [
                programName: "softwareToolProgramName_${nextId}",
                type       : SoftwareTool.Type.ALIGNMENT,
        ], properties)
    }

    SoftwareToolIdentifier createSoftwareToolIdentifier(Map properties = [:]) {
        return createDomainObject(SoftwareToolIdentifier, [
                name        : "softwareToolIdentifier_${nextId}",
                softwareTool: { createSoftwareTool() },
        ], properties)
    }

    SeqCenter createSeqCenter(Map properties = [:]) {
        return createDomainObject(SeqCenter, [
                name   : "seqCenterName_${nextId}",
                dirName: "seqCenterDirName_${nextId}",
        ], properties)
    }

    AntibodyTarget createAntibodyTarget(Map properties = [:]) {
        return createDomainObject(AntibodyTarget, [
                name       : "antibodyTargetName_${nextId}",
                importAlias: [],
        ], properties)
    }

    Run createRun(Map properties = [:]) {
        return createDomainObject(Run, [
                name       : "runName_${nextId}",
                seqCenter  : { createSeqCenter() },
                seqPlatform: { createSeqPlatformWithSeqPlatformGroup() },
        ], properties)
    }

    RunSegment createRunSegment(Map properties = [:]) {
        return createDomainObject(RunSegment, [
                importMode: RunSegment.ImportMode.AUTOMATIC,
        ], properties)
    }


    SeqTrack createSeqTrack(Map properties = [:]) {
        if (properties.seqType?.hasAntibodyTarget) {
            return createChipSeqSeqTrack(properties)
        }
        if (properties.seqType?.isExome()) {
            return createExomeSeqTrack(properties)
        }
        return createDomainObject(SeqTrack, getSeqTrackProperties(properties) + [
                seqType: { createSeqType() },
        ], properties)
    }

    ExomeSeqTrack createExomeSeqTrack(Map properties = [:]) {
        return createDomainObject(ExomeSeqTrack, getSeqTrackProperties(properties) + [
                seqType: { DomainFactory.createExomeSeqType() },
        ], properties)
    }

    ChipSeqSeqTrack createChipSeqSeqTrack(Map properties = [:]) {
        return createDomainObject(ChipSeqSeqTrack, getSeqTrackProperties(properties) + [
                seqType       : { DomainFactory.createChipSeqType() },
                antibodyTarget: { createAntibodyTarget() },
        ], properties)
    }

    private Map getSeqTrackProperties(Map properties = [:]) {
        return [
                laneId               : "laneId_${nextId}",
                sample               : { createSample() },
                pipelineVersion      : { createSoftwareTool() },
                run                  : { createRun() },
                kitInfoReliability   : { properties.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED },
                normalizedLibraryName: { SeqTrack.normalizeLibraryName(properties.libraryName) },
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

    Species createSpecies(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Species, [
                commonName     : 'commonName',
                scientificName : 'scientificName',
        ], properties, saveAndValidate)
    }

    SeqPlatform createSeqPlatformWithSeqPlatformGroup(Map properties = [:]) {
        Set<SeqPlatformGroup> spg = (properties.seqPlatformGroups ?: [createSeqPlatformGroup()]) as Set

        SeqPlatform sp = createSeqPlatform([seqPlatformGroups: spg] + properties)

        sp.seqPlatformGroups.each {
            sp.addToSeqPlatformGroups(it)
        }
        sp.save(flush: true)

        return sp
    }

    SeqPlatform createSeqPlatform(Map properties = [:]) {
        return createDomainObject(SeqPlatform, [
                name                 : "seqPlatform_${nextId}",
                seqPlatformModelLabel: { createSeqPlatformModelLabel() },
        ], properties)
    }

    SeqPlatformModelLabel createSeqPlatformModelLabel(Map properties = [:]) {
        return createDomainObject(SeqPlatformModelLabel, [
                name       : "seqPlatformModelLabel_${nextId}",
                importAlias: [],
        ], properties)
    }

    SequencingKitLabel createSequencingKitLabel(Map properties = [:]) {
        return createDomainObject(SequencingKitLabel, [
                name       : "SequencingKitLabel__${nextId}",
                importAlias: [],
        ], properties)
    }

    SeqPlatformGroup createSeqPlatformGroup(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [:], properties)
    }

    SeqPlatformGroup createSeqPlatformGroupWithMergingCriteria(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [
                mergingCriteria: createMergingCriteriaLazy(
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
                )
        ], properties)
    }

    MergingCriteria createMergingCriteria(Map properties = [:]) {
        return createDomainObject(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties)
    }

    MergingCriteria createMergingCriteriaLazy(Map properties) {
        if ((properties.get("seqType") as SeqType)?.isWgbs() && !properties.hasProperty("useLibPrepKit")) {
            properties.put("useLibPrepKit", false)
        }

        return findOrCreateDomainObject(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties)
    }
}
