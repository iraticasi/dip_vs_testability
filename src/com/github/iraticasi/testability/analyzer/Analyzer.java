
package com.github.iraticasi.testability.analyzer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Analyzer {

    File project;
    List<String> sourceFilePaths;
    List<String> sourceDirPaths;
    List<ClassInfo> classes;

    public Analyzer(File project) {
        this.project = project;

    }

    public void analyze() {
        sourceFilePaths = new ArrayList<>();
        sourceDirPaths = new ArrayList<>();
        findAllClasses(project,sourceFilePaths, sourceDirPaths);
        System.out.println(sourceDirPaths);
        parseClasses();;
        //List<CompilationUnit> badClasses = getBadClasses(classes);
        //Map<CompilationUnit, List<String>> dependencies = getDependencies(classes);
    }


    private static int findAllClasses(File folder, List<String> sourceFilePaths, List<String> sourceDirPaths) {
        int numFiles=0;
        if (folder.exists()) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    if (findAllClasses(fileEntry, sourceFilePaths, sourceDirPaths)>0) {
                        sourceDirPaths.add(fileEntry.getAbsolutePath());
                    }
                } else {
                    if (fileEntry.getName().endsWith(".java") &&
                            !isTest(fileEntry.getName()) &&
                            !fileEntry.getName().equals("module-info.java")) {
                        sourceFilePaths.add(fileEntry.getAbsolutePath());
                        numFiles++;

                    }
                }
            }
        }
        return numFiles;
    }


    private void parseClasses() {
        //set parser
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        Map options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        parser.setEnvironment(null, sourceDirPaths.toArray(new String[sourceDirPaths.size()]), null, true);
        //parse files into ClassInfo
        this.classes = new ArrayList<>();
        FileASTRequestor requestor = new FileASTRequestor() {
            public void acceptAST(String sourceFilePath, CompilationUnit cu) {
                String name = ((AbstractTypeDeclaration) cu.types().get(0)).getName().toString();
                String pkg = cu.getPackage()==null? "<no package>": cu.getPackage().getName().toString();
                ClassInfo classInfo = new ClassInfo(name, pkg);
                cu.accept(classInfo);
                classes.add(classInfo);
            }
        };
        parser.createASTs(sourceFilePaths.toArray(new String[sourceFilePaths.size()]), null, new String[]{}, requestor , null );
    }



    public static boolean isTest(String className) {
        return className.endsWith("Test.java") | className.endsWith("Tests.java");
    }

    private boolean isBad(String importName) {
        return importName.contains("java.io");
    }

    private Map<CompilationUnit, List<String>> getDependencies(List<CompilationUnit> classes) {
        Map<CompilationUnit, List<String>> dependencies = new HashMap<>();
        for (final CompilationUnit cu : classes) {
            List<String> collector = new ArrayList<>();
            //cu.accept(new DependenciesVisitor(collector));
            dependencies.put(cu, collector);
        }

        return dependencies;
    }


    public static void main(final String[] args) throws IOException {
        new Analyzer(new File("examples/code-coverage-jacoco")).analyze();
        new Analyzer(new File("examples/myExample")).analyze();
    }


}
