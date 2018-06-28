package com.github.iraticasi.testability.analyzer;

import com.opencsv.CSVWriter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;


/**
 * Dependency injection violations analyzer for a project
 *
 * NOTES:
 * In this class, "dependency" refers to a violation of the dependency injection principle, i.e. a object
 * We consider a dependency to be "external" if it is on:
 * - a class that is neither from the same package nor from java.util
 * - a class that has external dependencies itself (recursive)
 * */

public class Analyzer {

    private File project; //Base folder of the project
    private List<String> sourceFilePaths; //Source files paths
    private List<String> sourceDirPaths; //Directories paths that contain source file
    private List<ClassInfo> classes; //project classes

    /**
     * Creates a analyzer for a project
     * @param project base folder of the project
     */
    public Analyzer(File project) {
        this.project = project;

    }

    /**
     * Analyze all java source files in the project
     * @return list of all java source files info
     */
    public List<ClassInfo> analyze() {
        sourceFilePaths = new ArrayList<>();
        sourceDirPaths = new ArrayList<>();
        findAllClasses(project,sourceFilePaths, sourceDirPaths);
        this.parseClasses();;
        this.spreadExternalDependencies();
        return classes;
    }

    /**
     * Parse all the java source files in the project
     */
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
            public void acceptAST(String sourceFilePath, CompilationUnit cu) { //for each class
                if (cu.types().size()>0) {
                    TypeDeclaration typeDeclaration=null;
                    for (AbstractTypeDeclaration declaration: (List<AbstractTypeDeclaration>) cu.types()){
                        if (declaration instanceof TypeDeclaration) typeDeclaration = (TypeDeclaration) declaration;
                    }
                    if (typeDeclaration!=null){ //if there is any class-interface declaration
                        if (!typeDeclaration.isInterface()) { //if it's not an interface
                            //create and add the java info
                            String name = ((AbstractTypeDeclaration) cu.types().get(0)).getName().toString();
                            String pkg = cu.getPackage() == null ? "<no package>" : cu.getPackage().getName().toString();
                            ClassInfo classInfo = new ClassInfo(name, pkg, project.toString());
                            cu.accept(classInfo);
                            classes.add(classInfo);
                        }
                    }
                }
            }
        };
        parser.createASTs(sourceFilePaths.toArray(new String[sourceFilePaths.size()]), null, new String[]{}, requestor , null );
    }

    /**
     * Spread all "external" dependencies of the project classes
     * (i.e. if a class A has a dependency of a class B with "external" dependencies,
     * then class B is consider "external" so class A has "external" dependencies too )
     */
    private void spreadExternalDependencies() {
        //split classes
        List<ClassInfo> withExternal = new ArrayList<>();
        List<ClassInfo> noExternal = new ArrayList<>();
        for (ClassInfo classInfo: classes){
            if (classInfo.hasExternalDependencies()){
                noExternal.add(classInfo);
            }else{
                withExternal.add(classInfo);
            }
        }
        //spread external dependencies
        int i = 0;
        while (i<withExternal.size()){
            ClassInfo badClassInfo = withExternal.get(i);
            boolean dependencyMatch = false;
            int j=0;
            while (j<noExternal.size() & !dependencyMatch){
                ClassInfo goodClassInfo = noExternal.get(j);
                if (goodClassInfo.getDependencies().contains(badClassInfo.getFullName())){
                    dependencyMatch = true;
                    goodClassInfo.setExternalDependencies(true);
                    noExternal.remove(j);
                    withExternal.add(goodClassInfo);
                }
                j++;
            }
            i++;
        }
    }

    /**
     * Writes list with all project classes and whether they have "external dependencies" or not
     * @param file file to write report to
     * @throws IOException
     */
    public void makeReport(String file) throws IOException {
        String csvPath = project.getAbsolutePath() + File.separator + file;
        Writer writer = new FileWriter(csvPath);
        CSVWriter csvWriter = new CSVWriter(writer);
        String[] headerRecord = {"Package", "Class", "External dependencies"};
        csvWriter.writeNext(headerRecord);
        Collections.sort(classes); //by package, then by name
        for (ClassInfo classInfo: classes){
            String[] record = {classInfo.getPkg(),
                    classInfo.getName(),
                    classInfo.hasExternalDependencies() ? "Y" : "N"};
            csvWriter.writeNext(record);
        }
        csvWriter.close();

    }

    /**
     * Find all java source files in a directory recursevely and their directories paths
     * @param folder folder to search
     * @param sourceFilePaths list to store the file paths of the java files
     * @param sourceDirPaths list to store the directories paths of the java files
     * @return number of java files found in that directory (not recursive)
     */
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
                            !fileEntry.getName().equals("module-info.java") &&
                            !fileEntry.getName().equals("package-info.java")) {
                        sourceFilePaths.add(fileEntry.getAbsolutePath());
                        numFiles++;

                    }
                }
            }
        }
        return numFiles;
    }

    /**
     * If a java file is a dedicated test (i.es if it finishes with "Test" or "Tests")
     * @param className the java class name
     * @return
     */
    public static boolean isTest(String className) {
        return className.endsWith("Test.java") || className.endsWith("Tests.java");
    }


    public static void main(final String[] args) throws IOException {
        Analyzer analyzer = new Analyzer(new File("apache_projects/zeppelin"));
        analyzer.analyze();
        analyzer.makeReport("testability_report.csv");
    }

}
