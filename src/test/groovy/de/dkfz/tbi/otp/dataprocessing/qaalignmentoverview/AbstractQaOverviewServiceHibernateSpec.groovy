/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import asset.pipeline.grails.LinkGenerator
import grails.test.hibernate.HibernateSpec
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue

import java.time.ZoneId
import java.time.ZonedDateTime

class AbstractQaOverviewServiceHibernateSpec extends HibernateSpec implements RoddyPancanFactory {

    private AbstractQaOverviewService service

    @Override
    List<Class> getDomainClasses() {
        return [
                RoddyBamFile,
        ]
    }

    void "createQuery, when subclass do not extend query, then creates base query"() {
        given:
        service = new TestAbstractQaOverviewServiceWithoutExtension()

        String expect = """
            |select
            |    new map(
            |        bamFile.id as bamId,
            |        bamFile.version as dbVersion,
            |        bamFile.dateFromFileSystem as dateFromFileSystem,
            |        bamFile.withdrawn as withdrawn,
            |        bamFile.qcTrafficLightStatus as qcStatus,
            |        mergingWorkPackage.id as mergingWorkPackageId,
            |        pipeline.name as pipelineName,
            |        sampleType.name as sampleType,
            |        individual.pid as pid,
            |        individual.id as individualId,
            |        comment.comment as qcComment,
            |        comment.author as qcAuthor,
            |        qa.totalMappedReadCounter as totalMappedReadCounter,
            |        qa.duplicates as duplicates,
            |        qa.withMateMappedToDifferentChr as withMateMappedToDifferentChr,
            |        qa.properlyPaired as properlyPaired,
            |        qa.singletons as singletons,
            |        qa.totalReadCounter as totalReadCounter,
            |        qa.pairedInSequencing as pairedInSequencing,
            |        qa.insertSizeMedian as insertSizeMedian,
            |        config.programVersion as programVersion1,
            |        config2.programVersion as programVersion2,
            |        version.workflowVersion as workflowVersion,
            |        referenceGenome.name as referenceGenomeName,
            |        toolName.name as toolNameName,
            |        referenceGenomeIndex.indexToolVersion as indexToolVersion,
            |        run.state as state
            |    )
            |from
            |    ${AbstractQualityAssessment.name} qa
            |    join qa.qualityAssessmentMergedPass qaPass
            |    join qaPass.abstractBamFile bamFile
            |    join bamFile.workPackage mergingWorkPackage
            |    join mergingWorkPackage.sample sample
            |    join mergingWorkPackage.pipeline pipeline
            |    join sample.sampleType sampleType
            |    join sample.individual individual
            |    join individual.project project
            |    left outer join bamFile.comment comment
            |    left outer join mergingWorkPackage.config config
            |    left outer join bamFile.config config2
            |    left outer join bamFile.workflowArtefact artefact
            |    left outer join artefact.producedBy run
            |    left outer join run.workflowVersion version
            |    left outer join mergingWorkPackage.referenceGenomeIndex referenceGenomeIndex
            |    left outer join referenceGenomeIndex.referenceGenome referenceGenome
            |    left outer join referenceGenomeIndex.toolName toolName
            |where
            |    project = :project
            |    and bamFile.fileOperationStatus = 'PROCESSED'
            |    and bamFile.qualityAssessmentStatus = 'FINISHED'
            |    and bamFile.withdrawn = false
            |    and mergingWorkPackage.seqType = :seqType
            |    and mergingWorkPackage.bamFileInProjectFolder = bamFile
        """.stripMargin().replaceAll(' *\n *', ' | ')

        when:
        String query = service.createQuery(false).replaceAll(' *\n *', ' | ')

        then:
        query == expect
    }

