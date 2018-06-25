package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*


@Mock([
        AceseqInstance,
        AceseqQc,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
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
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool
])
class AceseqInstanceSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder

    TestConfigService configService

    AceseqInstance instance
    File instancePath

    /**
     * Creates Temporary File for Data Management path
     * so later on temp files can be generated and paths tested
     */
    void setup() {
        File temporaryFile = temporaryFolder.newFolder()
        Realm realm = DomainFactory.createRealm()
        configService = new TestConfigService(['otp.root.path': temporaryFile.path])

        this.instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        instance.project.realm = realm
        instance.project.save(flush: true)

        instancePath = new File(
                temporaryFile, "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/cnv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}"
        )
    }

    void cleanup() {
        configService.clean()
    }

    /**
     * Will test the getInstancePath, by creating
     * aceseqInstancePath holds the Path returned from the getter.
     * Afterwards both will be checked if they are equal
     * If yes the Tests runs correct if not equal exception is thrown
     */
    void "getAceseqInstancePath, tests if Path is in a valid form"() {

        when:
        OtpPath aceseqInstancePath = instance.getInstancePath()

        then:
        instance.project == aceseqInstancePath.project
        instancePath == aceseqInstancePath.absoluteDataManagementPath
    }

    /**
     * Will test getPlot
     * In the data table plot stands for the argument that should be passed.
     * Name stands for the Data ending that should be concatenated to the expectedPath
     *  If yes the Tests runs correct if not equal exception is thrown
     */
    @Unroll()
    void "test getPlot with #plot, tests if Path is in a valid form"() {

        expect:
        instance.getPlot(plot) == new File(instancePath, "plots/${plot == PlotType.ACESEQ_WG_COVERAGE ? 'control_' : ''}${instance.individual.pid}_${name}")

        where:
        plot                            | name
        PlotType.ACESEQ_GC_CORRECTED    | "gc_corrected.png"
        PlotType.ACESEQ_QC_GC_CORRECTED | "qc_rep_corrected.png"
        PlotType.ACESEQ_WG_COVERAGE     | "wholeGenome_coverage.png"
    }

    void "test getPlot with TCN_DISTANCE_COMBINED_STAR"() {
        expect:
        instance.getPlot(PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR) == new File(instancePath, "${instance.individual.pid}_tcn_distances_combined_star.png")
    }

    /**
     * Will test getPlots
     * expectedPath holds the path as it should be for the single Method calls.
     * In the data table plots stands for the argument that should be passed.
     * Name stands for the Data ending that should be concatenated to the expectedPath.
     *  If yes the Tests runs correct if not equal exception is thrown
     */
    @Unroll()
    void "test getPlots with #plots, tests if Path is in a valid form"() {

        given:
        File expectedPath = new File(instancePath, "${instance.individual.pid}_${name}")

        CreateFileHelper.createFile(expectedPath)

        AceseqQc qcInstance = DomainFactory.createAceseqQcWithExistingAceseqInstance(instance)
        qcInstance.ploidyFactor = '1.0'
        qcInstance.tcc = tcc
        qcInstance.save(flush: true)

        expect:
        instance.getPlots(plots) == [expectedPath]

        where:
        plots | name                        | tcc
        PlotType.ACESEQ_ALL   | "plot_XX_ALL.png"           | 2.0
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_2_XX.png"    | 2.0
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_0.1_XX.png"  | 0.1
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_0.42_XX.png" | 0.42
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_0_XX.png"    | 0
    }

    void "test getQcJsonFile"() {
        expect:
        instance.getQcJsonFile() == new File(instancePath, "cnv_${instance.individual.pid}_parameter.json")
    }
}
