package de.dkfz.tbi.otp.testing

import javax.servlet.*
import javax.servlet.descriptor.JspConfigDescriptor
import org.apache.commons.io.FileUtils

@SuppressWarnings("UnusedMethodParameter")
@SuppressWarnings("MissingOverrideAnnotation")
class OTPServletContext implements ServletContext {
    public String getRealPath(String relativePath) {
        return System.getProperty("user.dir") + relativePath
    }
    public String getContextPath() {
        return null
    }
    public Object getAttribute(String arg0) {
        if (arg0 == "javax.servlet.context.tempdir") {
            File tempDir = new File(System.getProperty("user.dir") + "/target/tmp")
            FileUtils.forceMkdir(tempDir)
            return tempDir
        }
        return null
    }
    public Enumeration<String> getAttributeNames() {
        return null
    }
    public ServletContext getContext(String arg0) {
        return null
    }
    public String getInitParameter(String arg0) {
        return null
    }
    public Enumeration getInitParameterNames() {
        return null
    }
    public int getMajorVersion() {
        return null
    }
    public String getMimeType(String arg0) {
        return null
    }
    public int getMinorVersion() {
        return null
    }
    public RequestDispatcher getNamedDispatcher(String arg0) {
        return null
    }
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null
    }
    public URL getResource(String arg0) throws MalformedURLException {
        return null
    }
    public InputStream getResourceAsStream(String arg0) {
        return null
    }
    public Set getResourcePaths(String arg0) {
        return null
    }
    public String getServerInfo() {
        return null
    }
    public Servlet getServlet(String arg0) throws ServletException {
        return null
    }
    public String getServletContextName() {
        return null
    }
    public Enumeration getServletNames() {
        return null
    }
    public Enumeration getServlets() {
        return null
    }
    public void log(java.lang.Exception exception, java.lang.String msg) {
        return null
    }
    public void log(String arg0) {
        return null
    }
    public void log(String arg0, Throwable arg1) {
        return null
    }
    public void removeAttribute(String arg0) {
        return null
    }
    public void setAttribute(String arg0, Object obj) {
        return null
    }
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null
    }
    public ServletRegistration$Dynamic addServlet(String arg0, String arg1) {
        return null
    }
    public ServletRegistration$Dynamic addServlet(String arg0, Servlet arg1) {
        return null
    }
    public FilterRegistration getFilterRegistration(String arg0) {
        return null
    }
    public Map getFilterRegistrations() {
        return null
    }
    public transient void declareRoles(String[] arg0) {
        return null
    }
    public ServletRegistration$Dynamic addServlet(String arg0, Class arg1) {
        return null
    }
    public ClassLoader getClassLoader() {
        return null
    }
    public Set getDefaultSessionTrackingModes() {
        return null
    }
    public ServletRegistration getServletRegistration(String arg0) {
        return null
    }
    public Map getServletRegistrations() {
        return null
    }
    public void addListener(Class arg0) {
        return null
    }
    public void addListener( arg0) {
        return null
    }
    public void addListener(String arg0) {
        return null
    }
    public FilterRegistration$Dynamic addFilter(String arg0, Class arg1) {
        return null
    }
    public FilterRegistration$Dynamic addFilter(String arg0, Filter arg1) {
        return null
    }
    public FilterRegistration$Dynamic addFilter(String arg0, String arg1) {
        return null
    }
    public def createFilter(Class arg0) throws ServletException {
        return null
    }
    public def createListener(Class arg0) throws ServletException {
        return null
    }
    public def createServlet(Class arg0) throws ServletException {
        return null
    }
    public int getEffectiveMajorVersion() {
        return null
    }
    public int getEffectiveMinorVersion() {
        return null
    }
    public Set getEffectiveSessionTrackingModes() {
        return null
    }
    public SessionCookieConfig getSessionCookieConfig() {
        return null
    }
    public boolean setInitParameter(String arg0, String arg1) {
        return null
    }
    public void setSessionTrackingModes(Set arg0) throws IllegalStateException ,IllegalArgumentException {
        return null
    }
}
