import java.nio.file.Paths

def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('BUILD SUCCESS') : 'build was not successful'
assertEmpty()
assertWithToken()
assertWithoutToken()

def assertEmpty() {
    def npmrcFile = Paths.get(basedir.getAbsolutePath(), 'user.home', 'empty', '.npmrc').toFile()
    assert npmrcFile.exists() : "${npmrcFile.getAbsolutePath()} does`exist";
    assert npmrcFile.text.equals('//localhost:${npm.registry.port}/:_authToken="NpmToken.22e3a730-9e62-11e8-98d0-529269fb1459"\n') : "incorrect content"
}

def assertWithToken() {
    def buildLog = new File(basedir, 'build.log').text
    assert buildLog.contains('Token exists. Skipping execution.') : 'build was not successful'
    def npmrcFile = Paths.get(basedir.getAbsolutePath(), 'user.home', 'with_token', '.npmrc').toFile()
    assert npmrcFile.exists() : "${npmrcFile.getAbsolutePath()} does`exist";
    assert npmrcFile.text.equals('//localhost:${npm.registry.port}/:_authToken="NpmToken.22e3a730-9e62-11e8-98d0-529269fb1459"\nregistry=https://localhost:${npm.registry.port}\n') : "incorrect content"
}

def assertWithoutToken() {
    def npmrcFile = Paths.get(basedir.getAbsolutePath(), 'user.home', 'without_token', '.npmrc').toFile()
    assert npmrcFile.exists() : "${npmrcFile.getAbsolutePath()} does`exist";
    assert npmrcFile.text.equals('//localhost:${npm.registry.port}/:_authToken="NpmToken.22e3a730-9e62-11e8-98d0-529269fb1459"\n//localhost/:_authToken="NpmToken.22e3a730-9e62-11e8-98d0-529269fb1459"\n' +
            'registry=https://localhost\n') : "incorrect content"
}