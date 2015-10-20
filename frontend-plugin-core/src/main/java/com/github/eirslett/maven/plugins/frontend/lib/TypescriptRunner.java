package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public interface TypescriptRunner {
    void execute(File argsTxt, File srcdir, File outputdir) throws TaskRunnerException;
}

final class DefaultTypescriptRunner extends NodeTaskExecutor implements TypescriptRunner {
	
    static final String TASK_LOCATION = "node_modules/typescript/bin/tsc";

    DefaultTypescriptRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
    
    @Override
    public void execute(File argsTxt, File srcdir, File outputdir) throws TaskRunnerException {

    	LinkedList<String> args = new LinkedList<String>();
    	
    	if (outputdir != null) {
	    	args.add("--outDir");
	    	args.add(outputdir.getAbsolutePath());
    	}
    	if (srcdir != null) {
    		args.add("--rootDir");
    		args.add(srcdir.getAbsolutePath());
	    	addFilesOfDirectoryToArgs(srcdir, args);
    	}
    	
    	final LinkedList<String> tscArgs = new LinkedList<String>();
    	if (argsTxt != null) {
    		FileWriter argsTxtWriter = null;
    		try {
    			final File destDir = argsTxt.getParentFile();
    			destDir.mkdirs();
    			argsTxtWriter = new FileWriter(argsTxt);
    			for (String arg : args) {
    				if (!arg.startsWith("--")) {
    					argsTxtWriter.append(" \"");
    				} else {
    					argsTxtWriter.append(" ");
    				}
    				argsTxtWriter.append(arg);
    				if (!arg.startsWith("--")) {
    					argsTxtWriter.append("\"");
    				}
    			}
    		} catch (IOException e) {
    			throw new TaskRunnerException("Could not build typescript arguments file '"
    					+ argsTxt + "'", e);
    		} finally {
    			if (argsTxtWriter != null) {
    				try {
    					argsTxtWriter.close();
    				} catch (Throwable e) {
    					e.printStackTrace();
    				}
    			}
    		}
    		tscArgs.add("@" + argsTxt.getAbsolutePath());
    	} else {
    		tscArgs.addAll(args);
    	}

		super.execute(tscArgs);

    }
    
    private void addFilesOfDirectoryToArgs(File directory, LinkedList<String> args) {
    	
    	if (directory == null) {
    		return;
    	}
    	
    	for (File file : directory.listFiles()) {
    		
    		if (file.isDirectory()) {
    			addFilesOfDirectoryToArgs(directory, args);
    		}
    		else if (file.isFile()) {
    			args.add(file.getAbsolutePath());
    		}
    		
    	}
    	
    }
    
}
