package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import grails.util.Environment

class DomainFactory {

    private DomainFactory() {
    }

    static final String DEFAULT_HOST = 'localhost'
    static final int    DEFAULT_PORT = 22
    static final String LABEL_DKFZ = 'DKFZ'
    static final String LABEL_BIOQUANT = 'BioQuant'

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
        name: 'FakeRealm',
        env: Environment.current.name,
        rootPath:           '#/invalidPath/root',
        processingRootPath: '#/invalidPath/processing',
        loggingRootPath:    '#/invalidPath/log',
        programsRootPath:   '#/invalidPath/programs',
        webHost: DEFAULT_HOST,
        host: DEFAULT_HOST,
        port: DEFAULT_PORT,
        unixUser: '!fakeuser',
        timeout: 60,
        pbsOptions: '',
    ]

    /** Default settings for the BioQuant cluster. These include the {@link #REALM_DEFAULTS}. */
    public static final Map REALM_DEFAULTS_BIOQUANT_CLUSTER = REALM_DEFAULTS + [
        name: LABEL_BIOQUANT,
        cluster: Realm.Cluster.BIOQUANT,
        host: 'otphost-other.example.org',
        port: DEFAULT_PORT,
        unixUser: 'unixUser2',
        pbsOptions: '{"-l": {nodes: "1:xeon", walltime: "5:00"}, "-W": {x: "NACCESSPOLICY:SINGLEJOB"}}',
    ]

    /** Default settings for the DKFZ cluster. These include the {@link #REALM_DEFAULTS}. */
    public static final Map REALM_DEFAULTS_DKFZ_CLUSTER = REALM_DEFAULTS + [
        name: LABEL_DKFZ,
        cluster: Realm.Cluster.DKFZ,
        host: 'headnode',
        port: DEFAULT_PORT,
        unixUser: 'otptest',
        pbsOptions: '{"-l": {nodes: "1:lsdf", walltime: "5:00"}}',
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

    public static ProcessingStep createAndSaveProcessingStep() {
        final JobExecutionPlan jep = new JobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0, startJobBean: "DontCare")
        assert jep.save()
        final JobDefinition jobDefinition = new JobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert jobDefinition.save()
        final Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "DontCare", startJobVersion: "1")
        assert process.save()
        final ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
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

    public static ClusterJob createClusterJob(
            final ProcessingStep processingStep, final ClusterJobIdentifier clusterJobIdentifier,
            final Map myProps = [
                    clusterJobName: "testName",
                    queued: new Date(),
                    requestedWalltime: 24 * 60 * 60,
                    requestedCores: 10,
                    requestedMemory: 1000,
            ]) {
        return new ClusterJob([
                processingStep: processingStep,
                realm: clusterJobIdentifier.realm,
                clusterJobId: clusterJobIdentifier.clusterJobId,
        ] + myProps)
    }
}
