package com.twitter;

import com.google.common.collect.ImmutableList;

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
 *
 * @phase generate-test-sources
 * @goal testCompile
 * @threadSafe true
 */

public final class MavenScroogeTestCompileMojo extends AbstractMavenScroogeMojo {

  /**
   * The source directories containing the sources to be compiled.
   *
   * @parameter default-value="${basedir}/src/test/thrift"
   * @required
   */
  private File thriftSourceRoot;

  /**
   * This is the directory into which the {@code .java} will be created.
   *
   * @parameter default-value="${project.build.directory}/generated-test-sources/thrift"
   * @required
   */
  private File outputDirectory;

  /**
   * This is the directory into which dependent {@code .thrift} files will be extracted.
   *
   * @parameter default-value="${project.build.directory}/generated-test-resources/"
   * @required
   */
  private File resourcesOutputDirectory;

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
            ImmutableList.of("**/*.thrift"), ImmutableList.of());
    projectHelper.addResource(project, resourcesOutputDirectory.getAbsolutePath(),
            ImmutableList.of("**/*.thrift"), ImmutableList.of());
  }

  @Override
  protected String getDependencyScopeFilter() {
    return "test";
  }

  @Override
  protected List<File> getReferencedThriftFiles() throws IOException {
    return getRecursiveThriftFiles(project, "test-classes");
  }
}
