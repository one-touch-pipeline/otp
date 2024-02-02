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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.*

import java.nio.file.Paths

abstract class AbstractAlignmentDeciderSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, FastqcDomainFactory, IsRoddy {

    protected AbstractAlignmentDecider decider
    protected int useFastqcCount

    protected SeqTrack seqTrack1
    protected SeqTrack seqTrack2
    protected List<SeqTrack> seqTracks
    protected List<FastqcProcessedFile> fastqcProcessedFiles
    protected MergingWorkPackage baseMergingWorkPackage
    protected SeqPlatformGroup seqPlatformGroup
    protected ReferenceGenome referenceGenome
    protected RoddyBamFile bamFile
    protected Workflow workflow
    protected WorkflowVersion workflowVersion

    protected ProjectSeqTypeGroup projectSeqTypeGroup
    protected AlignmentDeciderGroup alignmentDeciderGroup
    protected AlignmentArtefactDataList dataList
    protected AlignmentArtefactDataList additionalDataList
    protected AlignmentAdditionalData additionalData

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                WorkflowRunInputArtefact,
                WorkflowVersionSelector,
        ]
    }

    void "fetchInputArtefacts"() {
        given:
        Collection<WorkflowArtefact> inputArtefacts = [createWorkflowArtefact()]
        Set<SeqType> seqTypes = [createSeqType()] as Set
        SeqTrack seqTrack = createSeqTrack()

        AlignmentArtefactData<SeqTrack> seqTrackData = createAlignmentArtefactData(seqTrack)
        AlignmentArtefactData<FastqcProcessedFile> fastqcProcessedFileData = createAlignmentArtefactData()

        decider.alignmentArtefactService = Mock(AlignmentArtefactService) {
            0 * _
            1 * fetchSeqTrackArtefacts(inputArtefacts, seqTypes) >> [seqTrackData]
            useFastqcCount * fetchFastqcProcessedFileArtefacts(inputArtefacts, seqTypes) >> [fastqcProcessedFileData]
        }

        when:
        AlignmentArtefactDataList dataList = decider.fetchInputArtefacts(inputArtefacts, seqTypes)

        then:
        dataList.seqTrackData == [seqTrackData]
        dataList.fastqcProcessedFileData == (useFastqcCount ? [fastqcProcessedFileData] : [])
        dataList.bamData == []
    }

    void "fetchAdditionalArtefacts"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        AlignmentArtefactData<SeqTrack> seqTrackData = createAlignmentArtefactData(seqTrack)
        AlignmentArtefactData<FastqcProcessedFile> fastqcProcessedFileData = createAlignmentArtefactData()
        AlignmentArtefactData<RoddyBamFile> roddyBamFileData = createAlignmentArtefactData()

        AlignmentArtefactDataList dataList = new AlignmentArtefactDataList([seqTrackData], [fastqcProcessedFileData], [roddyBamFileData])

        AlignmentArtefactData<SeqTrack> seqTrackData2 = createAlignmentArtefactData()
        AlignmentArtefactData<FastqcProcessedFile> fastqcProcessedFileData2 = createAlignmentArtefactData()
        AlignmentArtefactData<RoddyBamFile> roddyBamFileData2 = createAlignmentArtefactData()

        decider.alignmentArtefactService = Mock(AlignmentArtefactService) {
            0 * _
            1 * fetchRelatedSeqTrackArtefactsForSeqTracks([seqTrack]) >> [seqTrackData, seqTrackData2]
            useFastqcCount * fetchRelatedFastqcArtefactsForSeqTracks([seqTrack]) >> [fastqcProcessedFileData, fastqcProcessedFileData2]
            1 * fetchRelatedBamFileArtefactsForSeqTracks([seqTrack]) >> [roddyBamFileData, roddyBamFileData2]
        }

        when:
        AlignmentArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.seqTrackData == [seqTrackData2]
        dataList2.fastqcProcessedFileData == (useFastqcCount ? [fastqcProcessedFileData2] : [])
        dataList2.bamData == [roddyBamFileData2]
    }

    void "fetchAdditionalData"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        RoddyBamFile roddyBamFile = createBamFile()
        Pipeline pipeline = findPipeline()

        ProjectSeqTypeGroup projectSeqTypeGroup = new ProjectSeqTypeGroup(seqTrack.project, seqTrack.seqType)

        and: 'objects for fetchReferenceGenome'
        SpeciesWithStrain speciesWithStrain = findOrCreateHumanSpecies()
        ReferenceGenome referenceGenome = createReferenceGenome([
                speciesWithStrain: [speciesWithStrain] as Set,
                species          : [] as Set,
        ])
        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = [
                (projectSeqTypeGroup): [
                        ([speciesWithStrain] as Set): referenceGenome,
                ],
        ]

        and: 'objects for fetchMergingCriteria'
        MergingCriteria mergingCriteria = createMergingCriteria()
        Map<ProjectSeqTypeGroup, MergingCriteria> mergingCriteriaMap = [
                (projectSeqTypeGroup): [mergingCriteria],
        ]

        and: 'objects for fetchSeqPlatformGroup'
        SeqPlatformGroup specificSeqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform specificSeqPlatform = createSeqPlatform()
        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> specificSeqPlatformGroupMap = [
                (projectSeqTypeGroup): [
                        (specificSeqPlatform): specificSeqPlatformGroup,
                ],
        ]

        and: 'objects for fetchDefaultSeqPlatformGroup'
        SeqPlatformGroup defaultSeqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform defaultSeqPlatform = createSeqPlatform()
        Map<SeqPlatform, SeqPlatformGroup> defaultSeqPlatformGroupMap = [
                (defaultSeqPlatform): defaultSeqPlatformGroup,
        ]

        and: 'objects for fetchMergingWorkPackage'
        AlignmentWorkPackageGroup alignmentWorkPackageGroup = new AlignmentWorkPackageGroup(roddyBamFile.sample, roddyBamFile.seqType, roddyBamFile.workPackage.antibodyTarget)
        Map<AlignmentWorkPackageGroup, MergingWorkPackage> mergingWorkPackageMap = [
                (alignmentWorkPackageGroup): roddyBamFile.workPackage,
        ]

        and: 'objects for fetchRawSequenceFiles'
        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = [
                (seqTrack): seqTrack.sequenceFiles,
        ]

        and: 'input objects'
        AlignmentArtefactData<SeqTrack> seqTrackData = createAlignmentArtefactData(seqTrack)
        AlignmentArtefactData<FastqcProcessedFile> fastqcProcessedFileData = createAlignmentArtefactData()
        AlignmentArtefactData<RoddyBamFile> roddyBamFileData = createAlignmentArtefactData()
        AlignmentArtefactDataList dataList = new AlignmentArtefactDataList([seqTrackData], [fastqcProcessedFileData], [roddyBamFileData])

        and: 'mocked services'
        decider.alignmentArtefactService = Mock(AlignmentArtefactService) {
            0 * _
            1 * fetchReferenceGenome(workflow, [seqTrack]) >> referenceGenomeMap
            1 * fetchMergingCriteria([seqTrack]) >> mergingCriteriaMap
            1 * fetchSpecificSeqPlatformGroup([seqTrack]) >> specificSeqPlatformGroupMap
            1 * fetchDefaultSeqPlatformGroup() >> defaultSeqPlatformGroupMap
            1 * fetchMergingWorkPackage([seqTrack]) >> mergingWorkPackageMap
            useFastqcCount * fetchRawSequenceFiles([seqTrack]) >> rawSequenceFileMap
        }
        decider.pipelineService = Mock(PipelineService) {
            0 * _
            1 * findByPipelineName(_) >> pipeline
        }

        when:
        AlignmentAdditionalData alignmentAdditionalData = decider.fetchAdditionalData(dataList, workflow)

        then:
        alignmentAdditionalData.referenceGenomeMap == referenceGenomeMap
        alignmentAdditionalData.mergingCriteriaMap == mergingCriteriaMap
        alignmentAdditionalData.specificSeqPlatformGroupMap == specificSeqPlatformGroupMap
        alignmentAdditionalData.defaultSeqPlatformGroupMap == defaultSeqPlatformGroupMap
        alignmentAdditionalData.mergingWorkPackageMap == mergingWorkPackageMap
        alignmentAdditionalData.rawSequenceFileMap == (useFastqcCount ? rawSequenceFileMap : [:])
        alignmentAdditionalData.pipeline == pipeline
    }

    void "fetchWorkflowVersionSelector"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)
        SeqTrack seqTrack = createSeqTrack()
        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector()

        and: 'input objects'
        AlignmentArtefactData<SeqTrack> seqTrackData = createAlignmentArtefactData(seqTrack)
        AlignmentArtefactData<FastqcProcessedFile> fastqcProcessedFileData = createAlignmentArtefactData()
        AlignmentArtefactData<RoddyBamFile> roddyBamFileData = createAlignmentArtefactData()
        AlignmentArtefactDataList dataList = new AlignmentArtefactDataList([seqTrackData], [fastqcProcessedFileData], [roddyBamFileData])

        and: 'mocked services'
        decider.alignmentArtefactService = Mock(AlignmentArtefactService) {
            0 * _
            1 * fetchWorkflowVersionSelectorForSeqTracks(workflow, [seqTrack]) >> [workflowVersionSelector]
        }

        when:
        List<WorkflowVersionSelector> workflowVersionSelectorList = decider.fetchWorkflowVersionSelector(dataList, workflow)

        then:
        workflowVersionSelectorList == [workflowVersionSelector]
    }

    void "groupData, one SeqTrack"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        Project project = seqTrack.project
        SeqType seqType = seqTrack.seqType
        SeqPlatform seqPlatform = seqTrack.seqPlatform

        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup([seqPlatforms: [seqPlatform]])
        List<FastqcProcessedFile> fastqcProcessedFiles = seqTrack.sequenceFiles.collect {
            createFastqcProcessedFile([sequenceFile: it])
        }

        and: 'AlignmentArtefactDataList'
        List<AlignmentArtefactData<SeqTrack>> seqTrackData = [createAlignmentArtefactDataForSeqTrack(seqTrack)]

        List<AlignmentArtefactData<FastqcProcessedFile>> fastqcProcessedFileData = fastqcProcessedFiles.collect { FastqcProcessedFile fastqcProcessedFile ->
            createAlignmentArtefactDataForFastqcProcessedFile(fastqcProcessedFile)
        }
        List<AlignmentArtefactData<RoddyBamFile>> roddyBamFileData = []
        AlignmentArtefactDataList dataList = new AlignmentArtefactDataList(seqTrackData, fastqcProcessedFileData, roddyBamFileData)

        and: 'additional data'
        ProjectSeqTypeGroup projectSeqTypeGroup = new ProjectSeqTypeGroup(project, seqType)

        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = [:]

        Map<ProjectSeqTypeGroup, MergingCriteria> mergingCriteriaMap = [
                (projectSeqTypeGroup): createMergingCriteria([project: project, seqType: seqType]),
        ]

        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> specificSeqPlatformGroupMap = [:]
        Map<SeqPlatform, SeqPlatformGroup> defaultSeqPlatformGroupMap = [
                (seqPlatform): seqPlatformGroup,
        ]
        Map<AlignmentWorkPackageGroup, MergingWorkPackage> mergingWorkPackageMap = [:]
        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = [
                (seqTrack): seqTrack.sequenceFiles
        ]

        AlignmentAdditionalData additionalData = new AlignmentAdditionalData(referenceGenomeMap,
                mergingCriteriaMap,
                specificSeqPlatformGroupMap,
                defaultSeqPlatformGroupMap,
                mergingWorkPackageMap,
                rawSequenceFileMap,
                findOrCreatePanCanPipeline())

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        AlignmentDeciderGroup alignmentDeciderGroup = createAlignmentDeciderGroup(seqTrack, seqPlatformGroup)

        when:
        Map<AlignmentDeciderGroup, AlignmentArtefactDataList> alignmentArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        alignmentArtefactDataListMap.size() == 1
        alignmentArtefactDataListMap.keySet().first() == alignmentDeciderGroup
        assertAlignmentArtefactDataList(alignmentArtefactDataListMap.values().first(), dataList)
    }

    void "groupData, two SeqTracks with same referenced data"() {
        given:
        SeqTrack seqTrack1 = createSeqTrackWithTwoFastqFile()
        Project project = seqTrack1.project
        SeqType seqType = seqTrack1.seqType
        SeqPlatform seqPlatform = seqTrack1.seqPlatform
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup([seqPlatforms: [seqPlatform]])
        List<FastqcProcessedFile> fastqcProcessedFiles1 = seqTrack1.sequenceFiles.collect {
            createFastqcProcessedFile([sequenceFile: it])
        }

        and: 'create second seqtrack'
        SeqTrack seqTrack2 = createSeqTrackWithTwoFastqFile([
                sample               : seqTrack1.sample,
                seqType              : seqTrack1.seqType,
                libraryPreparationKit: seqTrack1.libraryPreparationKit,
                run                  : seqTrack1.run,
        ])
        List<FastqcProcessedFile> fastqcProcessedFiles2 = seqTrack2.sequenceFiles.collect {
            createFastqcProcessedFile([sequenceFile: it])
        }

        and: 'AlignmentArtefactDataList'
        List<AlignmentArtefactData<SeqTrack>> seqTrackData = [
                createAlignmentArtefactDataForSeqTrack(seqTrack1),
                createAlignmentArtefactDataForSeqTrack(seqTrack2),
        ]

        List<AlignmentArtefactData<FastqcProcessedFile>> fastqcProcessedFileData = [
                fastqcProcessedFiles1,
                fastqcProcessedFiles2,
        ].collectMany {
            it.collect { FastqcProcessedFile fastqcProcessedFile ->
                createAlignmentArtefactDataForFastqcProcessedFile(fastqcProcessedFile)
            }
        }
        List<AlignmentArtefactData<RoddyBamFile>> roddyBamFileData = []
        AlignmentArtefactDataList dataList = new AlignmentArtefactDataList(seqTrackData, fastqcProcessedFileData, roddyBamFileData)

        and: 'additional data'
        ProjectSeqTypeGroup projectSeqTypeGroup = new ProjectSeqTypeGroup(project, seqType)

        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = [:]

        Map<ProjectSeqTypeGroup, MergingCriteria> mergingCriteriaMap = [
                (projectSeqTypeGroup): createMergingCriteria([project: project, seqType: seqType]),
        ]

        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> specificSeqPlatformGroupMap = [:]

        Map<SeqPlatform, SeqPlatformGroup> defaultSeqPlatformGroupMap = [
                (seqPlatform): seqPlatformGroup,
        ]

        Map<AlignmentWorkPackageGroup, MergingWorkPackage> mergingWorkPackageMap = [:]

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = [
                (seqTrack1): seqTrack1.sequenceFiles,
                (seqTrack2): seqTrack2.sequenceFiles,
        ]

        AlignmentAdditionalData additionalData = new AlignmentAdditionalData(referenceGenomeMap,
                mergingCriteriaMap,
                specificSeqPlatformGroupMap,
                defaultSeqPlatformGroupMap,
                mergingWorkPackageMap,
                rawSequenceFileMap,
                findOrCreatePanCanPipeline())

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        AlignmentDeciderGroup alignmentDeciderGroup = createAlignmentDeciderGroup(seqTrack1, seqPlatformGroup)

        when:
        Map<AlignmentDeciderGroup, AlignmentArtefactDataList> alignmentArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        alignmentArtefactDataListMap.size() == 1
        alignmentArtefactDataListMap.keySet().first() == alignmentDeciderGroup
        assertAlignmentArtefactDataList(alignmentArtefactDataListMap.values().first(), dataList)
    }

    void "groupData, two SeqTracks without shared references"() {
        given: 'first seqTrack'
        SeqTrack seqTrack1 = createSeqTrackWithTwoFastqFile()
        Project project1 = seqTrack1.project
        SeqType seqType1 = seqTrack1.seqType
        SeqPlatform seqPlatform1 = seqTrack1.seqPlatform
        SeqPlatformGroup seqPlatformGroup1 = createSeqPlatformGroup([seqPlatforms: [seqPlatform1]])
        List<FastqcProcessedFile> fastqcProcessedFiles1 = seqTrack1.sequenceFiles.collect {
            createFastqcProcessedFile([sequenceFile: it])
        }

        and: 'second seqTrack'
        SeqTrack seqTrack2 = createSeqTrackWithTwoFastqFile()
        Project project2 = seqTrack2.project
        SeqType seqType2 = seqTrack2.seqType
        SeqPlatform seqPlatform2 = seqTrack2.seqPlatform
        SeqPlatformGroup seqPlatformGroup2 = createSeqPlatformGroup([seqPlatforms: [seqPlatform2]])

        List<FastqcProcessedFile> fastqcProcessedFiles2 = seqTrack2.sequenceFiles.collect {
            createFastqcProcessedFile([sequenceFile: it])
        }

        and: 'AlignmentArtefactDataList 1'
        List<AlignmentArtefactData<SeqTrack>> seqTrackData1 = [createAlignmentArtefactDataForSeqTrack(seqTrack1)]
        List<AlignmentArtefactData<FastqcProcessedFile>> fastqcProcessedFileData1 = fastqcProcessedFiles1.collect { FastqcProcessedFile fastqcProcessedFile ->
            createAlignmentArtefactDataForFastqcProcessedFile(fastqcProcessedFile)
        }
        AlignmentArtefactDataList dataList1 = new AlignmentArtefactDataList(seqTrackData1, fastqcProcessedFileData1, [])

        and: 'AlignmentArtefactDataList 2'
        List<AlignmentArtefactData<SeqTrack>> seqTrackData2 = [createAlignmentArtefactDataForSeqTrack(seqTrack2)]
        List<AlignmentArtefactData<FastqcProcessedFile>> fastqcProcessedFileData2 = fastqcProcessedFiles2.collect { FastqcProcessedFile fastqcProcessedFile ->
            createAlignmentArtefactDataForFastqcProcessedFile(fastqcProcessedFile)
        }
        AlignmentArtefactDataList dataList2 = new AlignmentArtefactDataList(seqTrackData2, fastqcProcessedFileData2, [])

        and: 'AlignmentArtefactDataList together'
        AlignmentArtefactDataList dataListTogether = new AlignmentArtefactDataList(
                seqTrackData1 + seqTrackData2,
                fastqcProcessedFileData1 + fastqcProcessedFileData2,
                []
        )

        and: 'additional data'
        ProjectSeqTypeGroup projectSeqTypeGroup1 = new ProjectSeqTypeGroup(project1, seqType1)
        ProjectSeqTypeGroup projectSeqTypeGroup2 = new ProjectSeqTypeGroup(project2, seqType2)

        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = [:]

        Map<ProjectSeqTypeGroup, MergingCriteria> mergingCriteriaMap = [
                (projectSeqTypeGroup1): createMergingCriteria([project: project1, seqType: seqType1]),
                (projectSeqTypeGroup2): createMergingCriteria([project: project2, seqType: seqType2]),
        ]

        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> specificSeqPlatformGroupMap = [:]
        Map<SeqPlatform, SeqPlatformGroup> defaultSeqPlatformGroupMap = [
                (seqPlatform1): seqPlatformGroup1,
                (seqPlatform2): seqPlatformGroup2,
        ]
        Map<AlignmentWorkPackageGroup, MergingWorkPackage> mergingWorkPackageMap = [:]
        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = [
                (seqTrack1): seqTrack1.sequenceFiles,
                (seqTrack2): seqTrack2.sequenceFiles,
        ]

        AlignmentAdditionalData additionalData = new AlignmentAdditionalData(
                referenceGenomeMap,
                mergingCriteriaMap,
                specificSeqPlatformGroupMap,
                defaultSeqPlatformGroupMap,
                mergingWorkPackageMap,
                rawSequenceFileMap,
                findOrCreatePanCanPipeline()
        )

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        AlignmentDeciderGroup alignmentDeciderGroup1 = createAlignmentDeciderGroup(seqTrack1, seqPlatformGroup1)
        AlignmentDeciderGroup alignmentDeciderGroup2 = createAlignmentDeciderGroup(seqTrack2, seqPlatformGroup2)

        expect: 'check input'
        dataList1.seqTrackData.size() == 1
        dataList1.fastqcProcessedFileData.size() == 2
        dataList2.seqTrackData.size() == 1
        dataList2.fastqcProcessedFileData.size() == 2
        dataListTogether.seqTrackData.size() == 2
        dataListTogether.fastqcProcessedFileData.size() == 4

        when:
        Map<AlignmentDeciderGroup, AlignmentArtefactDataList> alignmentArtefactDataListMap = decider.groupData(dataListTogether, additionalData, userParams)

        then:
        alignmentArtefactDataListMap.size() == 2
        alignmentArtefactDataListMap.containsKey(alignmentDeciderGroup1)
        alignmentArtefactDataListMap.containsKey(alignmentDeciderGroup2)
        assertAlignmentArtefactDataList(alignmentArtefactDataListMap[alignmentDeciderGroup1], dataList1)
        assertAlignmentArtefactDataList(alignmentArtefactDataListMap[alignmentDeciderGroup2], dataList2)
    }

    @SuppressWarnings("NestedBlockDepth")
    void "groupData all data combination"() {
        given:
        List<SpeciesWithStrain> speciesWithStrains = (1..2).collect {
            findOrCreateHumanSpecies()
        }

        Map<SpeciesWithStrain, ReferenceGenome> referenceGenomesMap = speciesWithStrains.collectEntries {
            [(it): createReferenceGenome([
                    speciesWithStrain: [it] as Set,
                    species          : [] as Set,
            ])]
        }

        and: 'data to group'
        List<Project> projects = (1..2).collect {
            createProject()
        }
        List<SeqType> seqTypes = [false, true].collectMany { boolean antibodyTarget ->
            (1..2).collect {
                createSeqType([
                        hasAntibodyTarget: antibodyTarget,
                ])
            }
        }
        List<SampleType> sampleTypes = (1..2).collect {
            createSampleType()
        }
        List<AntibodyTarget> antibodyTargets = (1..2).collect {
            createAntibodyTarget()
        }
        List<LibraryPreparationKit> libraryPreparationKits = (1..2).collect {
            createLibraryPreparationKit()
        } + [null]
        List<SeqPlatformGroup> seqPlatformGroups = (1..2).collect {
            createSeqPlatformGroup([
                    seqPlatforms: [
                            createSeqPlatform(),
                            createSeqPlatform(),
                    ],
            ])
        }
        List<SeqPlatform> seqPlatforms = seqPlatformGroups.collectMany {
            it.seqPlatforms
        }
        List<Individual> individuals = projects.collectMany { Project project ->
            speciesWithStrains.collectMany { SpeciesWithStrain speciesWithStrain ->
                (1..2).collect {
                    createIndividual([
                            project: project,
                            species: speciesWithStrain,
                    ])
                }
            }
        }
        List<Sample> samples = individuals.collectMany { Individual individual ->
            sampleTypes.collect { SampleType sampleType ->
                createSample([
                        individual: individual,
                        sampleType: sampleType,
                ])
            }
        }
        projects.collectMany { Project project ->
            seqTypes.collect { SeqType seqType ->
                createMergingCriteria([
                        project: project,
                        seqType: seqType,
                ])
            }
        }
        List<SeqTrack> seqTracks = samples.collectMany { Sample sample ->
            seqTypes.collectMany { SeqType seqType ->
                (seqType.hasAntibodyTarget ? antibodyTargets : [null]).collectMany { AntibodyTarget antibodyTarget ->
                    libraryPreparationKits.collectMany { LibraryPreparationKit libraryPreparationKit ->
                        seqPlatforms.collectMany { SeqPlatform seqPlatform ->
                            (1..2).collect {
                                createSeqTrack([
                                        sample               : sample,
                                        seqType              : seqType,
                                        antibodyTarget       : antibodyTarget,
                                        libraryPreparationKit: libraryPreparationKit,
                                        run                  : createRun([
                                                seqPlatform: seqPlatform,
                                        ]),
                                ])
                            }
                        }
                    }
                }
            }
        }
        List<FastqcProcessedFile> fastqcProcessedFiles = useFastqcCount ? seqTracks.collectMany { SeqTrack seqTrack ->
            (1..2).collect { int mate ->
                // don't validate/save objects because it is very slow with lots of objects
                // (not needed because objects are passed directly and not queried from the database)
                createFastqcProcessedFile([
                        sequenceFile: createFastqFile([
                                seqTrack  : seqTrack,
                                mateNumber: mate,
                        ], false)
                ], false)
            }
        } : []

        and: 'input objects'
        List<AlignmentArtefactData<SeqTrack>> seqTrackData = seqTracks.collect { SeqTrack seqTrack ->
            createAlignmentArtefactDataForSeqTrack(seqTrack)
        }
        List<AlignmentArtefactData<FastqcProcessedFile>> fastqcProcessedFileData = fastqcProcessedFiles.collect { FastqcProcessedFile fastqcProcessedFile ->
            createAlignmentArtefactDataForFastqcProcessedFile(fastqcProcessedFile)
        }
        List<AlignmentArtefactData<RoddyBamFile>> roddyBamFileData = []
        AlignmentArtefactDataList dataList = new AlignmentArtefactDataList(seqTrackData, fastqcProcessedFileData, roddyBamFileData)

        and: 'additional data'
        List<ProjectSeqTypeGroup> projectSeqTypeGroups = projects.collectMany { Project project ->
            seqTypes.collect { SeqType seqType ->
                new ProjectSeqTypeGroup(project, seqType)
            }
        }
        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = projectSeqTypeGroups.collectEntries { ProjectSeqTypeGroup projectSeqTypeGroup ->
            [(projectSeqTypeGroup): referenceGenomesMap.collectEntries { SpeciesWithStrain speciesWithStrain, ReferenceGenome referenceGenome ->
                [([speciesWithStrain] as Set): referenceGenome]
            }]
        }
        Map<ProjectSeqTypeGroup, MergingCriteria> mergingCriteriaMap = projectSeqTypeGroups.collectEntries { ProjectSeqTypeGroup projectSeqTypeGroup ->
            [(projectSeqTypeGroup): createMergingCriteria([project: projectSeqTypeGroup.project, seqType: projectSeqTypeGroup.seqType])]
        }
        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> specificSeqPlatformGroupMap = [:]
        Map<SeqPlatform, SeqPlatformGroup> defaultSeqPlatformGroupMap = seqPlatformGroups.collectEntries { SeqPlatformGroup seqPlatformGroup ->
            seqPlatformGroup.seqPlatforms.collectEntries { SeqPlatform seqPlatform ->
                [(seqPlatform): seqPlatformGroup]
            }
        }

        Map<AlignmentWorkPackageGroup, MergingWorkPackage> mergingWorkPackageMap = [:]
        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = seqTracks.collectEntries {
            [(it): it.sequenceFiles]
        }

        AlignmentAdditionalData additionalData = new AlignmentAdditionalData(referenceGenomeMap,
                mergingCriteriaMap,
                specificSeqPlatformGroupMap,
                defaultSeqPlatformGroupMap,
                mergingWorkPackageMap,
                rawSequenceFileMap,
                findOrCreatePanCanPipeline())

        and: 'set useParams'
        Map<String, String> userParams = [:]

        and: 'expected alignment groups'
        Map<AlignmentDeciderGroup, AlignmentArtefactData<SeqTrack>> fastqDeciderGroups = seqTrackData.groupBy {
            createAlignmentDeciderGroup(it.artefact, defaultSeqPlatformGroupMap[it.seqPlatform])
        }
        Map<AlignmentDeciderGroup, AlignmentArtefactData<FastqcProcessedFile>> fastqcDeciderGroups = fastqcProcessedFileData.groupBy {
            SeqTrack seqTrack = it.artefact.sequenceFile.seqTrack
            createAlignmentDeciderGroup(seqTrack, defaultSeqPlatformGroupMap[seqTrack.seqPlatform])
        }

        when:
        Map<AlignmentDeciderGroup, AlignmentArtefactDataList> alignmentArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        TestCase.assertContainSame(alignmentArtefactDataListMap.keySet(), fastqDeciderGroups.keySet())
        alignmentArtefactDataListMap.each { AlignmentDeciderGroup group, AlignmentArtefactDataList list ->
            TestCase.assertContainSame(list.seqTrackData, fastqDeciderGroups[group])
            TestCase.assertContainSame(list.fastqcProcessedFileData, fastqcDeciderGroups[group] ?: [])
            TestCase.assertContainSame(list.bamData, [])
        }
    }

    @Unroll
    void "createAlignmentDeciderGroup, case: #name"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        MergingCriteria mergingCriteria = createMergingCriteria([
                project            : seqTrack.project,
                seqType            : seqTrack.seqType,
                useLibPrepKit      : useLibPrepKit,
                useSeqPlatformGroup: useSeqPlatformGroup,
        ])

        SeqPlatformGroup seqPlatformGroupDefault = createSeqPlatformGroup([seqPlatforms: [seqTrack.seqPlatform]])
        SeqPlatformGroup seqPlatformGroupSpecific = createSeqPlatformGroup([seqPlatforms: [seqTrack.seqPlatform], mergingCriteria: mergingCriteria])

        AlignmentArtefactData<SeqTrack> data = createAlignmentArtefactDataForSeqTrack(seqTrack)

        and: 'additional data'
        ProjectSeqTypeGroup projectSeqTypeGroup = new ProjectSeqTypeGroup(seqTrack.project, seqTrack.seqType)

        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = [:]

        Map<ProjectSeqTypeGroup, MergingCriteria> mergingCriteriaMap = [
                (projectSeqTypeGroup): mergingCriteria,
        ]

        Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> specificSeqPlatformGroupMap = [
                (projectSeqTypeGroup): [
                        (seqTrack.seqPlatform): seqPlatformGroupSpecific,
                ]
        ]
        Map<SeqPlatform, SeqPlatformGroup> defaultSeqPlatformGroupMap = [
                (seqTrack.seqPlatform): seqPlatformGroupDefault,
        ]
        Map<AlignmentWorkPackageGroup, MergingWorkPackage> mergingWorkPackageMap = [:]
        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = [:]

        AlignmentAdditionalData additionalData = new AlignmentAdditionalData(
                referenceGenomeMap,
                mergingCriteriaMap,
                specificSeqPlatformGroupMap,
                defaultSeqPlatformGroupMap,
                mergingWorkPackageMap,
                rawSequenceFileMap,
                findOrCreatePanCanPipeline()
        )

        and: "expected"
        SeqPlatformGroup seqPlatformGroupExpected = expectGroup == 'default' ? seqPlatformGroupDefault : expectGroup == 'project' ? seqPlatformGroupSpecific : null
        LibraryPreparationKit libraryPreparationKitExpected = useLibPrepKit ? seqTrack.libraryPreparationKit : null

        when:
        AlignmentDeciderGroup group = decider.createAlignmentDeciderGroup(data, ignoreSeqPlatformGroup, additionalData, false)

        then:
        group.individual == seqTrack.individual
        group.seqType == seqTrack.seqType
        group.sampleType == seqTrack.sampleType
        group.sample == seqTrack.sample
        group.antibodyTarget == seqTrack.antibodyTarget
        group.libraryPreparationKit == libraryPreparationKitExpected
        group.seqPlatformGroup == seqPlatformGroupExpected

        where:
        ignoreSeqPlatformGroup | useLibPrepKit | useSeqPlatformGroup                                                     || expectGroup
        false                  | true          | MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               || 'default'
        false                  | true          | MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC || 'project'
        false                  | true          | MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            || 'ignore'
        false                  | false         | MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               || 'default'
        false                  | false         | MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC || 'project'
        false                  | false         | MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            || 'ignore'
        true                   | true          | MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               || 'ignore'
        true                   | true          | MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC || 'ignore'
        true                   | true          | MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            || 'ignore'
        true                   | false         | MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               || 'ignore'
        true                   | false         | MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC || 'ignore'
        true                   | false         | MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            || 'ignore'

        name = "ignoreSeqPlatformGroup: ${ignoreSeqPlatformGroup} useLibPrepKit: ${useLibPrepKit} useSeqPlatformGroup: ${useSeqPlatformGroup} expectGroup: ${expectGroup}"
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #name, then create new bam file and add no warning"() {
        given:
        createDataForCreateWorkflowRunsAndOutputArtefacts(existingMergingWorkPackage, [
                existingBamFileOtherSeqTracks: existingBamFileOtherSeqTracks,
                noSeqPlatformGroupSeqTrack   : noSeqPlatformGroupSeqTrack,
                noSeqPlatformGroupMwp        : noSeqPlatformGroupMwp,
        ])

        and: 'expected input artefacts'
        List<SeqTrack> expectedSeqTracks = seqTracks
        List<WorkflowArtefact> expectedInputArtefacts = (seqTracks + fastqcProcessedFiles)*.workflowArtefact

        and: 'services'
        createServicesForCreateWorkflowRunsAndOutputArtefacts(workflowVersion, seqTrack1)

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(projectSeqTypeGroup, alignmentDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion)

        then:
        deciderResult.newArtefacts.size() == 1
        deciderResult.warnings.empty

        WorkflowArtefact workflowArtefact = deciderResult.newArtefacts[0]
        workflowArtefact.artefactType == ArtefactType.BAM
        workflowArtefact.outputRole == decider.outputBamRole

        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.list()
        roddyBamFiles.size() == (existingBamFileOtherSeqTracks ? 2 : 1)
        RoddyBamFile bamFile = roddyBamFiles.last()
        bamFile.workflowArtefact == workflowArtefact
        CollectionUtils.containSame(bamFile.seqTracks, expectedSeqTracks)
        CollectionUtils.containSame(bamFile.containedSeqTracks, seqTracks)
        bamFile.numberOfMergedLanes == 2

        MergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        mergingWorkPackage.sample == seqTrack1.sample
        mergingWorkPackage.seqType == seqTrack1.seqType
        mergingWorkPackage.antibodyTarget == seqTrack1.antibodyTarget
        mergingWorkPackage.libraryPreparationKit == seqTrack1.libraryPreparationKit
        noSeqPlatformGroupMwp || noSeqPlatformGroupSeqTrack || mergingWorkPackage.seqPlatformGroup == seqPlatformGroup
        mergingWorkPackage.referenceGenome == referenceGenome
        TestCase.assertContainSame(mergingWorkPackage.seqTracks, seqTracks)
        !existingMergingWorkPackage || (mergingWorkPackage == baseMergingWorkPackage)

        WorkflowRun run = workflowArtefact.producedBy
        run.workflow == workflow
        run.workflowVersion == workflowVersion

        TestCase.assertContainSame(run.inputArtefacts.values(), expectedInputArtefacts)

        where:
        name                                                  | existingMergingWorkPackage | existingBamFileOtherSeqTracks | noSeqPlatformGroupSeqTrack | noSeqPlatformGroupMwp
        'no mergingWorkPackage'                               | false                      | false                         | false                      | false
        'mergingWorkPackage exist, but no bam file'           | true                       | false                         | false                      | false
        'bam file with other seqtracks exist'                 | true                       | true                          | false                      | false
        'no seqplatformgroup for SeqTrack & MWP do not exist' | false                      | false                         | true                       | false
        'no seqplatformgroup for SeqTrack & MWP  exist'       | true                       | false                         | true                       | false
        'no seqplatformgroup for MWP'                         | true                       | false                         | false                      | true
        'no seqplatformgroup for MWP & SeqTrack'              | true                       | false                         | true                       | true
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #name, then do not create a new bam file and create a warning"() {
        given:
        createDataForCreateWorkflowRunsAndOutputArtefacts(createMwp, [(key): value])

        and: 'services'
        createEmptyServicesForCreateWorkflowRunsAndOutputArtefacts(mailCount)

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(projectSeqTypeGroup, alignmentDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion)

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 1
        deciderResult.warnings.first().contains(warningMessagePart)

        where:
        name                                    | createMwp | key                            | value | mailCount || warningMessagePart
        'no seqtracks'                          | false     | 'noSeqTrack'                   | true  | 0         || "since no seqTracks"
        'existing BAM file with same seqTracks' | true      | 'existingBamFileSameSeqTracks' | true  | 0         || "since existing BAM file with the same seqTracks found"
        'no species'                            | false     | 'noSpecies'                    | true  | 0         || "since no species is defined for individual"
        'no reference genome'                   | false     | 'noReferenceGenome'            | true  | 0         || "since no reference genome is configured for"
        'wrong library preparation kit'         | true      | 'wrongLibPrepKit'              | true  | 1         || "since existing MergingWorkPackage and Lanes do not match"
        'wrong seqplatformgroup'                | true      | 'wrongSeqPlatformGroup'        | true  | 1         || "since existing MergingWorkPackage and Lanes do not match"
        'wrong referenceGenome'                 | true      | 'wrongReferenceGenome'         | true  | 0         || "since existing MergingWorkPackage uses ReferenceGenome"
    }

    private Pipeline findOrCreatePanCanPipeline() {
        return findOrCreateDomainObject(Pipeline, [:], [
                name: Pipeline.Name.PANCAN_ALIGNMENT,
                type: Pipeline.Type.ALIGNMENT,
        ])
    }

    protected AlignmentDeciderGroup createAlignmentDeciderGroup(SeqTrack seqTrack, SeqPlatformGroup seqPlatformGroup) {
        return new AlignmentDeciderGroup(
                seqTrack.individual,
                seqTrack.seqType,
                seqTrack.sampleType,
                seqTrack.sample,
                seqTrack.antibodyTarget,
                seqTrack.libraryPreparationKit,
                seqPlatformGroup)
    }

    protected <T extends Artefact> AlignmentArtefactData<T> createAlignmentArtefactData(T t = null) {
        return new AlignmentArtefactData<>(createWorkflowArtefact(), t, null, null, null, null, null, null, null, null, null)
    }

    protected AlignmentArtefactData<SeqTrack> createAlignmentArtefactDataForSeqTrack(SeqTrack seqTrack) {
        return new AlignmentArtefactData<SeqTrack>(
                seqTrack.workflowArtefact,
                seqTrack,
                seqTrack.project,
                seqTrack.seqType,
                seqTrack.individual,
                seqTrack.sampleType,
                seqTrack.sample,
                seqTrack.antibodyTarget,
                seqTrack.libraryPreparationKit,
                seqTrack.seqPlatform,
                null
        )
    }

    protected AlignmentArtefactData<FastqcProcessedFile> createAlignmentArtefactDataForFastqcProcessedFile(FastqcProcessedFile fastqcProcessedFile) {
        SeqTrack seqTrack = fastqcProcessedFile.sequenceFile.seqTrack
        return new AlignmentArtefactData<FastqcProcessedFile>(
                fastqcProcessedFile.workflowArtefact,
                fastqcProcessedFile,
                seqTrack.project,
                seqTrack.seqType,
                seqTrack.individual,
                seqTrack.sampleType,
                seqTrack.sample,
                seqTrack.antibodyTarget,
                seqTrack.libraryPreparationKit,
                seqTrack.seqPlatform,
                null
        )
    }

    protected AlignmentArtefactData<RoddyBamFile> createAlignmentArtefactDataForRoddyBamFile(RoddyBamFile bamFile) {
        MergingWorkPackage workPackage = bamFile.workPackage
        return new AlignmentArtefactData<RoddyBamFile>(
                bamFile.workflowArtefact,
                bamFile,
                workPackage.project,
                workPackage.seqType,
                workPackage.individual,
                workPackage.sampleType,
                workPackage.sample,
                workPackage.antibodyTarget,
                workPackage.libraryPreparationKit,
                null,
                workPackage.seqPlatformGroup
        )
    }

    protected void assertAlignmentArtefactDataList(AlignmentArtefactDataList returned, AlignmentArtefactDataList expected) {
        TestCase.assertContainSame(returned.seqTrackData, expected.seqTrackData)
        TestCase.assertContainSame(returned.fastqcProcessedFileData, expected.fastqcProcessedFileData)
        TestCase.assertContainSame(returned.bamData, expected.bamData)
    }

    private Map<String, ?> createDefaultMapForCreateWorkflowRunsAndOutputArtefactsDomains() {
        return [
                noSeqTrack                   : false,
                existingBamFileOtherSeqTracks: false,
                existingBamFileSameSeqTracks : false,
                missingFastqc                : false,
                toManyFastqc                 : false,
                noSpecies                    : false,
                noReferenceGenome            : false,
                wrongLibPrepKit              : false,
                wrongSeqPlatformGroup        : false,
                wrongReferenceGenome         : false,

                noSeqPlatformGroupMwp        : false,
                noSeqPlatformGroupSeqTrack   : false,
                wrongMergingWorkPage         : false,
        ]
    }

    private void createDataForCreateWorkflowRunsAndOutputArtefactsDomains(Map<String, ?> adaption) {
        Map<String, ?> values = createDefaultMapForCreateWorkflowRunsAndOutputArtefactsDomains() + adaption

        SpeciesWithStrain speciesWithStrain = findOrCreateHumanSpecies()
        referenceGenome = createReferenceGenome([
                speciesWithStrain: [values.noReferenceGenome ? createSpeciesWithStrain() : speciesWithStrain] as Set,
                species          : [] as Set,
        ])

        Individual individual = createIndividual([
                species: values.noSpecies ? null : speciesWithStrain,
        ])
        seqTrack1 = createSeqTrackWithTwoFastqFile([
                workflowArtefact: createWorkflowArtefact([artefactType: ArtefactType.FASTQ]),
                sample          : createSample([
                        individual: individual,
                ]),
        ])
        seqTrack2 = createCorrespondingSeqTrack(seqTrack1)
        seqTracks = [
                seqTrack1,
                seqTrack2,
        ]

        List<SeqTrack> seqTracksForFastqc = seqTracks.clone()
        if (values.toManyFastqc) {
            seqTracksForFastqc << createCorrespondingSeqTrack(seqTrack1)
        }

        seqPlatformGroup = seqTrack1.seqPlatform.seqPlatformGroups.first()
        fastqcProcessedFiles = useFastqcCount ? seqTracksForFastqc*.sequenceFiles.flatten().collect {
            createFastqcProcessedFile([sequenceFile: it, workflowArtefact: createWorkflowArtefact([artefactType: ArtefactType.FASTQC])])
        } : []
        if (values.missingFastqc) {
            seqTracks << createCorrespondingSeqTrack(seqTrack1)
        }

        workflow = createWorkflow([name: decider.workflowName])
        workflowVersion = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)])
        createMergingCriteria([
                project: seqTrack1.project,
                seqType: seqTrack1.seqType,
        ])
    }

    protected void createDataForCreateWorkflowRunsAndOutputArtefacts(boolean createMwp, Map adaption) {
        createDataForCreateWorkflowRunsAndOutputArtefactsDomains(adaption)

        Map<String, ?> values = createDefaultMapForCreateWorkflowRunsAndOutputArtefactsDomains() + adaption

        // dto objects
        projectSeqTypeGroup = new ProjectSeqTypeGroup(seqTrack1.project, seqTrack1.seqType)

        alignmentDeciderGroup = createAlignmentDeciderGroup(seqTrack1, (values.noSeqPlatformGroupSeqTrack || values.noSeqPlatformGroupMwp) ? null : seqPlatformGroup)

        // AlignmentArtefactDataList
        List<AlignmentArtefactData<SeqTrack>> seqTrackData = values.noSeqTrack ? [] : seqTracks.collect {
            createAlignmentArtefactDataForSeqTrack(it)
        }

        List<AlignmentArtefactData<FastqcProcessedFile>> fastqcProcessedFileData = fastqcProcessedFiles.collect { FastqcProcessedFile fastqcProcessedFile ->
            createAlignmentArtefactDataForFastqcProcessedFile(fastqcProcessedFile)
        }
        List<AlignmentArtefactData<RoddyBamFile>> roddyBamFileData = []
        dataList = new AlignmentArtefactDataList(seqTrackData, fastqcProcessedFileData, roddyBamFileData)

        // additional AlignmentArtefactDataList
        additionalDataList = new AlignmentArtefactDataList([], [], [])

        // additional data
        Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> referenceGenomeMap = [
                (projectSeqTypeGroup): [
                        (referenceGenome.speciesWithStrain): referenceGenome,
                ],
        ]
        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = seqTracks.collectEntries {
            [(it): it.sequenceFiles]
        }

        additionalData = new AlignmentAdditionalData(referenceGenomeMap, [:], [:], [:], [:], rawSequenceFileMap, findOrCreatePanCanPipeline())

        if (createMwp) {
            baseMergingWorkPackage = createMergingWorkPackage([
                    sample               : seqTrack1.sample,
                    seqType              : seqTrack1.seqType,
                    antibodyTarget       : seqTrack1.antibodyTarget,
                    seqPlatformGroup     : values.noSeqPlatformGroupMwp ? null : values.wrongSeqPlatformGroup ? createSeqPlatformGroup() : seqPlatformGroup,
                    libraryPreparationKit: values.wrongLibPrepKit ? createLibraryPreparationKit() : seqTrack1.libraryPreparationKit,
                    referenceGenome      : values.wrongReferenceGenome ? createReferenceGenome() : referenceGenome,
                    pipeline             : findOrCreatePipeline(),
            ])
            AlignmentWorkPackageGroup group = new AlignmentWorkPackageGroup(baseMergingWorkPackage.sample, baseMergingWorkPackage.seqType, baseMergingWorkPackage.antibodyTarget)
            additionalData.mergingWorkPackageMap[group] = baseMergingWorkPackage

            if (values.existingBamFileOtherSeqTracks || values.existingBamFileSameSeqTracks) {
                bamFile = createBamFile([
                        workflowArtefact: createWorkflowArtefact([
                                artefactType: ArtefactType.BAM,
                        ]),
                        workPackage     : baseMergingWorkPackage,
                        seqTracks       : values.existingBamFileSameSeqTracks ? seqTracks : [seqTrack1],
                ])
                additionalDataList.bamData << createAlignmentArtefactDataForRoddyBamFile(bamFile)
            }
        }
    }

    private SeqTrack createCorrespondingSeqTrack(SeqTrack seqTrack) {
        return createSeqTrackWithTwoFastqFile([
                workflowArtefact     : createWorkflowArtefact([artefactType: ArtefactType.FASTQ]),
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                antibodyTarget       : seqTrack.antibodyTarget,
                run                  : seqTrack.run,
        ])
    }

    private void createServicesForCreateWorkflowRunsAndOutputArtefacts(WorkflowVersion workflowVersion, SeqTrack seqTrack) {
        decider.workflowRunService = Mock(WorkflowRunService) {
            1 * buildWorkflowRun(workflowVersion.workflow, seqTrack.project.processingPriority, "", seqTrack.project, _, _, workflowVersion) >> {
                Workflow workflowParam, ProcessingPriority priorityParam, String workDirectoryParam, Project projectParam,
                List<String> displayNameLinesParam, String shortNameParam, WorkflowVersion workflowVersionParam ->
                    new WorkflowRun([ // codenarc-disable-line
                                      workDirectory   : workDirectoryParam,
                                      state           : WorkflowRun.State.PENDING,
                                      project         : projectParam,
                                      combinedConfig  : null,
                                      priority        : priorityParam,
                                      restartedFrom   : null,
                                      omittedMessage  : null,
                                      workflowSteps   : [],
                                      workflow        : workflowParam,
                                      workflowVersion : workflowVersionParam,
                                      displayName     : displayNameLinesParam.join(', '),
                                      shortDisplayName: shortNameParam,
                    ]).save(flush: false)
            }
            0 * _
        }
        decider.workflowArtefactService = Mock(WorkflowArtefactService) {
            1 * buildWorkflowArtefact(_) >> { WorkflowArtefactValues values ->
                return new WorkflowArtefact([
                        producedBy      : values.run,
                        outputRole      : values.role,
                        withdrawnDate   : null,
                        withdrawnComment: null,
                        state           : WorkflowArtefact.State.PLANNED_OR_RUNNING,
                        artefactType    : values.artefactType,
                        displayName     : values.displayNameLines.join(', '),
                ]).save(flush: false)
            }
            0 * _
        }

        decider.roddyBamFileService = Mock(RoddyBamFileService) {
            1 * getWorkDirectory(_) >> { RoddyBamFile roddyBamFile ->
                Paths.get('/tmp')
            }
            0 * _
        }
    }

    protected void createEmptyServicesForCreateWorkflowRunsAndOutputArtefacts(int mailCount) {
        decider.workflowRunService = Mock(WorkflowRunService) {
            0 * _
        }
        decider.workflowArtefactService = Mock(WorkflowArtefactService) {
            0 * _
        }
        decider.roddyBamFileService = Mock(RoddyBamFileService) {
            0 * _
        }
        decider.unalignableSeqTrackEmailCreator = Mock(UnalignableSeqTrackEmailCreator) {
            mailCount * getMailContent(_, _) >> new UnalignableSeqTrackEmailCreator.MailContent()
        }
        decider.mailHelperService = Mock(MailHelperService) {
            mailCount * sendEmailToTicketSystem(_, _)
        }
    }

    protected Pipeline findPipeline() {
        return findOrCreatePipeline(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }
}
