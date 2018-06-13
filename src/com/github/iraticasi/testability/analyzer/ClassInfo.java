package com.github.iraticasi.testability.analyzer;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo extends ASTVisitor implements Comparable<ClassInfo> {

    private String name, pkg, project;
    private List<String> dependencies;
    private boolean hasExternalDependencies = false;
    private boolean indirectBadDependency = false;

    public ClassInfo(String name, String pkg, String project){
        this.name = name;
        this.pkg = pkg;
        this.project = project;
        this.dependencies = new ArrayList<>();
    }

    @Override
    public boolean visit(ClassInstanceCreation creation){

        String dependencyName = creation.getType().toString();
        IMethodBinding constructorBinding = creation.resolveConstructorBinding();
        String dependencyPkg="<not resolve>";
        if (constructorBinding!=null)  dependencyPkg = constructorBinding.getDeclaringClass().getPackage().getName();
        if (dependencyPkg.equals("")) dependencyPkg = "<no package>";
        String dependencyFullName = dependencyPkg + "." + dependencyName;

        if (isExternalDependency(dependencyFullName)) hasExternalDependencies = true;

        this.dependencies.add(dependencyFullName);
        return true;
    }

   /*@Override
    public boolean visit(ImportDeclaration importDeclaration){
        String importName = importDeclaration.getName().toString();
        System.out.println(importName);
        if (isExternalDependency(importName)) directBadDependency = true;
        return true;
    }
    */
    private boolean isExternalDependency(String dependency) {
        return !dependency.contains("org.apache."+ project) && !dependency.startsWith("java.util");
    }

    public String getFullName(){
        return pkg + "." + name;
    }

    public boolean isTestable() {
        return !directBadDependency && !indirectBadDependency;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setIndirectBadDependency(boolean indirectBadDependency) {
        this.indirectBadDependency = indirectBadDependency;
    }

    @Override
    public String toString(){
        return "{ " + this.getFullName() + ", testable: " + this.isTestable() + ", dependencies: " + dependencies.toString() + "}\n";
    }

    public String getName() {
        return name;
    }

    public String getPkg(){
        return pkg;
    }

    @Override
    public int compareTo(ClassInfo classInfo) {
        if (classInfo.getPkg().equals(pkg)){
            return classInfo.getName().compareTo(name);
        }else{
            return classInfo.getPkg().compareTo(pkg);
        }
    }
}
