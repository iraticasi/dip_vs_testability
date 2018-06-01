package com.github.iraticasi.testability.analyzer;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo extends ASTVisitor {

    private String name, pkg;
    private List<String> dependencies;
    private boolean testable= true;

    public ClassInfo(String name, String pkg){
        this.name = name;
        this.pkg = pkg;
        this.dependencies = new ArrayList<>();
        System.out.println(getFullName());
    }

    @Override
    public boolean visit(ClassInstanceCreation creation){

        String dependencyName = creation.getType().toString();
        creation.resolveConstructorBinding();
        String dependencyPkg = creation.resolveConstructorBinding().getDeclaringClass().getPackage().getName();
        if (dependencyPkg.equals("")) dependencyPkg = "<no package>";
        String dependencyFullName = dependencyPkg + "." + dependencyName;
        System.out.println("\tdependency:" + dependencyFullName);
        this.dependencies.add(dependencyFullName);
        return true;
    }

    @Override
    public boolean visit(ImportDeclaration importDeclaration){
        if (isBadDependency(importDeclaration)) testable = false;
        return true;
    }

    private boolean isBadDependency(ImportDeclaration importDeclaration) {
        System.out.println("\timport: " + importDeclaration.getName());
        return importDeclaration.getName().toString().contains("java.io");
    }

    public String getFullName(){
        return pkg + "." + name;
    }

    public boolean isTestable() {
        return testable;
    }
}
