package de.dkfz.tbi.otp.ngsdata

import grails.util.Environment
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType

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
}
