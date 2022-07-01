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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow

@Transactional
class SeqTypeService extends MetadataFieldsService<SeqType> {

    @Autowired
    ApplicationContext context

    SeqType findById(long id) {
        return SeqType.get(id)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map> getDisplayableMetadata() {
        return SeqType.list(sort: "name", order: "asc").collect {
            [
                    id               : SeqType.findAllByNameAndSingleCell(it.name, it.singleCell)*.id?.sort()?.first(),
                    name             : it.name,
                    legacy           : it.legacy,
                    dirName          : it.dirName,
                    singleCell       : it.singleCell,
                    hasAntibodyTarget: it.hasAntibodyTarget,
                    needsBedFile     : it.needsBedFile,
                    libraryLayouts   : SeqType.findAllByNameAndSingleCell(it.name, it.singleCell)*.libraryLayout.sort().join(MULTILINE_JOIN_STRING),
                    layouts          :
                            [
                                    SINGLE   : CollectionUtils.atMostOneElement(
                                            SeqType.findAllByNameAndLibraryLayoutAndSingleCell(it.name, SequencingReadType.SINGLE, it.singleCell)) ?
                                            true : false,
                                    PAIRED   : CollectionUtils.atMostOneElement(
                                            SeqType.findAllByNameAndLibraryLayoutAndSingleCell(it.name, SequencingReadType.PAIRED, it.singleCell)) ?
                                            true : false,
                                    MATE_PAIR: CollectionUtils.atMostOneElement(
                                            SeqType.findAllByNameAndLibraryLayoutAndSingleCell(it.name, SequencingReadType.MATE_PAIR, it.singleCell)) ?
                                            true : false,
                            ],
                    displayName      : it.displayName,
                    importAliases    : SeqType.findAllByName(it.name)*.importAlias?.flatten()?.unique()?.sort()?.join(MULTILINE_JOIN_STRING),
            ]
        }.unique().sort { it.name.toLowerCase() + it.id }
    }

    List<SeqType> list() {
        return SeqType.list()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<SeqType> alignableSeqTypesByProject(Project project) {
        return SeqTrack.createCriteria().listDistinct {
            projections {
                groupProperty('seqType')
                'in'('seqType', allAlignableSeqTypes)
                sample {
                    individual {
                        eq("project", project)
                    }
                }
            }
        }.sort { it.name }
    }

    static boolean hasSeqTypeByNameOrDisplayName(String nameOrDisplayName) {
        assert nameOrDisplayName: "the input nameOrDisplayName '${nameOrDisplayName}' is null"
        return SeqType.findAllByNameOrDisplayName(nameOrDisplayName, nameOrDisplayName).size() > 0
    }

    static boolean isSingleCell(String baseMaterial) {
        return baseMaterial in [SeqType.SINGLE_CELL_DNA, SeqType.SINGLE_CELL_RNA]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createMultiple(String name, List<SequencingReadType> libraryLayouts, Map properties = [:], List<String> importAliases = []) {
        assert libraryLayouts: "the input libraryLayout must not be empty"
        assert !libraryLayouts.findAll {
            it != SequencingReadType.SINGLE && it != SequencingReadType.PAIRED && it != SequencingReadType.MATE_PAIR
        }: "the input libraryLayout has invalid values"

        if (libraryLayouts.contains(SequencingReadType.SINGLE)) {
            create(name, properties + [libraryLayout: SequencingReadType.SINGLE], importAliases)
        }
        if (libraryLayouts.contains(SequencingReadType.PAIRED)) {
            create(name, properties + [libraryLayout: SequencingReadType.PAIRED], importAliases)
        }
        if (libraryLayouts.contains(SequencingReadType.MATE_PAIR)) {
            create(name, properties + [libraryLayout: SequencingReadType.MATE_PAIR], importAliases)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void changeLegacyState(SeqType seqType, boolean legacy) {
        SeqType.findAllByNameAndSingleCell(seqType.name, seqType.singleCell).each {
            it.legacy = legacy
            assert it.save(flush: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void toggleNeedsBedFileFlag(SeqType seqType) {
        seqType.needsBedFile = !seqType.needsBedFile
        seqType.save(flush: true)
    }

    @Override
    void getTAndAddAlias(String name, String importAlias) {
        List<SeqType> seqTypes = []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SequencingReadType.SINGLE, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SequencingReadType.SINGLE, singleCell: true]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SequencingReadType.PAIRED, singleCell: true]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SequencingReadType.MATE_PAIR, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SequencingReadType.MATE_PAIR, singleCell: true]) ?: []
        assert seqTypes: "No ${clazz} with name or importAlias ${name} exists"

        seqTypes.each { SeqType seqType ->
            if (seqType) {
                assert !seqType.importAlias.contains(importAlias): "the importAlias was already created"
                seqType.importAlias.add(importAlias)
                assert seqType.save(flush: true)
            }
        }
    }

    @Override
    protected SeqType findByName(String name, Map properties = [:]) {
        if (properties.libraryLayout && properties.singleCell != null) {
            return CollectionUtils.atMostOneElement(clazz.findAllByNameIlikeAndLibraryLayoutAndSingleCell(name, properties.libraryLayout as SequencingReadType,
                    properties.singleCell as boolean, [max: 1])) ?:
                    CollectionUtils.atMostOneElement(clazz.findAllByDisplayNameIlikeAndLibraryLayoutAndSingleCell(name,
                            properties.libraryLayout as SequencingReadType, properties.singleCell as boolean, [max: 1])) as SeqType
        }
        if (!properties.libraryLayout && properties.singleCell != null) {
            return CollectionUtils.atMostOneElement(clazz.findAllByNameIlikeAndSingleCell(name, properties.singleCell as boolean, [max: 1])) ?:
                    CollectionUtils.atMostOneElement(clazz.findAllByDisplayNameIlikeAndSingleCell(name, properties.singleCell as boolean, [max: 1]))
        }
        return CollectionUtils.atMostOneElement(clazz.findAllByName(name, [max: 1])) ?: CollectionUtils.atMostOneElement(clazz.findAllByDisplayName(name,
                [max: 1]))
    }

    @Override
    protected SeqType findByImportAlias(String importAlias, Map properties = [:]) {
        if (properties.libraryLayout && properties.singleCell != null) {
            return CollectionUtils.<SeqType> atMostOneElement(clazz.list().findAll {
                it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase()) &&
                        it.libraryLayout == properties.libraryLayout &&
                        it.singleCell == properties.singleCell
            })
        }
        return clazz.list().find {
            it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase())
        }
    }

    @Override
    protected void checkProperties(Map properties) {
        assert properties.dirName: "the input dirname must not be null"
        assert properties.libraryLayout: "the input libraryLayout must not be null"
        assert properties.libraryLayout instanceof SequencingReadType: "the input libraryLayout has to be of class 'LibraryLayout' but " +
                "is '${properties.libraryLayout.class}"
        assert properties.displayName: "the input displayName must not be null"
        assert properties.singleCell != null: "the input singleCell must not be null"
        assert !findByNameOrImportAlias(properties.displayName, properties): "The SeqType with displayname'${properties.displayName}' exists already"
    }

    @Override
    protected Class getClazz() {
        return SeqType
    }

    static List<SeqType> getAllSingleCellSeqTypes() {
        return SeqType.findAllBySingleCell(true)
    }

    static SeqType getWholeGenomePairedSeqType() {
        return getSingleSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, SequencingReadType.PAIRED, 'WGS PAIRED not found')
    }

    static SeqType getExomePairedSeqType() {
        return getSingleSeqType(SeqTypeNames.EXOME.seqTypeName, SequencingReadType.PAIRED, 'WES PAIRED not found')
    }

    static SeqType getWholeGenomeBisulfitePairedSeqType() {
        return getSingleSeqType(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, SequencingReadType.PAIRED, 'WGBS PAIRED not found')
    }

    static SeqType getWholeGenomeBisulfiteTagmentationPairedSeqType() {
        return getSingleSeqType(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, SequencingReadType.PAIRED,
                'WGBS_TAG PAIRED not found')
    }

    static SeqType getRnaPairedSeqType() {
        return getSingleSeqType(SeqTypeNames.RNA.seqTypeName, SequencingReadType.PAIRED, 'RNA PAIRED not found')
    }

    static SeqType getChipSeqPairedSeqType() {
        return getSingleSeqType(SeqTypeNames.CHIP_SEQ.seqTypeName, SequencingReadType.PAIRED, 'CHIP_SEQ PAIRED not found')
    }

    static SeqType getRnaSingleSeqType() {
        return getSingleSeqType(SeqTypeNames.RNA.seqTypeName, SequencingReadType.SINGLE, 'RNA SINGLE not found')
    }

    static SeqType get10xSingleCellRnaSeqType() {
        return getSingleSeqType(SeqTypeNames._10X_SCRNA.seqTypeName, SequencingReadType.PAIRED, true, '10x_scRNA PAIRED not found')
    }

    private static SeqType getSingleSeqType(String seqTypeName, SequencingReadType layout, boolean singleCell = false, String customErrorMessage) {
        return CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(seqTypeName, layout, singleCell), customErrorMessage)
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getDefaultOtpAlignableSeqTypes() {
        return [
                exomePairedSeqType,
                wholeGenomePairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getPanCanAlignableSeqTypes() {
        return [
                exomePairedSeqType,
                wholeGenomePairedSeqType,
                wholeGenomeBisulfitePairedSeqType,
                wholeGenomeBisulfiteTagmentationPairedSeqType,
                chipSeqPairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getRnaAlignableSeqTypes() {
        return [
                rnaPairedSeqType,
                rnaSingleSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getRoddyAlignableSeqTypes() {
        return [
                panCanAlignableSeqTypes,
                rnaAlignableSeqTypes,
        ].flatten()
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getCellRangerAlignableSeqTypes() {
        return [
                get10xSingleCellRnaSeqType(),
        ].flatten()
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getAllAlignableSeqTypes() {
        return [
                defaultOtpAlignableSeqTypes,
                roddyAlignableSeqTypes,
                cellRangerAlignableSeqTypes,
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesRequiredLibPrepKit() {
        return [
                wholeGenomeBisulfitePairedSeqType,
                wholeGenomeBisulfiteTagmentationPairedSeqType,
                chipSeqPairedSeqType,
                rnaPairedSeqType,
                rnaSingleSeqType,
        ] + SeqType.findAllByNeedsBedFile(true)
    }

    /**
     * search for seqTypes usable for trigger alignment and return them sorted
     */
    List<SeqType> findAlignAbleSeqTypes() {
        List<SeqType> seqTypes = SeqTypeService.roddyAlignableSeqTypes
        Workflow.list().findAll {
            it.beanName && !it.deprecatedDate && context.getBean(it.beanName, OtpWorkflow)?.isAlignment()
        }.each {
            seqTypes.addAll(it.supportedSeqTypes)
        }
        return seqTypes.unique().sort {
            SeqType a, SeqType b -> a.displayNameWithLibraryLayout <=> b.displayNameWithLibraryLayout
        }
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getSnvPipelineSeqTypes() {
        return [
                exomePairedSeqType,
                wholeGenomePairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getIndelPipelineSeqTypes() {
        return [
                exomePairedSeqType,
                wholeGenomePairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getSophiaPipelineSeqTypes() {
        return [
                exomePairedSeqType,
                wholeGenomePairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getAceseqPipelineSeqTypes() {
        return [
                wholeGenomePairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getRunYapsaPipelineSeqTypes() {
        return [
                exomePairedSeqType,
                wholeGenomePairedSeqType,
        ]
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getAllAnalysableSeqTypes() {
        return [
                snvPipelineSeqTypes,
                indelPipelineSeqTypes,
                sophiaPipelineSeqTypes,
                aceseqPipelineSeqTypes,
                runYapsaPipelineSeqTypes,
        ].flatten().unique()
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link Workflow#supportedSeqTypes} instead
     */
    @Deprecated
    static List<SeqType> getAllProcessableSeqTypes() {
        return [
                allAlignableSeqTypes,
                allAnalysableSeqTypes,
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesIgnoringLibraryPreparationKitForMerging() {
        return [
                wholeGenomeBisulfitePairedSeqType,
                wholeGenomeBisulfiteTagmentationPairedSeqType,
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesWithAntibodyTarget() {
        return SeqType.findAllByHasAntibodyTarget(true)
    }
}
