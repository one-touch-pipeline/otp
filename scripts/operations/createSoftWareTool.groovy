import de.dkfz.tbi.otp.ngsdata.*


String programName = ''
String programVersion = ''


//****************************************

assert programName : 'Please provide a program name'
assert programVersion : 'Please provide a program version'

SoftwareTool softwareTool = SoftwareTool.findAllByProgramNameAndProgramVersionAndType(programName, programVersion, "BASECALLING")

assert !softwareTool: 'The software tool already exist'

SoftwareTool.withTransaction {
    softwareTool = new SoftwareTool(
            programName: programName,
            programVersion: programVersion,
            type: "BASECALLING",
    )
    println softwareTool.save(flush: true)

    SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier(
            softwareTool: softwareTool,
            name: programVersion,
    )
    println softwareToolIdentifier.save(flush: true)
}
''
