package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.utils.ExternalScript
import grails.util.Environment
import org.joda.time.DateTime
import org.joda.time.Duration

class DomainFactory {

    private DomainFactory() {
    }

    static final String DEFAULT_REALM_NAME = 'FakeRealm'
    static final String DEFAULT_HOST = 'localhost'
    static final int    DEFAULT_PORT = 22
    static final String LABEL_DKFZ = 'DKFZ'
    static final String LABEL_BIOQUANT = 'BioQuant'
    static final String DEFAULT_MD5_SUM = '123456789abcdef123456789abcdef00'

    /**
     * Defaults for new realms.
     * <p>
     * The values are set to something passing the validator but often make no sense,
     * so they should be overwritten with something proper. This is intentional, so
     * they do not work by chance. The current defaults are:
     * <ul>
     * <li>the name "FakeRealm",</li>
     * <li>the current environment,</li>
     * <li>all paths beginning with "<code>#/invalidPath/</code>",</li>
     * <li>all hosts set to "localhost",</li>
     * <li>a port of 22 (SSH)</li>
     * <li>a unix user called "!fakeuser",</li>
     * <li>a time-out of 60</li>
     * <li>no PBS options.</li>
     * </ul>
     */
    public static final Map REALM_DEFAULTS = [
        name: DEFAULT_REALM_NAME,
        env: Environment.current.name,
        rootPath:           '/dev/null/otp-test/fakeRealm/root',
        processingRootPath: '/dev/null/otp-test/fakeRealm/processing',
        loggingRootPath:    '/dev/null/otp-test/fakeRealm/log',
        programsRootPath:   '/dev/null/otp-test/fakeRealm/programs',
        stagingRootPath:    '/dev/null/otp-test/fakeRealm/staging',
        webHost: DEFAULT_HOST,
        host: DEFAULT_HOST,
        port: DEFAULT_PORT,
        unixUser: '!fakeuser',
        timeout: 0,
        pbsOptions: '',
    ]

    /** Default settings for the BioQuant cluster. These include the {@link #REALM_DEFAULTS}. */
    public static final Map REALM_DEFAULTS_BIOQUANT_CLUSTER = REALM_DEFAULTS + [
        name: LABEL_BIOQUANT,
        cluster: Realm.Cluster.BIOQUANT,
        host: 'otphost-other.example.org',
        port: DEFAULT_PORT,
        unixUser: 'unixUser2',
        pbsOptions: '{"-l": {nodes: "1:xeon", walltime: "5:00"}}',
    ]

    /** Default settings for the DKFZ cluster. These include the {@link #REALM_DEFAULTS}. */
    public static final Map REALM_DEFAULTS_DKFZ_CLUSTER = REALM_DEFAULTS + [
        name: LABEL_DKFZ,
        cluster: Realm.Cluster.DKFZ,
        host: 'headnode',
        port: DEFAULT_PORT,
        unixUser: 'otptest',
        pbsOptions: '{"-l": {nodes: "1:lsdf", walltime: "30:00"}}',
    ]

    /**
     * Create a data management {@link Realm} for BioQuant with default cluster settings.
     * A map with additional values can be passed to add to or overwrite existing domain properties.
     *
     * @return a new data management {@link Realm} for the BioQuant
     */
    public static Realm createRealmDataManagementBioQuant(Map myProps = [:]) {
        new Realm(REALM_DEFAULTS_BIOQUANT_CLUSTER + [
            operationType: Realm.OperationType.DATA_MANAGEMENT,
        ] + myProps)
    }

    /**
     * Create a data processing {@link Realm} for BioQuant with default cluster settings for the DKFZ cluster.
     * A map with additional values can be passed to add to or overwrite existing domain properties.
     *
     * @return a new data processing {@link Realm} for the BioQuant, with settings for the DKFZ cluster
     */
    public static Realm createRealmDataProcessingBioQuant(Map myProps = [:]) {
        // NOTE: Data processing for BQ projects is done on DKFZ cluster, so the name needs to
        //       to be adjusted.
        new Realm(REALM_DEFAULTS_DKFZ_CLUSTER + [
            name: LABEL_BIOQUANT,
            operationType: Realm.OperationType.DATA_PROCESSING,
        ] + myProps)
    }

    /**
     * Create a data management {@link Realm} for DKFZ with default cluster settings.
     * A map with additional values can be passed to add to or overwrite existing domain properties.
     *
     * @return a new data management {@link Realm} for the DKFZ
     */
    public static Realm createRealmDataManagementDKFZ(Map myProps = [:]) {
        new Realm(REALM_DEFAULTS_DKFZ_CLUSTER + [
            operationType: Realm.OperationType.DATA_MANAGEMENT,
        ] + myProps)
    }

    /**
     * Create a data processing {@link Realm} for DKFZ with default cluster settings.
     * A map with additional values can be passed to add to or overwrite existing domain properties.
     *
     * @return a new data processing {@link Realm} for the DKFZ
     */
    public static Realm createRealmDataProcessingDKFZ(Map myProps = [:]) {
        new Realm(REALM_DEFAULTS_DKFZ_CLUSTER + [
            operationType: Realm.OperationType.DATA_PROCESSING,
        ] + myProps)
    }