    void "createQuery, when subclass extend query, then creates extended query"() {
        given:
        service = new TestAbstractQaOverviewServiceWithExtension()

        String expect = """
            |select
            |    new map(
            |        bamFile.id as bamId,
            |        bamFile.version as dbVersion,
            |        bamFile.dateFromFileSystem as dateFromFileSystem,
            |        bamFile.withdrawn as withdrawn,
            |        bamFile.qcTrafficLightStatus as qcStatus,
            |        mergingWorkPackage.id as mergingWorkPackageId,
            |        pipeline.name as pipelineName,
            |        sampleType.name as sampleType,
            |        individual.pid as pid,
            |        individual.id as individualId,
            |        comment.comment as qcComment,
            |        comment.author as qcAuthor,
            |        qa.totalMappedReadCounter as totalMappedReadCounter,
            |        qa.duplicates as duplicates,
            |        qa.withMateMappedToDifferentChr as withMateMappedToDifferentChr,
            |        qa.properlyPaired as properlyPaired,
            |        qa.singletons as singletons,
            |        qa.totalReadCounter as totalReadCounter,
            |        qa.pairedInSequencing as pairedInSequencing,
            |        qa.insertSizeMedian as insertSizeMedian,
            |        config.programVersion as programVersion1,
            |        config2.programVersion as programVersion2,
            |        version.workflowVersion as workflowVersion,
            |        referenceGenome.name as referenceGenomeName,
            |        toolName.name as toolNameName,
            |        referenceGenomeIndex.indexToolVersion as indexToolVersion,
            |        run.state as state,
            |        domain1.property1 as alias1,
            |        domain2.property2 as alias2
            |    )
            |from
            |    ${AbstractQualityAssessment.name} qa
            |    join qa.qualityAssessmentMergedPass qaPass
            |    join qaPass.abstractBamFile bamFile
            |    join bamFile.workPackage mergingWorkPackage
            |    join mergingWorkPackage.sample sample
            |    join mergingWorkPackage.pipeline pipeline
            |    join sample.sampleType sampleType
            |    join sample.individual individual
            |    join individual.project project
            |    left outer join bamFile.comment comment
            |    left outer join mergingWorkPackage.config config
            |    left outer join bamFile.config config2
            |    left outer join bamFile.workflowArtefact artefact
            |    left outer join artefact.producedBy run
            |    left outer join run.workflowVersion version
            |    left outer join mergingWorkPackage.referenceGenomeIndex referenceGenomeIndex
            |    left outer join referenceGenomeIndex.referenceGenome referenceGenome
            |    left outer join referenceGenomeIndex.toolName toolName
            |    join qa.property1 property1
            |    left outer join qa.property2 property2
            |    join property1.property3 property3,
            |    Domain1 domain1 join domain1.property1_1 property1_1,
            |    Domain2 domain2 join domain2.property2_1 property2_1
            |where
            |    project = :project
            |    and bamFile.fileOperationStatus = 'PROCESSED'
            |    and bamFile.qualityAssessmentStatus = 'FINISHED'
            |    and bamFile.withdrawn = false
            |    and mergingWorkPackage.seqType = :seqType
            |    and mergingWorkPackage.bamFileInProjectFolder = bamFile
            |    and restriction1
            |    and restriction2
        """.stripMargin().replaceAll(' *\n *', ' | ')

        when:
        String query = service.createQuery(false).replaceAll(' *\n *', ' | ')

        then:
        query == expect
    }

