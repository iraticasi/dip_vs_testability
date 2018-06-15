package com.github.iraticasi.testability.analyzer;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo extends ASTVisitor implements Comparable<ClassInfo> {

    private String name, pkg, project;
    private List<String> dependencies;
    private boolean externalDependencies = false;

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

        if (isExternalDependency(dependencyFullName)) externalDependencies = true;

        this.dependencies.add(dependencyFullName);
        return true;
    }

    private boolean isExternalDependency(String dependency) {
        return !dependency.contains("org.apache."+ project) && !dependency.startsWith("java.util");
    }

    public void setExternalDependencies(boolean value){
        externalDependencies = value;
    }

    public String getFullName(){
        return pkg + "." + name;
    }


    public List<String> getDependencies() {
        return dependencies;
    }

    public String getName() {
        return name;
    }

    public String getPkg(){
        return pkg;
    }

    public boolean hasExternalDependencies(){
        return externalDependencies;
    }

    public boolean hasDependency(String s){
        for(String dependency:dependencies){
            if (dependency.contains(s)) return true;
        }
        return false;
    }

    @Override
    public String toString(){
        return "{ " + this.getFullName()
                + ", external dependencies: " + (this.externalDependencies ? "Y": "N")
                + ", dependencies: " + dependencies.toString() + "}\n";
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
