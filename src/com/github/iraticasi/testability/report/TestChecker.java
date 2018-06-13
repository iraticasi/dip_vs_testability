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
    private void report() throws IOException {
        String csvPath = "test_checker_report.csv";
        Writer writer = new FileWriter(csvPath);
        csvWriter = new CSVWriter(writer);
        String[] headerRecord = {"Project name", "#goodClasses", "#goodWithTest", "#badClasses", "#badWithTest"};
        csvWriter.writeNext(headerRecord);
        if (this.folder.exists()) {
            for (final File project : this.folder.listFiles()) {
                if (project.isDirectory()) {
                    //for each project
                    System.out.println(project);
                    csvWriter.writeNext(check(project));
                }
            }
        }
        csvWriter.close();

    }


    private void checkAll() throws IOException {
        String csvPath = "test_checker_report.csv";
        Writer writer = new FileWriter(csvPath);
        csvWriter = new CSVWriter(writer);
        String[] headerRecord = {"Project name", "#goodClasses", "#goodWithTest", "#badClasses", "#badWithTest"};
        csvWriter.writeNext(headerRecord);
        if (this.folder.exists()) {
            for (final File project : this.folder.listFiles()) {
                if (project.isDirectory()) {
                //for each project
                    System.out.println(project);
                    csvWriter.writeNext(check(project));
                }
            }
        }
        csvWriter.close();
    }
    public static String[] check(File project){
        List<ClassInfo> classInfos = new Analyzer(project).analyze();
        Set<String> tests = findAllJavaTest(project);
        int goodClasses=0, goodWithTest=0, badClasses=0, badWithTest=0;
        for (ClassInfo cr : classInfos){
            if (cr.isTestable()){
                goodClasses++;
                if (tests.contains(cr.getName() + "Test.java")) goodWithTest++;
            }else{
                badClasses++;
                if (tests.contains(cr.getName() + "Test.java")) badWithTest++;
            }
        }
        System.out.println("Good percentage: " + (float)goodWithTest/goodClasses);
        System.out.println("Bad percentage: " + (float)badWithTest/badClasses);

        return new String[]{
                project.getName(),
                String.valueOf(goodClasses),
                String.valueOf(goodWithTest),
                String.valueOf(badClasses),
                String.valueOf(badWithTest)};
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
        new TestChecker("apache_projects").checkAll();



    }
}
