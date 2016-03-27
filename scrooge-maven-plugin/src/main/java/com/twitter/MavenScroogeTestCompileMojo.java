package com.twitter;

import com.google.common.collect.ImmutableList;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * This mojo executes the {@code thrift} compiler for generating <i>test</i>java sources
 * from thrift definitions. It also searches dependency artifacts for
 * thrift files and includes them in the thriftPath so that they can be
 * referenced. Finally, it adds the thrift files to the project as resources so
 * that they are included in the final artifact.
 */
@Mojo(name="testCompile", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public final class MavenScroogeTestCompileMojo extends AbstractMavenScroogeMojo {

  /**
   * The source directories containing the sources to be compiled.
   */
  @Parameter(required = true, defaultValue = "${basedir}/src/test/thrift")
  private File thriftSourceRoot;

  /**
   * This is the directory into which the {@code .java} will be created.
   */
  @Parameter(required = true, defaultValue = "${project.build.directory}/generated-test-sources/thrift")
  private File outputDirectory;

  /**
   * This is the directory into which dependent {@code .thrift} files will be extracted.
   */
  @Parameter(required = true, defaultValue = "${project.build.directory}/generated-test-resources/")
  private File resourcesOutputDirectory;

  @Inject
  protected MavenScroogeTestCompileMojo(MavenProjectHelper projectHelper, ProjectDependenciesResolver projectDependenciesResolver, RepositorySystem repoSystem) {
    super(projectHelper, projectDependenciesResolver, repoSystem);
  }

  @Override
  protected File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  protected File getResourcesOutputDirectory() {
    return resourcesOutputDirectory;
  }

  @Override
  protected File getThriftSourceRoot() {
    return thriftSourceRoot;
  }

  @Override
  protected void attachFiles(Set<String> compileRoots) {
    for (String root : compileRoots) {
      project.addTestCompileSourceRoot(new File(outputDirectory, root).getAbsolutePath());
    }
    projectHelper.addResource(project, thriftSourceRoot.getAbsolutePath(),
            ImmutableList.of("**/*.thrift"), ImmutableList.<String>of());
    projectHelper.addResource(project, resourcesOutputDirectory.getAbsolutePath(),
            ImmutableList.of("**/*.thrift"), ImmutableList.<String>of());
  }


  @Override
  protected List<File> getReferencedThriftFiles() throws IOException {
    return getRecursiveThriftFiles(project, "test-classes");
  }
}
