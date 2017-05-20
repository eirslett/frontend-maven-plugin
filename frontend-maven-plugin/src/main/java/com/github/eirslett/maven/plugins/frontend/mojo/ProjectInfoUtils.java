package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.DefaultProjectInfo;
import com.github.eirslett.maven.plugins.frontend.lib.ProjectInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

public class ProjectInfoUtils {

    public static ProjectInfo convert(MavenProject project) {

        ProjectInfo info = new DefaultProjectInfo();
        info.setName(project.getName());
        info.setVersion(project.getVersion());
        info.setDescription(project.getDescription());
        info.setPeople(convert(project.getDevelopers()));
        info.setRepository(convert(project.getScm()));

        return info;
    }

    public static List<ProjectInfo.Person> convert(List<Developer> developers) {
        if (developers == null) {
            return null;
        }
        List<ProjectInfo.Person> people = new ArrayList<ProjectInfo.Person>();
        for (Developer developer : developers) {
            people.add(new ProjectInfo.Person(developer.getName(), developer.getEmail(), developer.getUrl()));
        }

        return people;
    }

    public static ProjectInfo.Repository convert(Scm scm) {
        if (scm == null || scm.getConnection() == null || !scm.getConnection().startsWith("scm:")) {
            return null;
        }

        final Pattern pattern = Pattern.compile("scm:([^:]+):(.*)");
        final Matcher matcher = pattern.matcher(scm.getConnection());
        if (matcher.find()) {
            return new ProjectInfo.Repository(matcher.group(1), matcher.group(2));
        }

        return null;
    }

}
