package com.github.iraticasi.testability.report;

import com.github.iraticasi.testability.analyzer.Analyzer;
import com.github.iraticasi.testability.analyzer.ClassInfo;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class TestChecker {

    private File folder;
    private CSVWriter csvWriter;


    public TestChecker(String folderName) {
        this.folder = new File(folderName);

    }

    private class LibraryInfo{
        private int libraryWithTest=0, libraryNoTest=0, noLibraryWithTest=0, noLibraryNoTest=0;
        private String libraryName;

        public LibraryInfo(String libraryName){
            this.libraryName = libraryName;
        }

        public void countClass(ClassInfo clazz, boolean hasTest){
            if (clazz.hasDependency(libraryName)){
                if (hasTest) libraryWithTest++;
                else libraryNoTest++;
            }else{
                if (hasTest) noLibraryWithTest++;
                else noLibraryNoTest++;
            }
        }

        public String[] getRecord(){
            return new String[]{
                    libraryName,
                    String.valueOf(libraryWithTest),
                    String.valueOf(libraryNoTest),
                    String.valueOf(noLibraryWithTest),
                    String.valueOf(noLibraryNoTest)};
        }
    }


    private void report(String[] libraries) throws IOException {
        String csvPath = "libraries_report.csv";
        Writer writer = new FileWriter(csvPath);
        csvWriter = new CSVWriter(writer);
        String[] headerRecord = {"Library name",
                "library_dep WITH test",
                "library_dep NO test",
                "NO library_dep WITH test",
                "NO Library_dep NO test"};
        csvWriter.writeNext(headerRecord);

        List<LibraryInfo> libraryInfos = new ArrayList<>();
        for(String library: libraries){
            libraryInfos.add(new LibraryInfo(library));
        }

        if (this.folder.exists()) {
            for (final File project : this.folder.listFiles()) {
                if (project.isDirectory()) {
                    //for each project
                    System.out.println(project);
                    countProject(project, libraryInfos);
                }
            }
        }

        for(LibraryInfo libraryInfo:libraryInfos){
            csvWriter.writeNext(libraryInfo.getRecord());
        }


        csvWriter.close();

    }

    private static void countProject(File project, List<LibraryInfo> libraryInfos){
        List<ClassInfo> classInfos = new Analyzer(project).analyze();
        Set<String> tests = findAllJavaTest(project);

        for (ClassInfo cr : classInfos){
            boolean hasTest = tests.contains(cr.getName() + "Test.java");
            for (LibraryInfo libraryInfo:libraryInfos){
                libraryInfo.countClass(cr,hasTest);
            }
        }

    }



    private void checkAllExternal() throws IOException {
        String csvPath = "test_checker_report.csv";
        Writer writer = new FileWriter(csvPath);
        csvWriter = new CSVWriter(writer);
        String[] headerRecord = {"Project name", "# classes with external dependencies", "of which has test", "# classes without external dependencies", "of which has test"};
        csvWriter.writeNext(headerRecord);
        if (this.folder.exists()) {
            for (final File project : this.folder.listFiles()) {
                if (project.isDirectory()) {
                //for each project
                    System.out.println(project);
                    csvWriter.writeNext(checkExternal(project));
                }
            }
        }
        csvWriter.close();
    }
    private static String[] checkExternal(File project){
        List<ClassInfo> classInfos = new Analyzer(project).analyze();
        Set<String> tests = findAllJavaTest(project);
        int noExtClasses=0, noExtWithTest=0, extClasses=0, extWithTest=0;
        for (ClassInfo cr : classInfos){
            if (cr.hasExternalDependencies()){
                extClasses++;
                if (tests.contains(cr.getName() + "Test.java")) extWithTest++;
            }else{
                noExtClasses++;
                if (tests.contains(cr.getName() + "Test.java")) noExtWithTest++;

            }
        }

        return new String[]{
                project.getName(),
                String.valueOf(extClasses),
                String.valueOf(extWithTest),
                String.valueOf(noExtClasses),
                String.valueOf(noExtWithTest)};
    }

    private static Set<String> findAllJavaTest(File folder) {
        Set<String> tests = new HashSet<>();
        if (folder.exists()) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    tests.addAll(findAllJavaTest(fileEntry));
                } else {
                    if (Analyzer.isTest(fileEntry.getName())){
                        tests.add(fileEntry.getName());
                    }
                }
            }
        }
        return tests;
    }

    public static void main(String[] args) throws IOException {
        //new TestChecker("apache_projects").checkAllExternal();
        new TestChecker("apache_projects").report(new String[]{"java.io", "java.sql.", "java.net.", "javax"});


    }
}
