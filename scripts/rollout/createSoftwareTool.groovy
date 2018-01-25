import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.codehaus.groovy.grails.plugins.web.taglib.*

String programName = ''
String programVersion = ''

//****************************************

assert programName : 'Please provide a program name'
assert programVersion : 'Please provide a program version'

SoftwareTool softwareTool = CollectionUtils.atMostOneElement(
        SoftwareTool.findAllByProgramNameAndProgramVersionAndType(programName, programVersion, SoftwareTool.Type.BASECALLING))

assert !softwareTool: 'The software tool already exist'

SoftwareTool.withTransaction {
    softwareTool = new SoftwareTool(
            programName: programName,
            programVersion: programVersion,
            type: SoftwareTool.Type.BASECALLING,
    )
    println softwareTool.save(flush: true)

    println 'Please add a SoftwareToolIdentifier on ' + ctx.getBean(ApplicationTagLib).createLink(
            controller: 'softwareTool', action: 'list', absolute: 'true')
}
''
