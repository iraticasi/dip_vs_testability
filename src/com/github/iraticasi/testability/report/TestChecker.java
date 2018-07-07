package com.github.iraticasi.testability.report;

import com.github.iraticasi.testability.analyzer.Analyzer;
import com.github.iraticasi.testability.analyzer.ClassInfo;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Class to report statistics of the existence of dedicated tests of classes
 * <p>
 * NOTES:
 * In this class, "dependency" refers to a violation of the dependency injection principle, i.e. a object
 * We consider a dependency to be "external" if it is on:
 * - a class that is neither from the same package nor from java.util
 * - a class that has external dependencies itself (recursive)
 * Therefore, we consider A class to be "external" if it has "extena dependencies"
 * A class has a dedicated test if there exits a class <classname>Test.java or <classname>Tests.java
 * */

public class TestChecker {

    private File folder; //folder with all projects to check

    /**
     * Create a test checker for a given folder
     * @param folderName folder with projects to check
     */
    public TestChecker(String folderName) {
        this.folder = new File(folderName);

    }


    /**
     * Creates a CSV report for external dependencies.
     * <p>
     * For each project, statistics of the relation between classes with external dependencies and classes with dedicated test
     * @param file the name of the file to write the CSV report
     * @throws IOException
     */
    private void externalReport(String file) throws IOException {
        //create CSV writer
        Writer writer = new FileWriter(file);
        CSVWriter csvWriter = new CSVWriter(writer);
        //write header
        String[] headerRecord = {"Project name", "# external classes with tests", "# of external classes without test", "# internal classes with tests", "# of internal classes without test",};
        csvWriter.writeNext(headerRecord);
        //write record for each project
        if (this.folder.exists()) {
            for (final File project : this.folder.listFiles()) {
                if (project.isDirectory()) {
                //for each project
                    System.out.println(project);
                    csvWriter.writeNext(checkExternal(project));
                }
            }
        }
        //close
        csvWriter.close();
    }

    /**
     * Compute statistics of the relation between classes with external dependencies and classes with dedicated test
     * @param project base folder of the projects
     * @return String array with:
     *      {<project_name>,
     *      <# external classes with test>,
     *      <# external classes withOUT test>,
     *      <# internal classes with test>,
     *      <# internal classes withOUT test>}
     */
    public static String[] checkExternal(File project){
        //analyze
        List<ClassInfo> classInfos = new Analyzer(project).analyze();
        Set<String> tests = findAllJavaTest(project);
        //compute statistics
        int extNoTest=0, extWithTest=0, intNoTest=0, intWithTest=0;
        for (ClassInfo cr : classInfos){
            if (cr.hasExternalDependencies()){
                if (tests.contains(cr.getName() + "Test.java")) extWithTest++;
                else extNoTest++;
            }else{
                if (tests.contains(cr.getName() + "Test.java")) intWithTest++;
                else intNoTest++;

            }
        }
        //return string array
        return new String[]{
                project.getName(),
                String.valueOf(extWithTest),
                String.valueOf(extNoTest),
                String.valueOf(intWithTest),
                String.valueOf(intNoTest)};
    }


    /**
     * Creates a CSV report for diferents libraries:
     * For each library, statistics of the relation between classes with direct dependencies of that library and classes with dedicated test
     * @param file  the name of the file to write the CSV report
     * @param libraries string array with the libraries names
     * @throws IOException
     */
    public void librariesReport(String file, String[] libraries) throws IOException {
        //create CSV
        Writer writer = new FileWriter(file);
        CSVWriter csvWriter = new CSVWriter(writer);
        //write header
        String[] headerRecord = {"Library name",
                "library_dep WITH test",
                "library_dep NO test",
                "NO library_dep WITH test",
                "NO Library_dep NO test"};
        csvWriter.writeNext(headerRecord);
        //create libraryInfos
        List<LibraryInfo> libraryInfos = new ArrayList<>();
        for(String library: libraries){
            libraryInfos.add(new LibraryInfo(library));
        }
        //colect statistics
        if (this.folder.exists()) {
            for (final File project : this.folder.listFiles()) {
                if (project.isDirectory()) {
                    //for each project
                    System.out.println(project);
                    List<ClassInfo> classInfos = new Analyzer(project).analyze();
                    Set<String> tests = findAllJavaTest(project);

                    for (ClassInfo cr : classInfos){
                        boolean hasTest = tests.contains(cr.getName() + "Test.java");
                        for (LibraryInfo libraryInfo:libraryInfos){
                            libraryInfo.countClass(cr,hasTest);
                        }
                    }
                }
            }
        }
        //write record for each library
        for(LibraryInfo libraryInfo:libraryInfos){
            csvWriter.writeNext(libraryInfo.getRecord());
        }
        //close
        csvWriter.close();

    }

    /**
     * Internal class that represents statistics of a given library
     */
    private class LibraryInfo{

        private int libraryWithTest=0, libraryNoTest=0, noLibraryWithTest=0, noLibraryNoTest=0; //statistics
        private String libraryName; //library name

        public LibraryInfo(String libraryName){
            this.libraryName = libraryName;
        }

        /**
         * Update the statistics with a given class
         * @param clazz class to check
         * @param hasTest if clazz has a dedicated test
         */
        public void countClass(ClassInfo clazz, boolean hasTest){
            if (clazz.hasDependency(libraryName)){
                if (hasTest) libraryWithTest++;
                else libraryNoTest++;
            }else{
                if (hasTest) noLibraryWithTest++;
                else noLibraryNoTest++;
            }
        }

        /**
         *
         * @return the statistics of the library
         *      {<name of the library>,
         *      <# of classes with dependency on that library with dedicated test>,
         *      <# of classes with dependency on that library withOUT dedicated test>,
         *      <# of classes withOUT dependency on that library with dedicated test>,
         *      <# of classes withOUT dependency on that library withOUT dedicated test>,
         */
        public String[] getRecord(){
            return new String[]{
                    libraryName,
                    String.valueOf(libraryWithTest),
                    String.valueOf(libraryNoTest),
                    String.valueOf(noLibraryWithTest),
                    String.valueOf(noLibraryNoTest)};
        }
    }

    /**
     * Find all java files names that ends with "Test" or "Tests"
     * @param folder folder to search
     * @return list of java files names
     */
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
        new TestChecker("apache_projects").externalReport("external_report.csv");
        //new TestChecker("apache_projects").librariesReport("libraries_report.csv", new String[]{"java.io", "java.sql.", "java.net.", "javax"});


    }
}
