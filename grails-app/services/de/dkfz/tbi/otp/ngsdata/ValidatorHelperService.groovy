/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValueTuple

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class ValidatorHelperService {

    AntibodyTargetService antibodyTargetService
    LibraryPreparationKitService libraryPreparationKitService
    SampleIdentifierService sampleIdentifierService
    SeqTypeService seqTypeService
    SeqPlatformService seqPlatformService
    SpeciesWithStrainService speciesWithStrainService

    String getSeqTypeNameFromMetadata(ValueTuple tuple) {
        assert tuple

        return tuple.getValue(SEQUENCING_TYPE.name()) ?: ''
    }

    List<SpeciesWithStrain> getSpeciesFromMetadata(ValueTuple tuple) {
        assert tuple
        List<String> speciesList = tuple.getValue(MetaDataColumn.SPECIES.name())?.split('[+]')*.trim() ?: '' as List<String>
        List<SpeciesWithStrain> speciesWithStrainsList = []
        speciesList.each { String speciesCommonName ->
            SpeciesWithStrain sws = speciesWithStrainService.getByAlias(speciesCommonName)
            if (sws) {
                speciesWithStrainsList.add(sws)
            }
        }
        return speciesList.size() > speciesWithStrainsList.size() ? [] : speciesWithStrainsList.unique()
    }

    @CompileDynamic
    Project getProjectFromMetadata(ValueTuple tuple) {
        assert tuple

        String sampleName = tuple.getValue(SAMPLE_NAME.name()) ?: ''
        String projectName = tuple.getValue(PROJECT.name()) ?: ''
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.project
        }
        Project projectFromProjectColumn = ProjectService.findByNameOrNameInMetadataFiles(projectName)
        if (projectFromProjectColumn) {
            return projectFromProjectColumn
        }
        return null
    }

    SeqType getSeqTypeFromMetadata(ValueTuple tuple) {
        assert tuple

        boolean isSingleCell = seqTypeService.isSingleCell(tuple.getValue(BASE_MATERIAL.name()))
        SequencingReadType seqReadType = SequencingReadType.getByName(tuple.getValue(SEQUENCING_READ_TYPE.name()))
        if (!seqReadType) {
            return null
        }

        String seqTypeName = getSeqTypeNameFromMetadata(tuple)
        if (!seqTypeName) {
            return null
        }

        return seqTypeService.findByNameOrImportAlias(
                seqTypeName,
                [libraryLayout: seqReadType, singleCell: isSingleCell],
        )
    }

    AntibodyTarget findAntibodyTarget(ValueTuple valueTuple, SeqType seqType) {
        String antibodyTargetName = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
        if (seqType.hasAntibodyTarget && antibodyTargetName) {
            return antibodyTargetService.findByNameOrImportAlias(antibodyTargetName)
        }
        return null
    }

    LibraryPreparationKit findLibraryPreparationKit(ValueTuple valueTuple) {
        assert valueTuple

        String libraryPreparationKitName = valueTuple.getValue(LIB_PREP_KIT.name())
        return libraryPreparationKitName ? libraryPreparationKitService.findByNameOrImportAlias(libraryPreparationKitName) : null
    }

    SeqPlatform findSeqPlatform(ValueTuple valueTuple) {
        assert valueTuple

        String seqPlatformName = valueTuple.getValue(INSTRUMENT_PLATFORM.name())
        String seqPlatformModel = valueTuple.getValue(INSTRUMENT_MODEL.name())
        String sequencingKit = valueTuple.getValue(SEQUENCING_KIT.name())

        return seqPlatformName ? seqPlatformService.findSeqPlatform(seqPlatformName, seqPlatformModel, sequencingKit) : null
    }

    @CompileDynamic
    Sample findExistingSampleForValueTuple(ValueTuple valueTuple) {
        assert valueTuple

        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.sample
        }

        Project project = getProjectFromMetadata(valueTuple)
        if (!project) {
            return null
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return null
        }

        return Sample.createCriteria().get {
            individual {
                eq("pid", parsedSampleIdentifier.pid)
            }
            sampleType {
                or {
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(parsedSampleIdentifier.sampleTypeDbName))
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(
                            parsedSampleIdentifier.sampleTypeDbName.replace('_', '-')))
                }
            }
        } as Sample
    }

    SeqPlatformGroup findSeqPlatformGroup(ValueTuple valueTuple, SeqType seqType) {
        assert valueTuple
        assert seqType

        SeqPlatform seqPlatform = findSeqPlatform(valueTuple)
        return seqPlatform ? seqPlatform.getSeqPlatformGroupForMergingCriteria(
                getProjectFromMetadata(valueTuple),
                seqType,
        ) : null
    }

    @CompileDynamic
    String getPid(ValueTuple valueTuple) {
        assert valueTuple

        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.individual.pid
        }

        Project project = getProjectFromMetadata(valueTuple)
        if (!project) {
            return
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return
        }
        return parsedSampleIdentifier.pid
    }

    @CompileDynamic
    String getSampleType(ValueTuple valueTuple) {
        assert valueTuple

        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.sample.sampleType.displayName
        }

        Project project = getProjectFromMetadata(valueTuple)
        if (!project) {
            return
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return
        }

        return parsedSampleIdentifier.sampleTypeDbName
    }

    MergingCriteria getMergingCriteria(ValueTuple valueTuple, SeqType seqType) {
        assert valueTuple
        assert seqType

        Project project = getProjectFromMetadata(valueTuple)
        return findMergingCriteria(project, seqType)
    }

    @CompileDynamic
    MergingCriteria findMergingCriteria(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
    }
}
