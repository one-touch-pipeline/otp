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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.Canonical
import groovy.transform.Immutable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

@Transactional
class CellRangerConfigurationService {

    SeqType getSeqType() {
        CollectionUtils.exactlyOneElement(seqTypes)
    }

    List<SeqType> getSeqTypes() {
        getPipeline().getSeqTypes()
    }

    Pipeline getPipeline() {
        Pipeline.findByName(Pipeline.Name.CELL_RANGER)
    }

    MergingCriteria getMergingCriteria(Project project) {
        CollectionUtils.atMostOneElement(
                MergingCriteria.findAllByProjectAndSeqType(project, getSeqType())
        )
    }

    CellRangerConfig getWorkflowConfig(Project project) {
        CollectionUtils.atMostOneElement(
                CellRangerConfig.findAllWhere(
                        project: project,
                        seqType: seqType,
                        pipeline: pipeline,
                        obsoleteDate: null,
                )
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Samples getSamples(Project project, Individual ind, SampleType sampleType) {
        List<Sample> allSamples = Sample.createCriteria().list {
            seqTracks {
                eq("seqType", seqType)
            }
            individual {
                eq("project", project)
            }
        }

        List<Sample> selectedSamples = allSamples.findAll {
            if (ind && it.individual != ind) {
                return false
            }
            return (!sampleType || it.sampleType == sampleType)
        }

        return new Samples(
                allSamples,
                selectedSamples,
        )
    }

    @Immutable
    static class Samples {
        List<Sample> allSamples
        List<Sample> selectedSamples
    }

    @Canonical
    static class PlatformGroupAndKit {
        SeqPlatformGroup seqPlatformGroup
        LibraryPreparationKit libraryPreparationKit
    }

    Map<PlatformGroupAndKit, List<SeqTrack>> getSeqTracksGroupedByPlatformGroupAndKit(Collection<SeqTrack> seqTracks) {
        return seqTracks.groupBy { SeqTrack seqTrack ->
            [
                    seqPlatformGroup     : seqTrack.getSeqPlatformGroup(),
                    libraryPreparationKit: seqTrack.libraryPreparationKit,
            ]
        }.collectEntries { Map<String, Entity> key, List<SeqTrack> seqTracksPerPlatformGroupAndKit ->
            PlatformGroupAndKit platformGroupAndKit = new PlatformGroupAndKit(
                    seqPlatformGroup     : key['seqPlatformGroup'] as SeqPlatformGroup,
                    libraryPreparationKit: key['libraryPreparationKit'] as LibraryPreparationKit,
            )
            return [(platformGroupAndKit): seqTracksPerPlatformGroupAndKit]
        }
    }

    /**
     * Currently it is not supported to have SeqTracks of differing SeqPlatformGroups and LibPrepKits within one
     * Sample. For SingleCell data the import validator already throws an Error, see {@link MergingPreventionValidator}.
     */
    static void constrainSeqTracksGroupedByPlatformGroupAndKit(Map<PlatformGroupAndKit, List<SeqTrack>> map) {
        assert map.size() <= 1: "Can not handle SeqTracks processed over multiple platforms or with different library preparation kits"
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Errors createMergingWorkPackage(Integer expectedCells, Integer enforcedCells, ReferenceGenomeIndex referenceGenomeIndex, Project project, Individual individual, SampleType sampleType, SeqType seqType) {
        List<Sample> samples = new ArrayList(getSamples(project, individual, sampleType).selectedSamples)
        CellRangerConfig config = getWorkflowConfig(project)
        try {
            samples.unique().each { Sample sample ->
                Map<PlatformGroupAndKit, List<SeqTrack>> map = getSeqTracksGroupedByPlatformGroupAndKit(sample.seqTracks.findAll { it.seqType == seqType })
                constrainSeqTracksGroupedByPlatformGroupAndKit(map)
                map.each { PlatformGroupAndKit platformGroupAndKit, List<SeqTrack> seqTracks ->
                    new CellRangerMergingWorkPackage(
                            seqType              : seqType,
                            pipeline             : pipeline,
                            sample               : sample,
                            seqTracks            : seqTracks,
                            expectedCells        : expectedCells,
                            enforcedCells        : enforcedCells,
                            config               : config,
                            statSizeFileName     : null,
                            referenceGenomeIndex : referenceGenomeIndex,
                            referenceGenome      : referenceGenomeIndex.referenceGenome,
                            antibodyTarget       : null,
                            seqPlatformGroup     : platformGroupAndKit.seqPlatformGroup,
                            libraryPreparationKit: platformGroupAndKit.libraryPreparationKit,
                            needsProcessing      : true,
                    ).save(flush: true)
                }
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }
}
