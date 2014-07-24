import grails.util.Environment
import de.dkfz.tbi.otp.ngsdata.*

testDataConfig {
    sampleData {
        'de.dkfz.tbi.otp.ngsdata.Realm' {
            name = 'FakeRealm'
            env = Environment.current.name
            rootPath = '/tmp/otp-unit-test/root'
            processingRootPath = '/tmp/otp-unit-test/processing'
            loggingRootPath = '/tmp/otp-unit-test/log'
            programsRootPath = '/tmp/otp-unit-test/programs'
            webHost = 'localhost'
            host = 'localhost'
            port = 22
            unixUser = '!fakeuser'
            timeout = 60
            pbsOptions = ''
            name = 'DKFZ'
            cluster = Realm.Cluster.DKFZ
        }
    }
}


//disable plugin in production enviromant
environments {
    production {
        testDataConfig {
            enabled = false
        }
    }
}
