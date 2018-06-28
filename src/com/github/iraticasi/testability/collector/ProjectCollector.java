package com.github.iraticasi.testability.collector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * Collects apache
 */
public class ProjectCollector {

    public static void main(String[] args) throws IOException, InterruptedException {

        int numprojects = 20;

        Runtime rt = Runtime.getRuntime();
        String[] commands = {
                "/bin/sh",
                "-c",
                "curl 'https://api.github.com/search/repositories?q=+org:apache+language:Java&per_page=" + numprojects + "' > apache_projects.json"
        };
        Process p = rt.exec(commands);
        p.waitFor();


        JSONArray jsonArray= new JSONObject(readFile("apache_projects.json")).getJSONArray("items");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String cloneURL = jsonObject.getString("clone_url");
            String name = jsonObject.getString("name");
            String[] cloneCommand = {
                    "git",
                    "clone",
                    cloneURL,
                    "apache_projects/" + name
            };
            System.out.println(cloneURL);
            Process process = rt.exec(cloneCommand);
            process.waitFor();
        }
    }


    static String readFile(String path) throws IOException {

        File file = new File(path);

        byte[] encoded = Files.readAllBytes(Paths.get(file.getCanonicalPath()));
        String string =  new String(encoded, Charset.defaultCharset());
        return string;
    }
}
