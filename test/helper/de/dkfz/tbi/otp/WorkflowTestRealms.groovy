package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

class WorkflowTestRealms {

    static final String LABEL_DKFZ = 'DKFZ'
    static final int    DEFAULT_PORT = 22

    public static final Map REALM_DEFAULTS_DKFZ_CLUSTER = [
            name: LABEL_DKFZ,
            cluster: Realm.Cluster.DKFZ,
            host: 'headnode',
            port: DEFAULT_PORT,
            unixUser: 'otptest',
            timeout: 0,
            pbsOptions: '{"-l": {nodes: "1:lsdf", walltime: "30:00"}}',
    ]

    /**
     * Create a data management {@link Realm} for DKFZ with default cluster settings.
     * A map with additional values can be passed to add to or overwrite existing domain properties.
     *
     * @return a new data management {@link Realm} for the DKFZ
     */
    public static Realm createRealmDataManagementDKFZ(Map myProps = [:]) {
        DomainFactory.createRealm(REALM_DEFAULTS_DKFZ_CLUSTER + [
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
        DomainFactory.createRealm(REALM_DEFAULTS_DKFZ_CLUSTER + [
                operationType: Realm.OperationType.DATA_PROCESSING,
        ] + myProps)
    }
}
