package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

/**
 * AST Transformation adding the JobExecution annotation to the Job's execute method.
 * 
 * This transformation finds all classes implementing the Job interface and annotates
 * the execute method with the {@link JobExecution} annotation, so that implementers of
 * this interface do not have to care about it.
 *
 * Additionally the transformation generates the version method by looking at the git
 * history of the source file for the Job implementation. The latest git commit becomes
 * the unique version number of the Job.
 *
 * If the Job Implementation class inherits the AbstractJobImpl class the transformation
 * also adds the required constructors.
 *
 * The annotation JobExecution is required by the Aspect intercepting the JobExecution.
 *
 * @see JobExecution
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class JobTransformation implements ASTTransformation {
    Git git = null

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        List classNodes = sourceUnit.getAST()?.getClasses()
        
        classNodes.each { ClassNode classNode ->
            if (classNode.isInterface()) {
                // skip interfaces
                return
            }
            if ((classNode.modifiers & Opcodes.ACC_ABSTRACT) > 0) {
                // skip abstract classes
                return
            }
            // test whether the class implements the Job interface
            boolean isJob = false
            boolean inheritsAbstractJob = false
            classNode.getAllInterfaces().each {
                if (it.name == "de.dkfz.tbi.otp.job.processing.Job") {
                    isJob = true
                }
            }
            if (classNode.superClass.name == "de.dkfz.tbi.otp.job.processing.AbstractJobImpl") {
                isJob = true
                inheritsAbstractJob = true
            }
            if (!isJob) {
                return
            }
            if (inheritsAbstractJob) {
                // add generic Constructor
                classNode.addConstructor(new ConstructorNode(Opcodes.ACC_PUBLIC, new BlockStatement()))
                // add constructor with two arguments calling the super class constructor
                Parameter[] params = [
                    new Parameter(classNode.superClass.getField("processingStep").type, "a"),
                    new Parameter(new ClassNode(List), "b") ] as Parameter[]
                BlockStatement stmt = new BlockStatement()
                stmt.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.SUPER, new ArgumentListExpression(new VariableExpression("a"), new VariableExpression("b")))))
                classNode.addConstructor(Opcodes.ACC_PUBLIC, params, ClassNode.EMPTY_ARRAY, stmt)
                // add getVersion method - actual code will be generated below
                classNode.addMethod("getVersion", Opcodes.ACC_PUBLIC, new ClassNode(String), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, new ReturnStatement(new ConstantExpression("")))
            }

            // add the Annotation to the execute method and generates the version
            classNode.getMethods().each { method ->
                if (method.getName() == "execute") {
                    method.addAnnotation(new AnnotationNode(new ClassNode(this.class.classLoader.loadClass("de.dkfz.tbi.otp.job.scheduler.JobExecution"))))
                }
                if (method.getName() == "getVersion") {
                    if (!git) {
                        setupGit()
                    }
                    String version = "Unresolved Version"
                    // find the latest git commit on the file
                    Iterator<RevCommit> log = git.log().addPath(classNameToPath(classNode.name)).call().iterator()
                    if (log.hasNext()) {
                        RevCommit commit = log.next()
                        version = commit.getName()
                    }
                    // and add a return statement for the method
                    method.setCode(new ReturnStatement(new ConstantExpression(version)))
                }
            }
        }
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
