package com.github.iraticasi.testability.analyzer;

import com.opencsv.CSVWriter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class Analyzer {

    private File project;
    private List<String> sourceFilePaths;
    private List<String> sourceDirPaths;
    private List<ClassInfo> classes;

    public Analyzer(File project) {
        this.project = project;

    }

    public List<ClassInfo> analyze() {
        sourceFilePaths = new ArrayList<>();
        sourceDirPaths = new ArrayList<>();
        findAllClasses(project,sourceFilePaths, sourceDirPaths);
        this.parseClasses();;
        this.spreadBadDependencies();
        return classes;
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
                if (cu.types().size()>0) {
                    String name = ((AbstractTypeDeclaration) cu.types().get(0)).getName().toString();
                    String pkg = cu.getPackage() == null ? "<no package>" : cu.getPackage().getName().toString();
                    Class<?> clazz = ASTNode.nodeClassForType(cu.getNodeType());
                    ClassInfo classInfo = new ClassInfo(name, pkg, project.toString());
                    cu.accept(classInfo);
                    if (classInfo.getName().equals("ZeppelinIT")) System.out.println(classInfo);
                    classes.add(classInfo);
                }
            }
        };
        parser.createASTs(sourceFilePaths.toArray(new String[sourceFilePaths.size()]), null, new String[]{}, requestor , null );
    }

    private void spreadBadDependencies() {
        //split classes
        List<ClassInfo> bad = new ArrayList<>();
        List<ClassInfo> good = new ArrayList<>();
        for (ClassInfo classInfo: classes){
            if (classInfo.isTestable()){
                good.add(classInfo);
            }else{
                bad.add(classInfo);
            }
        }
        int i = 0;
        while (i<bad.size()){
            ClassInfo badClassInfo = bad.get(i);
            boolean dependencyMatch = false;
            int j=0;
            while (j<good.size() & !dependencyMatch){
                ClassInfo goodClassInfo = good.get(j);
                if (goodClassInfo.getDependencies().contains(badClassInfo.getFullName())){
                    dependencyMatch = true;
                    goodClassInfo.setIndirectBadDependency(true);
                    good.remove(j);
                    bad.add(goodClassInfo);
                }
                j++;
            }
            i++;
        }
    }
    public void makeReport() throws IOException {
        String csvPath = project.getAbsolutePath() + File.separator + "testability_report.csv";
        File csvFile = new File(csvPath);
        Writer writer = new FileWriter(csvPath);
        CSVWriter csvWriter = new CSVWriter(writer);
        String[] headerRecord = {"Package", "Class", "testable"};
        csvWriter.writeNext(headerRecord);
        Collections.sort(classes);
        int i=0;
        for (ClassInfo classInfo: classes){
            String[] record = {classInfo.getPkg(),
                    classInfo.getName(),
                    classInfo.isTestable() ? "Y" : "N"};
            if (classInfo.isTestable()) i++;
            csvWriter.writeNext(record);
        }
        System.out.println("#classes: " + classes.size());
        System.out.println("#testable " + i);
        csvWriter.close();

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

    public static boolean isTest(String className) {
        return className.endsWith("Test.java") || className.endsWith("Tests.java");
    }


    public static void main(final String[] args) throws IOException {
        Analyzer analyzer = new Analyzer(new File("apache_projects/zeppelin"));
        analyzer.analyze();
        analyzer.makeReport();
    }

}
