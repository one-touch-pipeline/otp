package de.dkfz.tbi.otp.ngsdata

import grails.util.Environment
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType

class DomainFactory {

    private DomainFactory() {
    }

    public static final Map REALM_DEFAULTS = [
        name: 'FakeRealm',
        env: Environment.current.name,
        rootPath:           '#/invalidPath/root',
        processingRootPath: '#/invalidPath/processing',
        loggingRootPath:    '#/invalidPath/log',
        programsRootPath:   '#/invalidPath/programs',
        webHost: 'localhost',
        host: 'localhost',
        port: 22,
        unixUser: '!fakeuser',
        timeout: 60,
        pbsOptions: '',
    ]

    public static final Map REALM_DEFAULTS_DKFZ = REALM_DEFAULTS + [
        name: 'DKFZ',
        cluster: Realm.Cluster.DKFZ,
        host: 'headnode',
        port: 22,
        unixUser: 'otptest',
        pbsOptions: '{"-l": {nodes: "1:lsdf", walltime: "5:00"}}',
    ]

    public static Realm createRealmDataManagementDKFZ(Map myProps = null) {
        new Realm(REALM_DEFAULTS_DKFZ + [
            operationType: Realm.OperationType.DATA_MANAGEMENT,
        ] + myProps)
    }

    public static Realm createRealmDataProcessingDKFZ(Map myProps = null) {
        new Realm(REALM_DEFAULTS_DKFZ + [
            operationType: Realm.OperationType.DATA_PROCESSING,
        ] + myProps)
    }
}
