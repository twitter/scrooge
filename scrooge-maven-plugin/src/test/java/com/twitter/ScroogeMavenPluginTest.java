package com.twitter;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class ScroogeMavenPluginTest extends AbstractMojoTestCase {
  /** {@inheritDoc} */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Copy the test artifacts to default local repository location
    https://maven.apache.org/plugin-testing/maven-plugin-testing-harness/examples/repositories.html
    FileUtils.copyDirectoryStructure(
      new File(getBasedir() + "/src/test/resources/unit/project-idl-deps/local-repository"),
      new File(getBasedir() + "/target/local-repo"));
  }

  /** {@inheritDoc} */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    FileUtils.deleteDirectory(getBasedir() + "/target/local-repo");
  }

  /**
   * Test if scrooge maven plugin is loaded in compile phase.
   * @throws Exception if any
   */
  public void testScroogeMavenPluginLoads() throws Exception {
    File testPom = new File(getBasedir(),
      "src/test/resources/unit/scrooge-maven-plugin-test/plugin-config.xml");
    assertNotNull(testPom);
    assertTrue(testPom.exists());

    MavenScroogeCompileMojo mojo = (MavenScroogeCompileMojo) lookupMojo("compile", testPom);
    assertNotNull(mojo);
  }

  /**
   * Test if scrooge can resolve idls transitively.
   * @throws Exception if any
   */
  public void testIdlDependenciesResolvedTransitively() throws Exception {
    String testResourcesBaseDir = getBasedir() + "/src/test/resources/unit/project-idl-deps/";
    File testPom = new File(testResourcesBaseDir, "pom.xml");
    assertNotNull(testPom);
    assertTrue(testPom.exists());
    final MavenScroogeCompileMojo mojo = (MavenScroogeCompileMojo) lookupMojo("compile", testPom);
    assertNotNull(mojo);
    mojo.execute();
    // Check if both thrift artifacts have scrooge generated classes
    String[] fileNames =  new String[] {"/scrooge/com/twitter/hello/thriftjava/HelloMessage.java",
                                        "/scrooge/com/twitter/person/thriftjava/Person.java"};
    final Collection<String> absExpectFiles = Collections2.transform(
      Arrays.asList(fileNames),
      new Function<String, String>() {
        @Override
        public String apply(final String input) {
          return new String(mojo.getOutputDirectory() + input);
        }
      });

    Collection<File> actualFiles = org.apache.commons.io.FileUtils.listFiles(
      mojo.getOutputDirectory(),
      null,
      true);
    final Collection<String> absActualFiles = Collections2.transform(
      actualFiles,
      new Function<File, String>() {
        @Override
        public String apply(final File input) {
          return new String(input.getAbsolutePath());
        }
      });
    assertTrue(CollectionUtils.isEqualCollection(absExpectFiles, absActualFiles));
  }

  public void testUnresolvedIdl() throws Exception{
    File testPom = new File(getBasedir(), "/src/test/resources/unit/unresolved-idl/pom.xml");
    assertNotNull(testPom);
    assertTrue(testPom.exists());
    MavenScroogeCompileMojo mojo = (MavenScroogeCompileMojo) lookupMojo("compile", testPom);
    try {
      mojo.execute();
      fail("Expected MojoExecution Exception");
    } catch (MojoExecutionException e) {
      String artifact_name= "com.twitter:person-java:jar:idl:0.0.1:compile";
      assertEquals(String.format("Could not resolve idl thrift dependency %s", artifact_name),
                   e.getMessage());
    }

  }

}