    public static MergingSet createMergingSet(final MergingWorkPackage mergingWorkPackage) {
        return MergingSet.build(
                mergingWorkPackage: mergingWorkPackage,
                identifier: MergingSet.nextIdentifier(mergingWorkPackage),
        )
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        MergingSet mergingSet = createMergingSet(mergingWorkPackage)
        return createProcessedMergedBamFile(mergingSet, properties)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:]) {
        return createProcessedMergedBamFile(MergingSet.build(), properties)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet, Map properties = [:]) {
        assignNewProcessedBamFile(mergingSet)
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.build([
                mergingPass: MergingPass.build(
                        mergingSet: mergingSet,
                        identifier: MergingPass.nextIdentifier(mergingSet),
                ),
                numberOfMergedLanes: 1,
        ] + properties)
        return bamFile
    }

    public static ProcessedBamFile assignNewProcessedBamFile(final ProcessedMergedBamFile processedMergedBamFile) {
        final ProcessedBamFile bamFile = assignNewProcessedBamFile(processedMergedBamFile.mergingSet)
        processedMergedBamFile.numberOfMergedLanes++
        return bamFile
    }

    public static ProcessedBamFile assignNewProcessedBamFile(final MergingSet mergingSet) {
        final ProcessedBamFile bamFile = createProcessedBamFile(mergingSet.mergingWorkPackage)
        MergingSetAssignment.build(mergingSet: mergingSet, bamFile: bamFile)
        return bamFile
    }

    public static ProcessedBamFile createProcessedBamFile(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {

        SeqTrack seqTrack = buildSeqTrackWithDataFile(mergingWorkPackage)

        final ProcessedBamFile bamFile = ProcessedBamFile.build([
                alignmentPass: TestData.createAndSaveAlignmentPass(
                        seqTrack: seqTrack,
                        workPackage: mergingWorkPackage,
                        referenceGenome: mergingWorkPackage.referenceGenome,
                ),
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status: AbstractBamFile.State.PROCESSED,
        ] + properties)

        return bamFile
    }

    public static createRoddyBamFile(Map bamFileProperties = [:]) {
        SeqType seqType = SeqType.buildLazy(name: SeqTypeNames.WHOLE_GENOME.seqTypeName, libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        MergingWorkPackage workPackage = bamFileProperties.workPackage
        if (!workPackage) {
            Workflow workflow = Workflow.buildLazy(name: Workflow.Name.PANCAN_ALIGNMENT, type: Workflow.Type.ALIGNMENT)
            workPackage = MergingWorkPackage.build(workflow: workflow, seqType: seqType)
        }
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        ExternalScript externalScript = ExternalScript.buildLazy()
        RoddyBamFile bamFile = RoddyBamFile.build([
                numberOfMergedLanes: 1,
                seqTracks: [seqTrack],
                workPackage: workPackage,
                identifier: RoddyBamFile.nextIdentifier(workPackage),
                config: RoddyWorkflowConfig.buildLazy(workflow: workPackage.workflow, externalScriptVersion: externalScript.scriptVersion, obsoleteDate: null),
                md5sum: DEFAULT_MD5_SUM,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize: 10000,
                roddyVersion: ProcessingOption.build(),
                ] + bamFileProperties)

        assert bamFile.save(flush: true) // build-test-data does not flush, only saves
        bamFile.individual.project = bamFile.config.project
        assert bamFile.individual.save(flush: true)
        return bamFile
    }

    public static createRoddyBamFile(RoddyBamFile baseBamFile, Map bamFileProperties = [:]) {
        ExternalScript externalScript = ExternalScript.buildLazy()
        RoddyBamFile bamFile = RoddyBamFile.build([
                baseBamFile: baseBamFile,
                config: RoddyWorkflowConfig.build(workflow:  baseBamFile.config.workflow, externalScriptVersion: externalScript.scriptVersion),
                workPackage: baseBamFile.workPackage,
                identifier: baseBamFile.identifier + 1,
                numberOfMergedLanes: baseBamFile.numberOfMergedLanes + 1,
                seqTracks: [DomainFactory.buildSeqTrackWithDataFile(baseBamFile.workPackage)],
                md5sum: DEFAULT_MD5_SUM,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize: 10000,
                ] + bamFileProperties
        )
        bamFile.save(flush: true)
        return bamFile
    }

    public static SeqTrack buildSeqTrackWithDataFile(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:]) {
        return buildSeqTrackWithDataFile([
                sample: mergingWorkPackage.sample,
                seqType: mergingWorkPackage.seqType,
                seqPlatform: SeqPlatform.build(seqPlatformGroup: mergingWorkPackage.seqPlatformGroup),
        ] + seqTrackProperties)
    }

    public static SeqTrack buildSeqTrackWithDataFile(Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        SeqTrack seqTrack
        if (seqTrackProperties.seqType?.name == SeqTypeNames.EXOME.seqTypeName) {
            seqTrack = ExomeSeqTrack.build(seqTrackProperties)
        } else {
            seqTrack = SeqTrack.build(seqTrackProperties)
        }
        buildSequenceDataFile(dataFileProperties + [seqTrack: seqTrack])
        return seqTrack
    }

    public static DataFile buildSequenceDataFile(final Map properties = [:]) {
        return DataFile.build([
                fileType: FileType.buildLazy(type: Type.SEQUENCE),
                dateCreated: new Date(),  // In unit tests Grails (sometimes) does not automagically set dateCreated.
        ] + properties)
    }

    public static SnvCallingInstance createSnvCallingInstance(Map properties) {
        if (!properties.containsKey('latestDataFileCreationDate')) {
            properties += [latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(
                    properties.sampleType1BamFile, properties.sampleType2BamFile)]
        }
        return new SnvCallingInstance(properties)
    }

    public static ProcessingStep createAndSaveProcessingStep(String jobClass = "de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob") {
        final JobExecutionPlan jep = new JobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0, startJobBean: "DontCare")
        assert jep.save()
        final JobDefinition jobDefinition = new JobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert jobDefinition.save()
        final Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "DontCare", startJobVersion: "1")
        assert process.save()
        final ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process, jobClass: jobClass)
        assert step.save()
        final ProcessingStepUpdate update = createProcessingStepUpdate(step, ExecutionState.CREATED)
        assert update.save(flush: true)
        return step
    }

    public static ProcessingStepUpdate createProcessingStepUpdate(final ProcessingStep step, final ExecutionState state) {
        return new ProcessingStepUpdate(
                date: new Date(),
                state: state,
                previous: step.latestProcessingStepUpdate,
                processingStep: step,
        )
    }

    public static ProcessParameter createProcessParameter(final Process process, final Object parameterValue) {
        return new ProcessParameter(
                process: process,
                className: parameterValue.class.name,
                value: parameterValue.id.toString(),
        )
    }

    public static ProcessParameter createProcessParameter(final Process process, final String className, final String value) {
        return new ProcessParameter(
                process: process,
                className: className,
                value: value,
        )
    }

    public static ClusterJob createClusterJob(
            final ProcessingStep processingStep, final ClusterJobIdentifier clusterJobIdentifier,
            final Map myProps = [
                    clusterJobName: "testName_${processingStep.nonQualifiedJobClass}",
                    jobClass: processingStep.nonQualifiedJobClass,
                    queued: new DateTime(),
                    requestedWalltime: Duration.standardMinutes(5),
                    requestedCores: 10,
                    requestedMemory: 1000,
            ]) {
        return new ClusterJob([
                processingStep: processingStep,
                realm: clusterJobIdentifier.realm,
                clusterJobId: clusterJobIdentifier.clusterJobId,
        ] + myProps)
    }

    static List<SeqType> createAlignableSeqTypes() {
        [
            SeqTypeNames.EXOME,
            SeqTypeNames.WHOLE_GENOME
        ].collect {
            SeqType.build(name: it.seqTypeName, alias: it.seqTypeName, libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        }
    }

    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, String key, String value) {
        MetaDataKey metaDataKey = MetaDataKey.buildLazy(name: key)

        return MetaDataEntry.build(
            value: value,
            dataFile: dataFile,
            key: metaDataKey,
            status: MetaDataEntry.Status.VALID,
            source: MetaDataEntry.Source.MDFILE,
            )
    }

    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, MetaDataColumn key, String value) {
        return createMetaDataKeyAndEntry(dataFile, key.name(), value)
    }

    static void createRoddyProcessingOptions() {
        ProcessingOption processingOptionPath = new ProcessingOption(
            name: "roddyPath",
            type: "",
            project: null,
            value: "/path/to/roddy/",
            comment: "Path to the roddy.sh on the current cluster (***REMOVED***cluster 11.4)",
        )
        assert processingOptionPath.save(flush: true)

        ProcessingOption processingOptionVersion = new ProcessingOption(
            name: "roddyVersion",
            type: "",
            project: null,
            value: "2.1.28",
            comment: "Roddy version which is used currently to process Roddy-Pipelines"
        )
        assert processingOptionVersion.save(flush: true)

        ProcessingOption processingOptionBaseConfigsPath = new ProcessingOption(
                name: "roddyBaseConfigsPath",
                type: "",
                project: null,
                value: "/path/to/roddyBaseConfigs/",
                comment: "Path to the baseConfig-files which are needed to execute Roddy",
        )
        assert processingOptionBaseConfigsPath.save(flush: true)

        ProcessingOption processingOptionApplicationIni = new ProcessingOption(
                name: "roddyApplicationIni",
                type: "",
                project: null,
                value: "/path/to/roddyBaseConfigs/applicationProperties.ini",
                comment: "Path to the application.ini which is needed to execute Roddy"
        )
        assert processingOptionApplicationIni.save(flush: true)
    }

}
