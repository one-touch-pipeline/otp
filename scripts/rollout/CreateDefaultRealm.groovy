import de.dkfz.tbi.otp.ngsdata.*
import grails.util.*

//create default realm
Realm.withTransaction {
    [
            Realm.OperationType.DATA_MANAGEMENT,
            Realm.OperationType.DATA_PROCESSING,
    ].each {
        Realm realm = new Realm([
                name                       : Realm.LATEST_DKFZ_REALM,
                env                        : Environment.PRODUCTION,
                operationType              : it,
                rootPath                   : 'TODO', //will be removed
                processingRootPath         : 'TODO', //will be removed
                loggingRootPath            : 'TODO', //will be removed
                programsRootPath           : 'TODO', //will be removed
                stagingRootPath            : 'TODO', //will be removed
                webHost                    : 'test.host.invalid', //will be removed
                jobScheduler               : Realm.JobScheduler.PBS,
                host                       : 'test.host.invalid',
                port                       : -1,
                unixUser                   : '!fakeuser',
                timeout                    : -1,
                defaultJobSubmissionOptions: '',
                cluster                    : Realm.Cluster.DKFZ,
        ])

        assert realm.save(flush: true)
    }
}
''