    void "createQuery, when subclass extend query and include sample, then creates extended query with sample"() {
        given:
        service = new TestAbstractQaOverviewServiceWithExtension()

        String expect = """
            |select
            |    new map(
            |        bamFile.id as bamId,
            |        bamFile.version as dbVersion,
            |        bamFile.dateFromFileSystem as dateFromFileSystem,
            |        bamFile.withdrawn as withdrawn,
            |        bamFile.qcTrafficLightStatus as qcStatus,
            |        mergingWorkPackage.id as mergingWorkPackageId,
            |        pipeline.name as pipelineName,
            |        sampleType.name as sampleType,
            |        individual.pid as pid,
            |        individual.id as individualId,
            |        comment.comment as qcComment,
            |        comment.author as qcAuthor,
            |        qa.totalMappedReadCounter as totalMappedReadCounter,
            |        qa.duplicates as duplicates,
            |        qa.withMateMappedToDifferentChr as withMateMappedToDifferentChr,
            |        qa.properlyPaired as properlyPaired,
            |        qa.singletons as singletons,
            |        qa.totalReadCounter as totalReadCounter,
            |        qa.pairedInSequencing as pairedInSequencing,
            |        qa.insertSizeMedian as insertSizeMedian,
            |        config.programVersion as programVersion1,
            |        config2.programVersion as programVersion2,
            |        version.workflowVersion as workflowVersion,
            |        referenceGenome.name as referenceGenomeName,
            |        toolName.name as toolNameName,
            |        referenceGenomeIndex.indexToolVersion as indexToolVersion,
            |        run.state as state,
            |        domain1.property1 as alias1,
            |        domain2.property2 as alias2
            |    )
            |from
            |    ${AbstractQualityAssessment.name} qa
            |    join qa.qualityAssessmentMergedPass qaPass
            |    join qaPass.abstractBamFile bamFile
            |    join bamFile.workPackage mergingWorkPackage
            |    join mergingWorkPackage.sample sample
            |    join mergingWorkPackage.pipeline pipeline
            |    join sample.sampleType sampleType
            |    join sample.individual individual
            |    join individual.project project
            |    left outer join bamFile.comment comment
            |    left outer join mergingWorkPackage.config config
            |    left outer join bamFile.config config2
            |    left outer join bamFile.workflowArtefact artefact
            |    left outer join artefact.producedBy run
            |    left outer join run.workflowVersion version
            |    left outer join mergingWorkPackage.referenceGenomeIndex referenceGenomeIndex
            |    left outer join referenceGenomeIndex.referenceGenome referenceGenome
            |    left outer join referenceGenomeIndex.toolName toolName
            |    join qa.property1 property1
            |    left outer join qa.property2 property2
            |    join property1.property3 property3,
            |    Domain1 domain1 join domain1.property1_1 property1_1,
            |    Domain2 domain2 join domain2.property2_1 property2_1
            |where
            |    project = :project
            |    and bamFile.fileOperationStatus = 'PROCESSED'
            |    and bamFile.qualityAssessmentStatus = 'FINISHED'
            |    and bamFile.withdrawn = false
            |    and mergingWorkPackage.seqType = :seqType
            |    and mergingWorkPackage.bamFileInProjectFolder = bamFile
            |    and restriction1
            |    and restriction2
            |    and sample = :sample
        """.stripMargin().replaceAll(' *\n *', ' | ')

        when:
        String query = service.createQuery(true).replaceAll(' *\n *', ' | ')

        then:
        query == expect
    }

    @Unroll
    void "createParameterMap, when #name, then creates expected map"() {
        given:
        service = additionalParams ? new TestAbstractQaOverviewServiceWithExtension() : new TestAbstractQaOverviewServiceWithoutExtension()
        Project project = createProject()
        SeqType seqType = createSeqType()
        Sample sample = additinalSample ? createSample() : null

        Map<String, ? extends Object> expectedParameterMap = [
                project: project,
                seqType: seqType,
        ]
        if (additinalSample) {
            expectedParameterMap.put('sample', sample)
        }
        if (additionalParams) {
            expectedParameterMap.put('param1', 'param1')
            expectedParameterMap.put('param2', 'param2')
        }

        when:
        Map<String, ? extends Object> parameterMap = service.createParameterMap(project, seqType, sample)

        then:
        parameterMap == expectedParameterMap

        where:
        name                                       | additionalParams | additinalSample
        'no additional params and no sample given' | false            | false
        'additional params and sample given'       | true             | false
        'no additional params and no sample given' | false            | true
        'additional params and sample given'       | true             | true
    }

    @Unroll
    void "addDerivedData, when called with map containing needed values, then should return map with extracted values"() {
        given:
        service = new TestAbstractQaOverviewServiceWithoutExtension()

        Map<String, ?> qaMap = [
                //numerator
                totalMappedReadCounter      : totalMappedReadCounter,
                duplicates                  : duplicates,
                withMateMappedToDifferentChr: withMateMappedToDifferentChr,
                properlyPaired              : properlyPaired,
                singletons                  : singletons,
                //denominator
                pairedInSequencing          : pairedInSequencing,
                totalReadCounter            : totalReadCounter,
        ]

        Map<String, ?> expected = qaMap + [
                percentMappedReads   : percentMappedReads,
                percentDuplicates    : percentDuplicates,
                percentDiffChr       : percentDiffChr,
                percentProperlyPaired: percentProperlyPaired,
                percentSingletons    : percentSingletons,
        ]

        when:
        service.addDerivedData([qaMap])

        then:
        TestCase.assertContainSame(qaMap, expected)

        where:
        totalMappedReadCounter | duplicates | withMateMappedToDifferentChr | properlyPaired | singletons | pairedInSequencing | totalReadCounter || percentMappedReads | percentDuplicates | percentDiffChr | percentProperlyPaired | percentSingletons
        4                      | 8          | 12                           | 16             | 20         | 2                  | 4                || 100.0              | 200.0             | 300.0          | 800.0                 | 500.0
        4                      | 8          | 12                           | 16             | 20         | 20                 | 40               || 10.0               | 20.0              | 30.0           | 80.0                  | 50.0
        4                      | 8          | 12                           | 16             | 20         | 2000               | 4000             || 0.1                | 0.2               | 0.3            | 0.8                   | 0.5
        400                    | 800        | 1200                         | 1600           | 2000       | 2                  | 4                || 10000.0            | 20000.0           | 30000.0        | 80000.0               | 50000.0
        4                      | 8          | 12                           | 16             | 20         | null               | null             || null               | null              | null           | null                  | null
        null                   | null       | null                         | null           | null       | 2                  | 4                || null               | null              | null           | null                  | null
    }

