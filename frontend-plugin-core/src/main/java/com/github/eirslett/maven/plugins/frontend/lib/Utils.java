package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Utils {
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
    public static List<String> merge(List<String> first, List<String> second) {
        ArrayList<String> result = new ArrayList<String>(first);
        result.addAll(second);
        return result;
    }

    public static List<String> prepend(String first, List<String> list){
        return merge(Arrays.asList(first), list);
    }

    public static String normalize(String path){
        return path.replace("/", File.separator);
    }

    public static String implode(String separator, List<String> elements){
        StringBuffer s = new StringBuffer();
        for(int i = 0; i < elements.size(); i++){
            if(i > 0){
                s.append(" ");
            }
            s.append(elements.get(i));
        }
        return s.toString();
    }

    public static boolean isRelative(String path) {
        return !path.startsWith("/") && !path.startsWith("file:") && !path.matches("^[a-zA-Z]:\\\\.*");
    }
    
    /**
     * This method is a convenience for copying all contents from 1 directory to another while preserving file permissions
     * @param sourceDirectory - The directory to copy all contents from
     * @param destDirectory - The new directory to copy all contents to
     */
    public static void copyDirectoryContents(File sourceDirectory, File destDirectory) {
    		if(sourceDirectory.isDirectory()) {
	    		//Get all files and directory names from the current source directory
			for(File child : sourceDirectory.listFiles()) {
				//If the File is a Directory then we need to create it and any parent directories that may not exist
				//At the same time we need to go ahead and recursively call our current method to go into all child directories to copy
				//all of their contents as well
				if(child.isDirectory()) {
					File newDirectory = new File(destDirectory.getAbsolutePath() + File.separator + child.getName());
					logger.debug("Creating directory :" + newDirectory.getAbsolutePath());
					newDirectory.mkdirs();
					copyDirectoryContents(child, newDirectory);
				}
				else {
					//If this is a plain file then we need to copy it and preserve permissions
					File dest = new File(destDirectory.getPath() + File.separator + child.getName());
					try {
						logger.debug("Creating file :" + dest.getAbsolutePath());
						Files.copy(Paths.get(child.getAbsolutePath()), Paths.get(dest.getAbsolutePath()), StandardCopyOption.COPY_ATTRIBUTES);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
	    }
    }

}
