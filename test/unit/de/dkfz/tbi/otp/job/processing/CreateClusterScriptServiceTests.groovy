package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.*
import org.junit.*


@TestFor(CreateClusterScriptService)
class CreateClusterScriptServiceTests {
    @Test
    void test_makeDirs() {
        String result = service.makeDirs([new File("/asdf"), new File("/qwertz")], "777")
        assert result == 'umask 000; mkdir --parents --mode 777 /asdf /qwertz &>/dev/null; echo $?'

        result = service.makeDirs([new File("/asdf")], "750")
        assert result == 'umask 027; mkdir --parents --mode 750 /asdf &>/dev/null; echo $?'

        result = service.makeDirs([new File("/asdf")])
        assert result == ' mkdir --parents  /asdf &>/dev/null; echo $?'
    }
}
