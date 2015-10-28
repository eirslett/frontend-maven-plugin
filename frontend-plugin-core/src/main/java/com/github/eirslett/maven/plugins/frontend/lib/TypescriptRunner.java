package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public interface TypescriptRunner {
    void execute(File argsTxt, File srcdir, File rootDir, File outputdir) throws TaskRunnerException;
}

final class DefaultTypescriptRunner extends NodeTaskExecutor implements TypescriptRunner {
	
	private static interface FileVisitor {
		void visit(File file) throws TaskRunnerException;
	}
	
    static final String TASK_LOCATION = "node_modules/typescript/bin/tsc";

    DefaultTypescriptRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
    
    @Override
    public void execute(File argsTxt, File srcDir, File rootDir, File outDir) throws TaskRunnerException {

    	LinkedList<String> args = new LinkedList<String>();

    	if (outDir == null) {
    		throw new TaskRunnerException("Missing parameter '<outDir>'");
    	}
    	args.add("--outDir");
    	args.add(outDir.getAbsolutePath());
    	
    	if (srcDir == null) {
    		throw new TaskRunnerException("Missing parameter '<srcDir>'");
    	}
    	
    	if (rootDir != null) {
    		
    		args.add("--rootDir");
    		args.add(rootDir.getAbsolutePath());
    		
    		compileEntireSrcDir(argsTxt, args, srcDir);
	    	
    	} else {
    		
    		compileFileByFile(args, srcDir);
    		
    	}
    	
    }
    
    private void superExecute(LinkedList<String> args) throws TaskRunnerException {
    	
    	super.execute(args);
    	
    }
    
    private void compileFileByFile(final LinkedList<String> args, final File srcDir)
    		throws TaskRunnerException {
    	
    	visitFilesOfDirectory(srcDir, new FileVisitor() {
			@Override
			public void visit(File file) throws TaskRunnerException {
		    	final LinkedList<String> tscArgs = new LinkedList<String>(args);
		    	tscArgs.add(file.getAbsolutePath());
				superExecute(tscArgs);
			}
		});
    	
    }
    
    private void compileEntireSrcDir(final File argsTxt, LinkedList<String> args, final File srcDir)
    		throws TaskRunnerException {
    	
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
    					argsTxtWriter.append(' ');
    				}
    				argsTxtWriter.append(arg);
    				if (!arg.startsWith("--")) {
    					argsTxtWriter.append("\"");
    				}
    			}
    			
    			final FileWriter finalWriter = argsTxtWriter;
    	    	visitFilesOfDirectory(srcDir, new FileVisitor() {
    				@Override
    				public void visit(File file) throws TaskRunnerException {
    					try {
    						finalWriter.append(' ');
    						finalWriter.append(file.getAbsolutePath());
    					} catch (IOException e) {
    						throw new TaskRunnerException("Could not append file-information", e);
    					}
    				}
    			});
    			
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
    	
		superExecute(tscArgs);
    	
    }
    
    private void visitFilesOfDirectory(File directory, FileVisitor visitor) throws TaskRunnerException {
    	
    	if (directory == null) {
    		return;
    	}
    	
    	for (File file : directory.listFiles()) {
    		
    		if (file.isDirectory()) {
    			visitFilesOfDirectory(file, visitor);
    		}
    		else if (file.isFile()) {
    			visitor.visit(file);
    		}
    		
    	}
    	
    }
    
}
