package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*
import grails.test.mixin.Mock

@Mock([
        AceseqQc,
        DataFile,
        ExternalScript,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingSet,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqTrack,
        SeqType,
        SoftwareTool
])

class AceseqInstanceSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder

    AceseqInstance instance
    File temporaryFile

    /**
     * Creates Temporary File for Data Management path
     * so later on temp files can be generated and paths tested
     */
    void setup() {
        this.temporaryFile = temporaryFolder.newFolder()
        Realm realm = DomainFactory.createRealmDataManagement(rootPath: temporaryFile)

        this.instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        instance.project.realmName = realm.name
        instance.project.save(flush: true)
    }
    /**
     * Will test the getInstancePath, by creating
     * expectedPath what holds the path as it should be.
     * aceseqInstancePath holds the Path returned from the getter.
     * Afterwards both will be checked if they are equal
     * If yes the Tests runs correct if not equal exception is thrown
     */
    @Unroll("Tests #method, tests if Path is in a valid form")
    void "getAceseqInstancePath, tests if Path is in a valid form"() {

        given:
        File expectedPath = new File(
                temporaryFile, "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/cnv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}"
        )

        when:
        OtpPath aceseqInstancePath = instance.getInstancePath()

        then:
        instance.project == aceseqInstancePath.project
        expectedPath.path == aceseqInstancePath.absoluteDataManagementPath.path
    }

    /**
     * Will test the getters GcCorrected, QcGcCorrected and WgCoverage
     * expectedRelativePath holds the path as it should be for the single Method calls.
     * In the Datatable Method stands for the getter Method name that should be called.
     * Name stands for the Data ending that should be concatenated to the expectedPath
     *  If yes the Tests runs correct if not equal exception is thrown
     */
    @Unroll("Tests #method, tests if Path is in a valid form")
    void "tests the getGcCorrected, getQcGcCorrected and the getWgCoverage, tests if Path is in a valid form"() {

        given:
        File expectedPath = new File(
                temporaryFile, "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/cnv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}" +
                "/${instance.instanceName}/plots/${instance.individual.pid}${name}"
        )

        when:
        File result = instance."${method}"()

        then:
        expectedPath.path == result.path

        where:
        method             | name
        "getGcCorrected"   | "_gc_corrected.png"
        "getQcGcCorrected" | "_qc_rep_corrected.png"
        "getWgCoverage"    | "_wholeGenome_coverage.png"
    }

    /**
     * Will test the getters getDistanceCombinedStars, getPlotAll and getPlotExtra
     * expectedPath holds the path as it should be for the single Method calls.
     * In the Datatable Method stands for the getter Method name that should be called.
     * Name stands for the Data ending that should be concatenated to the expectedPath.
     *  If yes the Tests runs correct if not equal exception is thrown
     */
    @Unroll("Tests #method, tests if Path is in a valid form")
    void "Tests getDistanceCombinedStars, getPlotAll and getPlotExtra, tests if Path is in a valid form"() {

        given:

         File expectedPath = new File(
                temporaryFile, "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/cnv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}/${instance.individual.pid}${name}"
        )

        CreateFileHelper.createFile(expectedPath)

        AceseqQc qcInstance = DomainFactory.createAceseqQcWithExistingAceseqInstance(instance)
        qcInstance.ploidyFactor = '1.0'
        qcInstance.purity = '2.0'
        qcInstance.save(flush: true)

        when:
        def result = instance."${method}"()

        then:
        //tcn returns no list, so if getter no list just check the path
        (result instanceof List)? result == [expectedPath] : expectedPath.path == result.path

        where:
        method                        | name
        "getTcnDistancesCombinedStar" | "_tcn_distances_combined_star.png"
        "getPlotAll"                  | "_plot_XX_ALL.png"
        "getPlotExtra"                | "_plot_1.0extra_2.0_XX.png"
    }
}
