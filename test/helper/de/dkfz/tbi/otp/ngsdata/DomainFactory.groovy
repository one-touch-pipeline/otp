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

    public static final Map REALM_DEFAULTS_BIOQUANT_CLUSTER = REALM_DEFAULTS + [
        name: LABEL_BIOQUANT,
        cluster: Realm.Cluster.BIOQUANT,
        host: 'otphost-other.example.org',
        port: DEFAULT_PORT,
        unixUser: 'unixUser2',
        pbsOptions: '{"-l": {nodes: "1:xeon", walltime: "5:00"}, "-W": {x: "NACCESSPOLICY:SINGLEJOB"}}',
    ]

    public static final Map REALM_DEFAULTS_DKFZ_CLUSTER = REALM_DEFAULTS + [
        name: LABEL_DKFZ,
        cluster: Realm.Cluster.DKFZ,
        host: 'headnode',
        port: DEFAULT_PORT,
        unixUser: 'otptest',
        pbsOptions: '{"-l": {nodes: "1:lsdf", walltime: "5:00"}}',
    ]

    public static Realm createRealmDataManagementBioQuant(Map myProps = [:]) {
        new Realm(REALM_DEFAULTS_BIOQUANT_CLUSTER + [
            operationType: Realm.OperationType.DATA_MANAGEMENT,
        ] + myProps)
    }

    public static Realm createRealmDataProcessingBioQuant(Map myProps = [:]) {
        // NOTE: Data processing for BQ projects is done on DKFZ cluster, so the name needs to
        //       to be adjusted.
        new Realm(REALM_DEFAULTS_DKFZ_CLUSTER + [
            name: LABEL_BIOQUANT,
            operationType: Realm.OperationType.DATA_PROCESSING,
        ] + myProps)
    }

    public static Realm createRealmDataManagementDKFZ(Map myProps = [:]) {
        new Realm(REALM_DEFAULTS_DKFZ_CLUSTER + [
            operationType: Realm.OperationType.DATA_MANAGEMENT,
        ] + myProps)
    }

    public static Realm createRealmDataProcessingDKFZ(Map myProps = [:]) {
        new Realm(REALM_DEFAULTS_DKFZ_CLUSTER + [
            operationType: Realm.OperationType.DATA_PROCESSING,
        ] + myProps)
    }
}
