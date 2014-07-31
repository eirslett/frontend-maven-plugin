package com.github.eirslett.maven.plugins.frontend.lib;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.normalize;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author p.hoeffling
 */
public interface PackageJsonUpdater {

    public void update(ProjectInfo infos) throws TaskRunnerException;

}

final class DefaultPackageJsonUpdater implements PackageJsonUpdater {

    static final String FILE_NAME = "/package.json";

    private final Logger logger;

    private final File workingDirectory;

    public DefaultPackageJsonUpdater(File workingDirectory) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void update(ProjectInfo info) throws TaskRunnerException {

        final String absoluteFileName = workingDirectory + normalize(FILE_NAME);
        final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);

        logger.info("Updating {}", absoluteFileName);
        logger.debug("Project info: {}", info);

        try {
            File file = new File(absoluteFileName);
            File backup = new File(absoluteFileName + ".versionsBackup");

            logger.info("Creating backup file {}", backup);
            FileUtils.copyFile(file, backup);

            PackageJson packageJson = mapper.readValue(file, PackageJson.class);
            ProjectInfoUtils.merge(info, packageJson);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, packageJson);

        } catch (IOException ex) {
            throw new TaskRunnerException("update of package.json failed.", ex);
        }

    }

}

final class PackageJson implements ProjectInfo {

    private String name;

    private String version;

    private String description;

    private List<ProjectInfo.Person> people;

    private Repository repository;

    private Map<String, Object> any = new HashMap<String, Object>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("contributors")
    @Override
    public void setPeople(List<Person> people) {
        this.people = people;
    }

    @JsonProperty("contributors")
    @Override
    public List<Person> getPeople() {
        return people;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Object get(String key) {
        return any.get(key);
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return any;
    }

    @JsonAnySetter
    public void set(String key, Object value) {
        any.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("PackageJson{name=%s, version=%s}", name, version);
    }
}
