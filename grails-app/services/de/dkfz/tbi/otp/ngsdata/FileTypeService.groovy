package de.dkfz.tbi.otp.ngsdata

class FileTypeService {

    static transactional = true

    ///////////////////////////////////////////////////////////////////////////

    private void safeSave(def obj) {
        //
        // probably will go to separate static class
        // no formal exception, information only
        //

        obj.validate()
        if (obj.hasErrors()) {
            println obj.errors
            return
        }

        if (!obj.save())
            println "can not save ${obj}"
    }


    /**
     * 
     * This methods loads database with all file types
     * known in the syste,
     * 
     * This method is executed only at bootstrap
     * 
     */
    void loadFileTypes() {

        FileType fileType

        //
        // initial files
        // without sub-files and signatures
        // 

        def fileTypes = [
            FileType.Type.SOURCE,
            FileType.Type.METADATA,
            FileType.Type.MERGED,
            FileType.Type.RESULT,
            FileType.Type.UNKNOWN
        ]

        fileTypes.each {
            fileType = new FileType(type: it)
            safeSave(fileType)
        }

        // sequence files
        def fileTypesSeq = [
            ["srf", ".srf", "/srf/"],
            ["seq", "sequence.txt", "/sequence/"],
            ["fastq", ".fastq", "/fastq/"],
            ["csfasta", ".csfasta", "/cs/"],
            ["qual", "qual", "/cs/"],
            ["stats","stats", "/cs/"]
        ]

        fileTypesSeq.each {

            fileType = new FileType(
                type: FileType.Type.SEQUENCE,
                subType: it[0],
                signature: it[1],
                vbpPath: it[2]
            )
            safeSave(fileType)
        }


        // alignment files
        def fileTypesAlign = [
            ["sequence","sequence.sam", "/sam/"],
            ["bwt", "bwtout", "/bwtout/"],
            ["export", "export.txt", "/export/"],
            ["bam", ".bam", "/bam/"],
            ["bai", ".bai", "/bam/"],
            ["stats", ".stats", "/bam/"]
        ]

        fileTypesAlign.each {

            fileType = new FileType(
                type: FileType.Type.ALIGNMENT,
                subType: it[0],
                signature: it[1],
                vbpPath: it[2]
            )
            safeSave(fileType)
        }
    }
    
	///////////////////////////////////////////////////////////////////////////
		
	FileType getFileType(String filename) {
		//
		// try to provide and object from file name
		//
		//
		
		def fileTypeList = FileType.findAll()
		FileType tt = null
		
		fileTypeList.each {
			if (filename.contains(it.signature)) tt = it
		}
		
		if (!tt) tt = FileType.findByType(FileType.Type.UNKNOWN)
		tt
	}
		
	///////////////////////////////////////////////////////////////////////////
		
	FileType getFileType(String filename, FileType.Type type) {
		//
		// provides object from file name and known type
		// (to make a difference between stats from sequence and alignment)
		//
		
		def fileTypeList = FileType.findAll()
		FileType tt = null
		
		fileTypeList.each {
			if (it.type != type) return
			if (filename.contains(it.signature)) tt = it
		}
		
		if (!tt) tt = FileType.findByType(FileType.Type.UNKNOWN)
		tt
	}
		
	///////////////////////////////////////////////////////////////////////////
		
		
}
