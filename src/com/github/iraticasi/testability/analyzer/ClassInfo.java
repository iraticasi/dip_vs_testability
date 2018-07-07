package com.github.iraticasi.testability.analyzer;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents information of a java class about the violation of the dependency injection principle
 * <p>
 * NOTES:
 * In this class, "dependency" refers to a violation of the dependency injection principle, i.e. a object
 * We consider a dependency to be "external" if it is on:
 * - a class that is neither from the same package nor from java.util
 * - a class that has external dependencies itself (recursive)
 * */

public class ClassInfo extends ASTVisitor implements Comparable<ClassInfo> {

    private String name, pkg, project;
    private List<String> dependencies; //dependency injection principle violations
    private boolean externalDependencies = false; //whether some dependency is "external"

    /**
     *
     * @param name name of the java class
     * @param pkg name ot its package
     * @param project name of its project
     */
    public ClassInfo(String name, String pkg, String project){
        this.name = name;
        this.pkg = pkg;
        this.project = project;
        this.dependencies = new ArrayList<>();
    }

    /**
     * It visit all objects creation in search of dependencies
     * @param creation
     * @return
     */
    @Override
    public boolean visit(ClassInstanceCreation creation){
        //get dependency
        String dependencyName = creation.getType().toString();
        IMethodBinding constructorBinding = creation.resolveConstructorBinding();
        String dependencyPkg="<not resolve>";
        if (constructorBinding!=null)  dependencyPkg = constructorBinding.getDeclaringClass().getPackage().getName();
        if (dependencyPkg.equals("")) dependencyPkg = "<no package>";
        String dependencyFullName = dependencyPkg + "." + dependencyName;
        //check if is external
        if (isExternalDependency(dependencyFullName)) externalDependencies = true;
        //add it to list
        this.dependencies.add(dependencyFullName);
        return true;
    }

    /**
     * Check if a dependency is consider directly external or not
     * @param dependency
     * @return whether a dependency is consider directly external or not
     */
    private boolean isExternalDependency(String dependency) {
        return !dependency.contains("org.apache."+ project) && !dependency.startsWith("java.util");
    }

    /**
     * Set if the class has some external dependency
     * @param value
     */
    public void setExternalDependencies(boolean value){
        externalDependencies = value;
    }

    /**
     *
     * @return full name of the class (package+name)
     */
    public String getFullName(){
        return pkg + "." + name;
    }

    /**
     *
     * @return list of dependencies names
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     *
     * @return name of the java class
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return name of the java class
     */
    public String getPkg(){
        return pkg;
    }

    /**
     *
     * @return whether the class has "external" dependencies
     */
    public boolean hasExternalDependencies(){
        return externalDependencies;
    }

    /**
     * Check if the class has some dependency that matches the string s
     * @param s string to match
     * @return whether the class has some dependency that matches the string s
     */
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
