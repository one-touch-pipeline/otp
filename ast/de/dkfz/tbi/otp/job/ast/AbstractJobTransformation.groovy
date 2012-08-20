package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.Opcodes
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

/**
 * Abstract base class for Job Transformations.
 *
 * Provides some shared methods for the AST Transformation of
 * Job and StartJob.
 *
 */
abstract class AbstractJobTransformation {
    private Git git = null

    /**
     * Creates the code for the getVersion method based on the git
     * hash of the Class.
     * Only for methods with name {@code getVersion} code will be generated.
     * @param method The method for which it should be tested whether code has (and is) to be generated
     */
    protected void createGetVersion(MethodNode method) {
        if (method.getName() != "getVersion") {
            return
        }
        if (!git) {
            setupGit()
        }
        String version = "Unresolved Version"
        // find the latest git commit on the file
        Iterator<RevCommit> log = git.log().addPath(classNameToPath(method.declaringClass.name)).call().iterator()
        if (log.hasNext()) {
            RevCommit commit = log.next()
            version = commit.getName()
        }
        // and add a return statement for the method
        method.setCode(new ReturnStatement(new ConstantExpression(version)))
    }

    protected void addScopeAnnotation(ClassNode classNode) {
        ClassNode scopeAnnotationClass = new ClassNode(this.class.classLoader.loadClass("org.springframework.context.annotation.Scope"))
        if (classNode.getAnnotations(scopeAnnotationClass).isEmpty()) {
            AnnotationNode scopeAnnotation = new AnnotationNode(scopeAnnotationClass)
            scopeAnnotation.addMember("value", new ConstantExpression("prototype"))
            classNode.addAnnotation(scopeAnnotation)
        }
    }

    protected void addComponentAnnotation(ClassNode classNode) {
        ClassNode componentAnnotationClass = new ClassNode(this.class.classLoader.loadClass("org.springframework.stereotype.Component"))
        if (classNode.getAnnotations(componentAnnotationClass).isEmpty()) {
            AnnotationNode componentAnnotation = new AnnotationNode(componentAnnotationClass)
            String name = classNode.nameWithoutPackage.substring(0, 1).toLowerCase() + classNode.nameWithoutPackage.substring(1)
            componentAnnotation.addMember("value", new ConstantExpression(name))
            classNode.addAnnotation(componentAnnotation)
        }
    }

    /**
     * Adds the
     * private static final Log __internalLog = LogFactory.getLog(this)
     * to the classNode
     * @param classNode
     */
    protected void addLog(ClassNode classNode) {
        classNode.addField("__internalLog", Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, new ClassNode(Log),
            new StaticMethodCallExpression(new ClassNode(LogFactory), "getLog", new ArgumentListExpression(new ConstantExpression(classNode.name))))
        classNode.addField("log", Opcodes.ACC_PRIVATE, new ClassNode(Log), new EmptyExpression())
    }

    /**
     * Creates the git repository from the current working directory.
     * Assumes that the directory from where grails something is called is the root of a git repository.
     * This assumption should hold for whenever a grails project is git versioned.
     */
    private void setupGit() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(new File(System.getProperty("user.dir")))
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build()
        git = new Git(repository)
    }

    /**
     * Translates a fully qualified class name into the directory structure.
     * Assumes that all files are in src/groovy and end with .groovy.
     * @param className The Class Name as org.example.foo.Bar
     * @return Path to the source file, e.g. src/groovy/org/example/foo/Bar.groovy
     */
    private String classNameToPath(String className) {
        return "src/groovy/" + className.replace('.', '/') + ".groovy"
    }
}
