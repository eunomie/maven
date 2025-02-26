package io.dagger.modules.maven;

import io.dagger.client.*;
import io.dagger.module.AbstractModule;
import io.dagger.module.annotation.Function;
import io.dagger.module.annotation.Object;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Object
public class Maven extends AbstractModule {
  private static final String mavenImage = "maven:3.9.9-eclipse-temurin-23-alpine";
  private static final String mavenDigest =
      "sha256:0e5e89100c3c1a0841ff67e0c1632b9b983e94ee5a9b1f758125d9e43c66856f";

  public Container mavenContainer;

  public Maven() {}

  public Maven(Client dag, Optional<Directory> sources) {
    super(dag);
    this.mavenContainer =
        dag.container()
            .from("%s@%s".formatted(mavenImage, mavenDigest))
            .withMountedCache("/root/.m2", dag.cacheVolume("maven-m2"))
            .withWorkdir("/src");
    sources.ifPresent(s -> this.withSources(s));
  }

  /** Run maven commands */
  @Function
  public Maven withMvnExec(List<String> commands) {
    this.mavenContainer =
        this.mavenContainer.withExec(Stream.concat(Stream.of("mvn"), commands.stream()).toList());
    return this;
  }

  /** Retrieve the jar file */
  @Function
  public File jar() throws ExecutionException, DaggerQueryException, InterruptedException {
    pkg();
    return mavenContainer.file(jarFileName());
  }

  public String jarFileName()
      throws ExecutionException, DaggerQueryException, InterruptedException {
    String artifactID =
        mavenContainer
            .withExec(
                List.of(
                    "mvn",
                    "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate",
                    "-Dexpression=project.artifactId",
                    "-q",
                    "-DforceStdout"))
            .stdout();
    String version =
        mavenContainer
            .withExec(
                List.of(
                    "mvn",
                    "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate",
                    "-Dexpression=project.version",
                    "-q",
                    "-DforceStdout"))
            .stdout();
    return String.format("target/%s-%s.jar", artifactID, version);
  }

  /**
   * Mount source directory
   *
   * @param source Source directory
   */
  @Function
  public Maven withSources(Directory source) {
    this.mavenContainer = this.mavenContainer.withMountedDirectory("/src", source);
    return this;
  }

  /** Run maven package */
  @Function
  public Maven pkg() {
    return this.withMvnExec(List.of("package"));
  }

  /** Run maven clean */
  @Function
  public Maven clean() {
    return this.withMvnExec(List.of("clean"));
  }

  /** Run maven install */
  @Function
  public Maven install() {
    return this.withMvnExec(List.of("install"));
  }

  /** Run maven test */
  @Function
  public Maven test() {
    return this.withMvnExec(List.of("test"));
  }

  /** Execute a command */
  @Function
  public Maven withExec(List<String> commands) {
    this.mavenContainer = this.mavenContainer.withExec(commands);
    return this;
  }
}
