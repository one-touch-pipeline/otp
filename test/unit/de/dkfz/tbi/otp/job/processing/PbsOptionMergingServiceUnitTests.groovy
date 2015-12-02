package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(PbsOptionMergingService.class)
class PbsOptionMergingServiceUnitTests {

    @Test
    void testJsonStringToMap() {
        PbsOptionMergingService pbsOptionMergingService = new PbsOptionMergingService()
        assertEquals([:], pbsOptionMergingService.jsonStringToMap("{}"))
        assertEquals([a: 'a'], pbsOptionMergingService.jsonStringToMap("{a: a}"))
        assertEquals([a: 'a', b: 'b'], pbsOptionMergingService.jsonStringToMap("{a: a, b: b}"))
        assertEquals([a: [aa: 'aa']], pbsOptionMergingService.jsonStringToMap("{a: {aa: aa}}"))

        assertEquals([a: [aa: 'aa', ab: 'ab',  ac: 'ac'], b: [bb: 'bb', bc: 'bc',  bd: 'bd'], c: 'c', d: 'd', e: 'e'],
        pbsOptionMergingService.jsonStringToMap("{a: {aa: aa, ab: ab, ac: ac}, b: {bb: bb, bc: bc,  bd: bd}, c: c, d: d, e: e}"))

        assertEquals(['-l': ['nodes': '1', walltime: '48:00:00']], pbsOptionMergingService.jsonStringToMap("{'-l': {nodes: '1', walltime: '48:00:00'}}"))
        assertEquals(['-W': ['x': 'NACCESSPOLICY:SINGLEJOB']], pbsOptionMergingService.jsonStringToMap("{'-W': {x: 'NACCESSPOLICY:SINGLEJOB'}}"))

        assertEquals(['-l': [nodes: '1:ppn=6', walltime: '48:00:00', mem: '3g', file: '50g'], '-q': 'convey', '-A': 'RUNFAST', '-m': 'a', '-S': '/bin/bash'],
        pbsOptionMergingService.jsonStringToMap("{'-l': {nodes: '1:ppn=6', walltime: '48:00:00', mem: '3g', file: '50g'}, '-q': convey, '-A': RUNFAST, '-m': a, '-S': '/bin/bash'}"))

        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap(null) } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap("") } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap("{") } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap("}") } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap("{:}") } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap("{{}") } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.jsonStringToMap("{a}") } )
    }

    @Test
    void testMapToPbsOptions() {
        PbsOptionMergingService pbsOptionMergingService = new PbsOptionMergingService()
        assertEquals("", pbsOptionMergingService.mapToPbsOptions([:]))
        assertEquals("-a b ", pbsOptionMergingService.mapToPbsOptions(['-a': 'b']))
        assertEquals("-a b -c d ", pbsOptionMergingService.mapToPbsOptions(['-a': 'b', '-c': 'd']))
        assertEquals("-a b=c -a d=e ", pbsOptionMergingService.mapToPbsOptions(['-a': [b: 'c', d: 'e']]))
        assertEquals("-a b=c -a d=e -a f=g -h i=j -h k=l -m n ", pbsOptionMergingService.mapToPbsOptions(['-a': [b: 'c', d: 'e', f: 'g'], '-h': [i: 'j', k: 'l'], '-m': 'n']))

        assertEquals("-l nodes=1 -l walltime=48:00:00 ", pbsOptionMergingService.mapToPbsOptions(['-l': ['nodes': '1', walltime: '48:00:00']]))
        assertEquals("-W x=NACCESSPOLICY:SINGLEJOB ", pbsOptionMergingService.mapToPbsOptions(['-W': ['x': 'NACCESSPOLICY:SINGLEJOB']]))

        assertEquals("-l nodes=1:ppn=6 -l walltime=48:00:00 -l mem=3g -l file=50g -q convey -A RUNFAST -m a -S /bin/bash ",
                pbsOptionMergingService.mapToPbsOptions(['-l': [nodes: '1:ppn=6', walltime: '48:00:00', mem: '3g', file: '50g'], '-q': 'convey', '-A': 'RUNFAST', '-m': 'a', '-S': '/bin/bash']))

        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mapToPbsOptions(null) } )
        shouldFail(RuntimeException.class, { pbsOptionMergingService.mapToPbsOptions("") } )
        shouldFail(RuntimeException.class, { pbsOptionMergingService.mapToPbsOptions(1) } )
    }

    @Test
    void testMergeHelper() {
        PbsOptionMergingService pbsOptionMergingService = new PbsOptionMergingService()
        assertEquals([:], pbsOptionMergingService.mergeHelper([:], [:]))
        assertEquals([a: 'a'], pbsOptionMergingService.mergeHelper([a: 'a'], [:]))
        assertEquals([a: 'a'], pbsOptionMergingService.mergeHelper([:], [a: 'a']))

        assertEquals([a: 'a', b: 'b'], pbsOptionMergingService.mergeHelper([a: 'a', b: 'b'], [:]))
        assertEquals([a: 'a', b: 'b'], pbsOptionMergingService.mergeHelper([:], [a: 'a', b: 'b']))
        assertEquals([a: 'a', b: 'b'], pbsOptionMergingService.mergeHelper([a: 'a'], [b: 'b']))

        assertEquals([a: 'a2'], pbsOptionMergingService.mergeHelper([a: 'a'], [a: 'a2']))
        assertEquals([a: 'a2', b: 'b'], pbsOptionMergingService.mergeHelper([a: 'a', b: 'b'], [a: 'a2']))
        assertEquals([a: 'a', b: 'b2'], pbsOptionMergingService.mergeHelper([a: 'a', b: 'b'], [b: 'b2']))
        assertEquals([a: 'a2', b: 'b2'], pbsOptionMergingService.mergeHelper([a: 'a', b: 'b'], [a: 'a2', b: 'b2']))

        assertEquals([a: 'a2', c: 'c', b: 'b'], pbsOptionMergingService.mergeHelper([a: 'a', b: 'b'], [a: 'a2', c: 'c']))

        assertEquals([a: [aa: 'aa']], pbsOptionMergingService.mergeHelper([a: [aa: 'aa']], [:]))
        assertEquals([a: [aa: 'aa']], pbsOptionMergingService.mergeHelper([:], [a: [aa: 'aa']]))

        assertEquals([a: [aa: 'aa'], b: 'b'], pbsOptionMergingService.mergeHelper([a: [aa: 'aa'], b: 'b'], [:]))
        assertEquals([a: [aa: 'aa'], b: 'b'], pbsOptionMergingService.mergeHelper([:], [a: [aa: 'aa'], b: 'b']))
        assertEquals([a: [aa: 'aa'], b: 'b'], pbsOptionMergingService.mergeHelper([a: [aa: 'aa']], [b: 'b']))
        assertEquals([a: [aa: 'aa'], b: [bb: 'bb']], pbsOptionMergingService.mergeHelper([a: [aa: 'aa'], b: [bb: 'bb']], [:]))
        assertEquals([a: [aa: 'aa'], b: [bb: 'bb']], pbsOptionMergingService.mergeHelper([:], [a: [aa: 'aa'], b: [bb: 'bb']]))
        assertEquals([a: [aa: 'aa'], b: [bb: 'bb']], pbsOptionMergingService.mergeHelper([a: [aa: 'aa']], [b: [bb: 'bb']]))

        assertEquals([a: [aa: 'aa2']], pbsOptionMergingService.mergeHelper([a: [aa: 'aa']], [a: [aa: 'aa2']]))
        assertEquals([a: [aa: 'aa2'], b: 'b'], pbsOptionMergingService.mergeHelper([a: [aa: 'aa'], b: 'b'], [a: [aa: 'aa2']]))
        assertEquals([a: [aa: 'aa2'], b: 'b'], pbsOptionMergingService.mergeHelper([a: [aa: 'aa']], [a: [aa: 'aa2'], b: 'b']))
        assertEquals([a: [aa: 'aa2'], b: 'b', c: 'c'], pbsOptionMergingService.mergeHelper([a: [aa: 'aa'], b: 'b'], [a: [aa: 'aa2'], c: 'c']))

        assertEquals([a: [aa: 'aa', ab: 'ab']], pbsOptionMergingService.mergeHelper([a: [aa: 'aa']], [a: [ab: 'ab']]))
        assertEquals([a: [aa: 'aa', ab: 'ab2',  ac: 'ac']], pbsOptionMergingService.mergeHelper([a: [aa: 'aa', ab: 'ab']], [a: [ab: 'ab2', ac: 'ac']]))

        assertEquals([a: [aa: 'aa', ab: 'ab2',  ac: 'ac'], b: [bb: 'bb', bc: 'bc2',  bd: 'bd'], c: 'c', d: 'd2', e: 'e'],
        pbsOptionMergingService.mergeHelper([a: [aa: 'aa', ab: 'ab'], b: [bb: 'bb', bc: 'bc'], c: 'c', d: 'd'],
        [a: [ab: 'ab2', ac: 'ac'], b: [bc: 'bc2', bd: 'bd'], d: 'd2', e: 'e']))

        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergeHelper(null, [:]) } )
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergeHelper([:], null) } )
        shouldFail(RuntimeException.class, { pbsOptionMergingService.mergeHelper([a: 'b'], [a: [aa: 'bb']]) } )
    }
}
