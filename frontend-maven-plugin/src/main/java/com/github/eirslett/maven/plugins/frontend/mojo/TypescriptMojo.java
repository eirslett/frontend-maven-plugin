package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.github.eirslett.maven.plugins.frontend.lib.TypescriptRunner;

@Mojo(name="tsc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class TypescriptMojo extends AbstractFrontendMojo {

	/**
	 * The name of the file in which the args for the typescript-compiler should be
	 * stored. See "tsc --help" -> "tsc @args.txt"
	 */
	@Parameter(property = "args.txt", defaultValue = "target/tsc-args.txt")
	private File argsTxt;
	
    /**
     * The directory containing typescript files that will be processed by tsc.
     */
    @Parameter(property = "srcDir")
    private File srcDir;

    /**
     * Whether the directory structure within the configure srcDir
     * should be preserved in the outDir. Otherwise all ts-files
     * will be compiled direct into the outDir. If set to true the
     * typescript parameter --rootDir will be set to the directory
     * of srcDir.
     */
    @Parameter(property = "preserveDirectoryStructure", defaultValue = "false")
    private boolean preserveDirectoryStructure;
    
    /**
     * Remove all comments except copy-right header comments beginning with /!*
     */
    @Parameter(property = "removeComments", defaultValue = "false")
    private boolean removeComments;
    
    /**
     * Specify module code generation: 'commonjs', 'amd', 'system', or 'umd'.
     */
    @Parameter(property = "module", defaultValue = "system")
    private TypescriptRunner.Module module;
    
    /**
     * Specify ECMAScript target version: 'ES3' (default), 'ES5', or 'ES6
     */
    @Parameter(property = "target", defaultValue = "ES3")
    private TypescriptRunner.Target target;
    
    /**
     * The directory where compiled typescript files will be placed by tsc.
     */
    @Parameter(property = "outDir")
    private File outDir;

    /**
     * The character set of the input files. Defaults to ${project.build.sourceEncoding}.
     */
    @Parameter(property = "charset", defaultValue = "${project.build.sourceEncoding}")
    private String charset;

    /**
     * Generates corresponding '.map' file. default: true.
     */
    @Parameter(property = "sourceMap", defaultValue = "true")
    private boolean sourceMap;
    
    /**
     * Generates corresponding '.d.ts' file. default: false.
     */
    @Parameter(property = "declaration", defaultValue = "false")
    private boolean declaration;
    
    /**
     * Specifies the location where debugger should locate map files instead of generated locations. Use this flag if the .map files will be located at run-time in a different location than than the .js files. The location specified will be embedded in the sourceMap to direct the debugger where the map files where be located.
     */
    @Parameter(property = "mapRoot")
    private File mapRoot;
    
    /**
     * Raise error on expressions and declarations with an implied 'any' type.
     */
    @Parameter(property = "noImplicitAny", defaultValue = "false")
    private boolean noImplicitAny;
    
    /**
     * Do not add triple-slash references or module import targets to the list of compiled files.
     */
    @Parameter(property = "noResolve", defaultValue = "false")
    private boolean noResolve;
    
    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.tsc", defaultValue = "false")
    private Boolean skip;

    @Component
    private BuildContext buildContext;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {

        factory.getTypescriptRunner().execute(
        		argsTxt, srcDir, preserveDirectoryStructure, outDir,
        		removeComments, module, target, charset, sourceMap,
        		declaration, mapRoot, noImplicitAny, noResolve);

        if (outDir != null) {
            getLog().info("Refreshing files after tsc: " + outDir);
            buildContext.refresh(outDir);
        }
        
    }

}
