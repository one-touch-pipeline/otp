package de.dkfz.tbi.otp.job.scheduler

import groovy.xml.MarkupBuilder
import java.lang.reflect.Field
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import org.codehaus.groovy.grails.plugins.codecs.MD5Codec
import javax.servlet.ServletContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder

/**
 * Service for logging exceptions to files
 * 
 * This Service gets exceptions from jobs and stores them in files.
 * Each job should write each different exception type to a dedicated file.
 * As file name the MD5 sum of job's name and the occurred are taken to ensure this.
 * The content of the files is organized as xml to have a clear structure.
 * If a specific exception occurs several times for each job only the timestamp
 * with the point in time when the exception occurred is appended as exception itself
 * is by definition of the file name completely equivalent to the already stored.
 * 
 *
 */
class ErrorLogService {

    /**
     * Dependency injection of grails Application
     */
    def grailsApplication

   /**
    * Dependency injection of Servlet Context
    */
   def servletContext

    /**
     * The central method coordinating the service's functionality
     * 
     * The method makes the functionality the service provides publicly available.
     * The MD5 sum is calculated and taken as name of the file storing
     * the stacktraces of occurred exceptions.
     * Here are the methods triggered forming the xml around contents to be stored
     * and writing the file to the file system.
     * 
     * @param jobClassName The name of the job of which an exception is to be stored
     * @param thrownException The thrown exception
     */
    public void writeStacktraceToFile(String jobClassName, Exception thrownException) {
        String md5Sum = (jobClassName + thrownException.toString()).encodeAsMD5()
        String fileName = md5Sum + ".xml"
        File exceptionStoringFile = new File(fileName)
        String dir = servletContext.getRealPath(grailsApplication.config.otp.errorLogging.stacktraces)
        String xml
        String existingFilePath = dir + "/" + fileName
        if (new File(existingFilePath).isFile()) {
            xml = addToXml(existingFilePath)
        } else {
            xml = contentToXml(jobClassName, thrownException)
        }
        writeToFile(dir, fileName, [xml])
    }

    /**
     * Writes the exception to a file
     * 
     * The exception is stored to a file which is written to the file system
     * to a configurable directory.
     * 
     * @param directory The directory to contain the file
     * @param fileName The fileName to carry the exception
     * @param content The content to be stored
     */
    private void writeToFile(String directory, String fileName, List content) {
        if (! new File(directory).isDirectory()) {
            new File(directory).mkdir()
        }
        def file = new File(directory,"${fileName}")
        file.withWriter { out ->
            content.each {
            out.println it
          }
        }
      }

    /**
     * Transforms the exception to a string
     * 
     * @param thrownException The exception to store
     * @return String containing the Exception
     */
    private String stack2string(Exception thrownException) {
        try {
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            thrownException.printStackTrace(pw)
            return sw.toString()
        } catch(Exception e2) {
            return "bad stack2string"
        }
    }

    /**
     * Wraps a new combination of job name and exception in xml
     * 
     * The method is called when a new combination of job and exception
     * occurs and therefore a file containing this combination is not yet existent.
     * It wraps the exception's stacktrace together with the job's name and a timestamp
     * in xml.
     * 
     * @param jobClassName The JobClassName of the job where the Exception occurred
     * @param thrownException The ThrownException to be stored
     * @return String with complete xml file.
     */
    private String contentToXml(String jobClassName, Exception thrownException) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        String stackString = stack2string(thrownException)
        xml.stacktraceElement(jobClass: jobClassName, exceptionMessage: thrownException.message) {
                stacktrace(stackString)
                timestamp(new Date())
        }
        return writer.toString()
    }

    /**
     * Adds a new timestamp to an existent xml file
     * 
     * @param existingFilePath The existingFilePath to the xml file
     * @return String containing the edited xml file
     */
    private String addToXml(String existingFilePath) {
        File exceptionFile = new File(existingFilePath)
        def root = new XmlParser().parse(exceptionFile)
        String recentDate = new Date().toString()
        root.appendNode('timestamp', recentDate)
        def writer = new StringWriter()
        new XmlNodePrinter(new PrintWriter(writer)).print(root)
        return writer.toString()
    }
}