    @Unroll
    void "calculatePercentage, when called numerator=#numerator and denominator=#denominator, then return #expected"() {
        given:
        service = new TestAbstractQaOverviewServiceWithoutExtension()

        when:
        Double result = service.calculatePercentage(numerator, denominator)

        then:
        result == expected

        where:
        numerator  | denominator || expected
        1          | 1           || 100.0
        5          | 1           || 500.0
        5          | 5           || 100.0
        1          | 5           || 20.00
        1          | 10          || 10.0
        1          | 100         || 1.0
        1          | 1000        || 0.1
        1          | 10000       || 0.01
        1          | 100000      || 0.001
        1234567890 | 1           || 123456789000.0
        1          | 0           || null
        1          | null        || null
        null       | 1           || null
        null       | null        || null
    }

    void "createList, when subclass do not extend keys, then creates minimal result"() {
        given:
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of('+0'))
        final long individualId = nextId
        final long bamId = nextId
        final double readLength = nextId

        Map pidLinkMap = [
                controller: 'individual',
                action    : 'show',
                id        : individualId,
        ]
        String pidLink = "pidLink"

        Map<String, ?> qaMap = [
                //general
                pid                           : 'pid',
                individualId                  : individualId,
                libraryPreparationKitName     : 'libraryPreparationKitName',
                dateFromFileSystem            : Date.from(zonedDateTime.toInstant()),
                pipelineName                  : Pipeline.Name.PANCAN_ALIGNMENT,
                qcStatus                      : AbstractBamFile.QcTrafficLightStatus.WARNING,
                qcComment                     : 'qcComment',
                qcAuthor                      : 'qcAuthor',
                bamId                         : bamId,

                //QC_KEY_BASE
                percentMappedReads            : 101,
                percentDuplicates             : 102,
                percentDiffChr                : 103,
                percentProperlyPaired         : 104,
                percentSingletons             : 105,

                //qcKeys: none

                //keyMapBase
                insertSizeMedian              : 301,
                readLength                    : readLength,

                //qcKeysMap(qaMap):none
        ]

        TableCellValue qcStatusCell = Mock(TableCellValue)

        Project project = createProject()
        SeqType seqType = createSeqType()

        QcThresholdService.ThresholdColorizer thresholdColorizerMock = Mock(QcThresholdService.ThresholdColorizer) {
            0 * _
            1 * colorize([
                    "percentMappedReads",
                    "percentDuplicates",
                    "percentDiffChr",
                    "percentProperlyPaired",
                    "percentSingletons",
            ], qaMap) >> { List<String> properties, Map<String, ?> paramMap ->
                properties.collectEntries {
                    assert paramMap.containsKey(it)
                    [(it), paramMap[it]]
                }
            }
            1 * colorize([], qaMap) >> [:]
            1 * colorize([
                    insertSizeMedian: readLength,
            ], qaMap) >> { Map<String, Double> properties, Map<String, ?> paramMap ->
                properties.collectEntries {
                    assert paramMap.containsKey(it.key)
                    [(it.key): paramMap[it.key]]
                }
            }
            1 * colorize([:], qaMap) >> [:]
        }

        service = new TestAbstractQaOverviewServiceWithoutExtension()
        service.qcStatusCellService = Mock(QcStatusCellService) {
            1 * generateQcStatusCell(qaMap) >> qcStatusCell
            0 * _
        }
        service.qcThresholdService = Mock(QcThresholdService) {
            0 * _
            1 * createThresholdColorizer(project, seqType, service.qaClass()) >> thresholdColorizerMock
        }
        service.linkGenerator = Mock(LinkGenerator) {
            0 * _
            1 * link(pidLinkMap) >> pidLink
        }

