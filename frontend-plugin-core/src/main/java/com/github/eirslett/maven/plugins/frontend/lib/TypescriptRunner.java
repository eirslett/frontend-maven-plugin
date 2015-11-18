package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public interface TypescriptRunner {
	
	enum Module { commonjs, amd, system, umd };
	
	enum Target { ES3, ES5, ES6 };

    void execute(File argsTxt, File srcdir, boolean preserveDirectoryStructure,
    		File outputdir, boolean removeComments, Module module,
    		Target target, String charset, boolean sourceMap,
    		boolean declaration, File mapRoot, boolean noImplicitAny,
    		boolean noResolve, boolean noEmitOnError) throws TaskRunnerException;
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
    public void execute(File argsTxt, File srcDir, boolean preserveDirectoryStructure,
    		File outDir, boolean removeComments, Module module, Target target,
    		String charset, boolean sourceMap, boolean declaration,
    		File mapRoot, boolean noImplicitAny, boolean noResolve,
    		boolean noEmitOnError) throws TaskRunnerException {

    	LinkedList<String> args = new LinkedList<String>();

    	if (outDir == null) {
    		throw new TaskRunnerException("Missing parameter '<outDir>'");
    	}
    	args.add("--outDir");
    	args.add(outDir.getAbsolutePath());
    	
    	if (srcDir == null) {
    		throw new TaskRunnerException("Missing parameter '<srcDir>'");
    	}
    	
    	if (mapRoot != null) {
    		args.add("--mapRoot");
    		args.add(mapRoot.getAbsolutePath());
    	}
    	
    	if (noImplicitAny) {
    		args.add("--noImplicitAny");
    	}

    	if (noResolve) {
    		args.add("--noResolve");
    	}

    	if (noEmitOnError) {
    		args.add("--noEmitOnError");
    	}
    	
    	if (declaration) {
    		args.add("--declaration");
    	}
    	
    	if (sourceMap) {
    		args.add("--sourceMap");
    	}

    	if (charset != null) {
    		args.add("--charset");
    		args.add(charset);
    	}
    	
    	if (removeComments) {
    		args.add("--removeComments");
    	}
    	
    	if (module != null) {
    		args.add("--module");
    		args.add(module.name());
    	}
    	
    	if (target != null) {
    		args.add("--target");
    		args.add(target.name());
    	}
    	
    	if (preserveDirectoryStructure) {
    		
    		args.add("--rootDir");
    		args.add(srcDir.getAbsolutePath());
    		
    		compileEntireSrcDir(argsTxt, args, srcDir, noEmitOnError);
	    	
    	} else {
    		
    		compileFileByFile(args, srcDir, noEmitOnError);
    		
    	}
    	
    }
    
    private void superExecute(LinkedList<String> args, boolean noEmitOnError) throws TaskRunnerException {
    	
    	try {
    		super.execute(args);
    	} catch (TaskRunnerException e) {
    		// treat tsc as successful if noEmitOnError-flag was false or absent
    		if (e.getCause() != null) {
    			throw e;
    		}
    		// see https://github.com/Microsoft/TypeScript/blob/f8489afe7ff16a6936ba661893968d22da9bc3eb/lib/typescriptServices.d.ts#L1038
   			if (e.getMessage().contains("error code 1")) {
    			throw e;
    		}
    		if (noEmitOnError) {
    			throw e;
    		}
    		// suppress exception if tsc was instructed to build output even in case of errors
    	}
    	
    }
    
    private void compileFileByFile(final LinkedList<String> args, final File srcDir, final boolean noEmitOnError)
    		throws TaskRunnerException {
    	
    	visitFilesOfDirectory(srcDir, new FileVisitor() {
			@Override
			public void visit(File file) throws TaskRunnerException {
		    	final LinkedList<String> tscArgs = new LinkedList<String>(args);
		    	tscArgs.add(file.getAbsolutePath());
				superExecute(tscArgs, noEmitOnError);
			}
		});
    	
    }
    
    private void compileEntireSrcDir(final File argsTxt, LinkedList<String> args, final File srcDir, boolean noEmitOnError)
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
    					if (file.getName().endsWith(".d.ts")) {
    						return;
    					}
    					if (!file.getName().endsWith(".ts")) {
    						return;
    					}
    					try {
    						finalWriter.append(" \"");
    						finalWriter.append(file.getAbsolutePath());
    						finalWriter.append('"');
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
    	
		superExecute(tscArgs, noEmitOnError);
    	
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
