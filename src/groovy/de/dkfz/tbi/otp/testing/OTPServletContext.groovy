package de.dkfz.tbi.otp.testing

import javax.servlet.*
import javax.servlet.descriptor.JspConfigDescriptor
import org.apache.commons.io.FileUtils

@SuppressWarnings(["UnusedMethodParameter", "MissingOverrideAnnotation"])
class OTPServletContext implements ServletContext {
    String getRealPath(String relativePath) {
        return System.getProperty("user.dir") + relativePath
    }
    String getContextPath() {
        return null
    }
    Object getAttribute(String arg0) {
        if (arg0 == "javax.servlet.context.tempdir") {
            File tempDir = new File(System.getProperty("user.dir") + "/target/tmp")
            FileUtils.forceMkdir(tempDir)
            return tempDir
        }
        return null
    }
    Enumeration<String> getAttributeNames() {
        return null
    }
    ServletContext getContext(String arg0) {
        return null
    }
    String getInitParameter(String arg0) {
        return null
    }
    Enumeration getInitParameterNames() {
        return null
    }
    int getMajorVersion() {
        return null
    }
    String getMimeType(String arg0) {
        return null
    }
    int getMinorVersion() {
        return null
    }
    RequestDispatcher getNamedDispatcher(String arg0) {
        return null
    }
    RequestDispatcher getRequestDispatcher(String arg0) {
        return null
    }
    URL getResource(String arg0) throws MalformedURLException {
        return null
    }
    InputStream getResourceAsStream(String arg0) {
        return null
    }
    Set getResourcePaths(String arg0) {
        return null
    }
    String getServerInfo() {
        return null
    }
    Servlet getServlet(String arg0) throws ServletException {
        return null
    }
    String getServletContextName() {
        return null
    }
    Enumeration getServletNames() {
        return null
    }
    Enumeration getServlets() {
        return null
    }
    void log(java.lang.Exception exception, java.lang.String msg) {
        return null
    }
    void log(String arg0) {
        return null
    }
    void log(String arg0, Throwable arg1) {
        return null
    }
    void removeAttribute(String arg0) {
        return null
    }
    void setAttribute(String arg0, Object obj) {
        return null
    }
    JspConfigDescriptor getJspConfigDescriptor() {
        return null
    }
    ServletRegistration$Dynamic addServlet(String arg0, String arg1) {
        return null
    }
    ServletRegistration$Dynamic addServlet(String arg0, Servlet arg1) {
        return null
    }
    FilterRegistration getFilterRegistration(String arg0) {
        return null
    }
    Map getFilterRegistrations() {
        return null
    }
    transient void declareRoles(String[] arg0) {
        return null
    }
    ServletRegistration$Dynamic addServlet(String arg0, Class arg1) {
        return null
    }
    ClassLoader getClassLoader() {
        return null
    }
    Set getDefaultSessionTrackingModes() {
        return null
    }
    ServletRegistration getServletRegistration(String arg0) {
        return null
    }
    Map getServletRegistrations() {
        return null
    }
    void addListener(Class arg0) {
        return null
    }
    void addListener( arg0) {
        return null
    }
    void addListener(String arg0) {
        return null
    }
    FilterRegistration$Dynamic addFilter(String arg0, Class arg1) {
        return null
    }
    FilterRegistration$Dynamic addFilter(String arg0, Filter arg1) {
        return null
    }
    FilterRegistration$Dynamic addFilter(String arg0, String arg1) {
        return null
    }
    def createFilter(Class arg0) throws ServletException {
        return null
    }
    def createListener(Class arg0) throws ServletException {
        return null
    }
    def createServlet(Class arg0) throws ServletException {
        return null
    }
    int getEffectiveMajorVersion() {
        return null
    }
    int getEffectiveMinorVersion() {
        return null
    }
    Set getEffectiveSessionTrackingModes() {
        return null
    }
    SessionCookieConfig getSessionCookieConfig() {
        return null
    }
    boolean setInitParameter(String arg0, String arg1) {
        return null
    }
    void setSessionTrackingModes(Set arg0) throws IllegalStateException ,IllegalArgumentException {
        return null
    }
}