        when:
        List<Map<String, ?>> resultList = service.createList(project, seqType, [qaMap])

        then:
        resultList
        resultList.size() == 1
        Map<String, ?> resultEntry = resultList[0]
        resultEntry.dateFromFileSystem == "2000-01-01"
        resultEntry.pipelineName == Pipeline.Name.PANCAN_ALIGNMENT.displayName
        resultEntry.qcStatusGui == qcStatusCell

        TableCellValue pidCell = (TableCellValue) resultEntry.pid
        pidCell.value == 'pid'
        pidCell.link == pidLink

        TableCellValue kitCell = (TableCellValue) resultEntry.kit
        kitCell.value == 'libraryPreparationKitName'
        kitCell.tooltip == 'libraryPreparationKitName'
        kitCell.warnColor == null
        kitCell.link == null

        resultEntry.percentMappedReads == 101
        resultEntry.percentDuplicates == 102
        resultEntry.percentDiffChr == 103
        resultEntry.percentProperlyPaired == 104
        resultEntry.percentSingletons == 105

        resultEntry.insertSizeMedian == 301
    }

    void "createList, when subclass extend query, then creates extended result"() {
        given:
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of('+0'))
        final long individualId = nextId
        final long bamId = nextId
        final double readLength = nextId

        Map pidLinkMap = [
                controller: 'individual',
                action    : 'show',
                id        : individualId,
        ]
        String pidLink = "pidLink"

        Map<String, ?> qaMap = [
                //general
                pid                           : 'pid',
                individualId                  : individualId,
                libraryPreparationKitName     : 'libraryPreparationKitName',
                dateFromFileSystem            : Date.from(zonedDateTime.toInstant()),
                pipelineName                  : Pipeline.Name.PANCAN_ALIGNMENT,
                qcStatus                      : AbstractBamFile.QcTrafficLightStatus.WARNING,
                qcComment                     : 'qcComment',
                qcAuthor                      : 'qcAuthor',
                bamId                         : bamId,

                //QC_KEY_BASE
                percentMappedReads            : 101,
                percentDuplicates             : 102,
                percentDiffChr                : 103,
                percentProperlyPaired         : 104,
                percentSingletons             : 105,

                //qcKeys
                key1                          : 201,
                key2                          : 202,

                //keyMapBase
                insertSizeMedian              : 301,
                readLength                    : readLength,

                //qcKeysMap(qaMap)
                keyMap1                       : 401,
                keyMap2                       : 402,
        ]

        TableCellValue qcStatusCell = Mock(TableCellValue)

        Project project = createProject()
        SeqType seqType = createSeqType()

        QcThresholdService.ThresholdColorizer thresholdColorizerMock = Mock(QcThresholdService.ThresholdColorizer) {
            0 * _
            1 * colorize([
                    "percentMappedReads",
                    "percentDuplicates",
                    "percentDiffChr",
                    "percentProperlyPaired",
                    "percentSingletons",
            ], _) >> { List<String> properties, Map<String, ?> paramMap ->
                properties.collectEntries {
                    assert paramMap.containsKey(it)
                    [(it), paramMap[it]]
                }
            }
            1 * colorize([
                    "key1",
                    "key2",
            ], _) >> { List<String> properties, Map<String, ?> paramMap ->
                properties.collectEntries {
                    assert paramMap.containsKey(it)
                    [(it), paramMap[it]]
                }
            }
            1 * colorize([
                    insertSizeMedian: readLength,
            ], _) >> { Map<String, Double> properties, Map<String, ?> paramMap ->
                properties.collectEntries {
                    assert paramMap.containsKey(it.key)
                    [(it.key), paramMap[it.key]]
                }
            }
            1 * colorize([
                    keyMap1: 1.0,
                    keyMap2: 2.0,
            ], _) >> { Map<String, Double> properties, Map<String, ?> paramMap ->
                properties.collectEntries {
                    assert paramMap.containsKey(it.key)
                    [(it.key), paramMap[it.key]]
                }
            }
        }

        service = new TestAbstractQaOverviewServiceWithExtension()
        service.qcThresholdService = Mock(QcThresholdService) {
            0 * _
            1 * createThresholdColorizer(project, seqType, service.qaClass()) >> thresholdColorizerMock
        }
        service.linkGenerator = Mock(LinkGenerator) {
            0 * _
            1 * link(pidLinkMap) >> pidLink
        }
        service.qcStatusCellService = Mock(QcStatusCellService) {
            1 * generateQcStatusCell(qaMap) >> qcStatusCell
            0 * _
        }

        when:
        List<Map<String, ?>> resultList = service.createList(project, seqType, [qaMap])

        then:
        resultList
        resultList.size() == 1
        Map<String, ?> resultEntry = resultList[0]
        resultEntry.dateFromFileSystem == "2000-01-01"
        resultEntry.pipelineName == Pipeline.Name.PANCAN_ALIGNMENT.displayName
        resultEntry.qcStatusGui == qcStatusCell

        TableCellValue pidCell = (TableCellValue) resultEntry.pid
        pidCell.value == 'pid'
        pidCell.link == pidLink

        TableCellValue kitCell = (TableCellValue) resultEntry.kit
        kitCell.value == 'libraryPreparationKitName'
        kitCell.tooltip == 'libraryPreparationKitName'
        kitCell.warnColor == null
        kitCell.link == null

        resultEntry.percentMappedReads == 101
        resultEntry.percentDuplicates == 102
        resultEntry.percentDiffChr == 103
        resultEntry.percentProperlyPaired == 104
        resultEntry.percentSingletons == 105

        resultEntry.key1 == 201
        resultEntry.key2 == 202

        resultEntry.insertSizeMedian == 301

        resultEntry.keyMap1 == 401
        resultEntry.keyMap2 == 402

        resultEntry.extract1 == "extract1"
        resultEntry.extract2 == "extract2"
    }

    static class TestAbstractQaOverviewServiceWithoutExtension extends AbstractQaOverviewService {

        @Override
        Class<? extends AbstractQualityAssessment> qaClass() {
            return AbstractQualityAssessment
        }

        @Override
        List<SeqType> supportedSeqTypes() {
            return []
        }

        @Override
        protected List<String> additionalJoinDomains() {
            return []
        }

        @Override
        protected List<String> additionalDomainHierarchies() {
            return []
        }

        @Override
        protected List<String> restriction() {
            return []
        }

        @Override
        protected List<ColumnDefinition> columnDefinitionList() {
            return []
        }

        @Override
        protected Map<String, ? extends Object> additionalParameters() {
            return [:]
        }

        @Override
        protected List<String> qcKeys() {
            return []
        }

        @Override
        protected Map<String, Double> qcKeysMap(Map<String, ?> qaMap) {
            return [:]
        }

        @Override
        protected Map<String, ?> extractSpecificValues(Project project, Map<String, ?> qaMap) {
            return [:]
        }
    }

    static class TestAbstractQaOverviewServiceWithExtension extends AbstractQaOverviewService {

        @Override
        Class<? extends AbstractQualityAssessment> qaClass() {
            return AbstractQualityAssessment
        }

        @Override
        List<SeqType> supportedSeqTypes() {
            return []
        }

        @Override
        protected List<String> additionalJoinDomains() {
            return [
                    "join qa.property1 property1",
                    "left outer join qa.property2 property2",
                    "join property1.property3 property3",
            ]
        }

        @Override
        protected List<String> additionalDomainHierarchies() {
            return [
                    "Domain1 domain1 join domain1.property1_1 property1_1",
                    "Domain2 domain2 join domain2.property2_1 property2_1",
            ]
        }

        @Override
        protected List<String> restriction() {
            return [
                    "restriction1",
                    "restriction2",
            ]
        }

        @Override
        protected List<ColumnDefinition> columnDefinitionList() {
            return [
                    new PropertyColumnDefinition('domain1', 'property1', 'alias1'),
                    new PropertyColumnDefinition('domain2', 'property2', 'alias2'),
            ]
        }

        @Override
        protected Map<String, ? extends Object> additionalParameters() {
            return [
                    param1: 'param1',
                    param2: 'param2',
            ]
        }

        @Override
        protected List<String> qcKeys() {
            return [
                    'key1',
                    'key2',
            ]
        }

        @Override
        protected Map<String, Double> qcKeysMap(Map<String, ?> qaMap) {
            return [
                    keyMap1: 1.0,
                    keyMap2: 2.0,
            ]
        }

        @Override
        protected Map<String, ?> extractSpecificValues(Project project, Map<String, ?> qaMap) {
            return [
                    extract1: "extract1",
                    extract2: "extract2",
            ]
        }
    }
}
