package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.List;

public class DefaultProjectInfo implements ProjectInfo {

    private String name;

    private String version;

    private String description;

    private List<Person> people;

    private Repository repository;

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

    @Override
    public void setPeople(List<Person> people) {
        this.people = people;
    }

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

    @Override
    public String toString() {
        return String.format("PackageJson{name=%s, version=%s}", name, version);
    }

}
