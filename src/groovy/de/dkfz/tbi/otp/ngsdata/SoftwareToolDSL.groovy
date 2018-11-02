package de.dkfz.tbi.otp.ngsdata

class SoftwareToolDSL {

    static def softwareToolDef = { String programName, SoftwareTool.Type type, c1 ->

        c1.version = { String programVersion, c2 ->
            SoftwareTool tool = new SoftwareTool(programName: programName, programVersion: programVersion, type: type)
            assert(tool.save())
            c2.name = { String name ->
                assert((new SoftwareToolIdentifier(name: name, softwareTool: tool)).save())
            }
            c2()
            assert(tool.save(flush: true))
        }
        c1()
    }
}
