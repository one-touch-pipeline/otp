package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqPlatformGroup,
        SeqType,
])
class SamplePairSpec extends Specification {

    List setUpForPathTests(String analysisName) {
        Realm realm = DomainFactory.createRealmDataManagement()
        Project project = DomainFactory.createProject(
                realmName: realm.name,
        )
        Individual individual = DomainFactory.createIndividual(
                project: project,
        )
        SeqType seqType = DomainFactory.createSeqType(
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED
        )
        SampleType sampleType1 = DomainFactory.createSampleType(
                name: "TUMOR",
        )
        SampleType sampleType2 = DomainFactory.createSampleType(
                name: "CONTROL",
        )
        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleType1,
                category: SampleType.Category.DISEASE,
        )
        MergingWorkPackage mergingWorkPackage1 = DomainFactory.createMergingWorkPackage(
                seqType: seqType,
                sample: DomainFactory.createSample(
                        individual: individual,
                        sampleType: sampleType1,
                ),
        )
        SamplePair samplePair = DomainFactory.createSamplePair(mergingWorkPackage1,
                DomainFactory.createMergingWorkPackage(mergingWorkPackage1, sampleType2))

        return ["${project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}/${analysisName.toLowerCase()}_results/paired/tumor_control", samplePair, project]
    }

    @Unroll
    def "get #analysisName sample pair path"() {
        given:
        def (String path, SamplePair samplePair, Project project) = setUpForPathTests(analysisName)
        File expectedExtension = new File(path)

        when:
        OtpPath samplePairPath = samplePair."get${analysisName}SamplePairPath"()

        then:
        expectedExtension == samplePairPath.relativePath
        project == samplePairPath.project

        where:
        analysisName << [
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    def "set #analysisName processing status"() {
        given:
        SamplePair samplePair = setUpForPathTests(analysisName)[1]

        when:
        SamplePair."set${analysisName}ProcessingStatus"([samplePair], SamplePair.ProcessingStatus.DISABLED)

        then:
        samplePair."${analysisName.toLowerCase()}ProcessingStatus" == SamplePair.ProcessingStatus.DISABLED

        where:
        analysisName << [
                "Aceseq",
                "Indel",
                "Snv",
        ]
    }
}
