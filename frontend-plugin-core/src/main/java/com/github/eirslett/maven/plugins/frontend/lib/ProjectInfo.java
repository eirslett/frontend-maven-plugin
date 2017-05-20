package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.List;

public interface ProjectInfo {

    String getName();

    void setName(String name);

    String getVersion();

    void setVersion(String version);

    String getDescription();

    void setDescription(String description);

    void setPeople(List<Person> people);

    List<Person> getPeople();

    Repository getRepository();

    void setRepository(Repository repository);

    public class Person {

        private String name;

        private String email;

        private String url;

        public Person() {
        }

        public Person(String name, String email, String url) {
            this.name = name;
            this.email = email;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public class Repository {

        private String type;

        private String url;

        public Repository() {
        }

        public Repository(String type, String url) {
            this.type = type;
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

}

final class ProjectInfoUtils {

    public static ProjectInfo merge(final ProjectInfo from, ProjectInfo to) {
        
        to.setName(to.getName());
        to.setVersion(from.getVersion());
        to.setDescription(from.getDescription());
        to.setPeople(from.getPeople());
        to.setRepository(from.getRepository());
        
        return to;
    }

}
