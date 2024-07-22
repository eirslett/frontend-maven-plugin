assert new File(basedir, 'target/bun').exists(): "Bun was not installed in the custom install directory";

import org.codehaus.plexus.util.FileUtils;

String buildLog = FileUtils.fileRead(new File(basedir, 'build.log'));

assert buildLog.contains('BUILD SUCCESS'): 'build was not successful'
