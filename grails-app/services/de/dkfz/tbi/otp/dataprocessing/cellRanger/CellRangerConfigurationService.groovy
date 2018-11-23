package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.validation.*
import groovy.transform.*
import org.springframework.security.access.prepost.*
import org.springframework.validation.*

class CellRangerConfigurationService {

    SeqType getSeqType() {
        CollectionUtils.exactlyOneElement(getPipeline().getSeqTypes())
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
            if (sampleType && it.sampleType != sampleType) {
                return false
            }
            return true
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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Errors createMergingWorkPackage(
            int expectedCells,
            Integer enforcedCells,
            Project project,
            Individual individual1,
            SampleType sampleType
    ) {
        List<Sample> samples = new ArrayList(getSamples(project, individual1, sampleType).selectedSamples)

        CellRangerConfig config = getWorkflowConfig(project)
        MergingCriteria mergingCriteria = getMergingCriteria(project)

        try {
            samples.unique().each { Sample sample ->
                Set<SeqTrack> seqTracks = new HashSet<SeqTrack>(sample.seqTracks)
                SeqPlatformGroup seqPlatformGroup
                LibraryPreparationKit libraryPreparationKit

                if (mergingCriteria.useSeqPlatformGroup != MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING) {
                    Map<SeqPlatformGroup, List<SeqTrack>> seqTracksByPlatformGroup = seqTracks.groupBy {
                        it.seqPlatformGroup
                    }
                    if (seqTracksByPlatformGroup.size() < 1) {
                        seqTracks = seqTracksByPlatformGroup.max { it.value.size() }.value
                    }
                    seqPlatformGroup = CollectionUtils.exactlyOneElement(seqTracks*.seqPlatformGroup.unique())
                } else {
                    seqPlatformGroup = null
                }

                if (mergingCriteria.useLibPrepKit) {
                    Map<LibraryPreparationKit, List<SeqTrack>> seqTracksByLibPrepKit = seqTracks.groupBy {
                        it.libraryPreparationKit
                    }
                    if (seqTracksByLibPrepKit.size() < 1) {
                        seqTracks = seqTracksByLibPrepKit.max { it.value.size() }.value
                    }
                    libraryPreparationKit = CollectionUtils.exactlyOneElement(seqTracks*.libraryPreparationKit.unique())
                } else {
                    libraryPreparationKit = null
                }

                CellRangerMergingWorkPackage mwp = new CellRangerMergingWorkPackage(
                        expectedCells: expectedCells,
                        enforcedCells: enforcedCells,
                        config: config,
                        needsProcessing: true,
                        seqPlatformGroup: seqPlatformGroup,
                        statSizeFileName: null,
                        seqTracks: seqTracks,
                        sample: sample,
                        seqType: seqType,
                        referenceGenome: config.referenceGenomeIndex.referenceGenome,
                        pipeline: pipeline,
                        antibodyTarget: null,
                        libraryPreparationKit: libraryPreparationKit,
                )
                mwp.save(flush: true)
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }
}
