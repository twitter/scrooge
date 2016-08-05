package com.twitter;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ScroogeMavenPluginTest extends AbstractMojoTestCase {

  private RepositorySystemSession newSession() throws Exception
  {
    RepositorySystem system = lookup(RepositorySystem.class);
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(getBasedir() + "/target/local-repo");
    session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

    return session;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Copy the test artifacts to default local repository location
    https://maven.apache.org/plugin-testing/maven-plugin-testing-harness/examples/repositories.html
    FileUtils.copyDirectoryStructure(
      new File(getBasedir() + "/src/test/resources/unit/project-idl-deps/local-repository"),
      new File(getBasedir() + "/target/local-repo"));

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    FileUtils.deleteDirectory(getBasedir() + "/target/local-repo");
  }


  /**
   * Test if scrooge maven plugin is loaded in compile phase.
   * @throws Exception if any
   */
  @Test
  public void testScroogeMavenPluginLoads() throws Exception {
    File testPom = new File(getBasedir(),
      "src/test/resources/unit/scrooge-maven-plugin-test/plugin-config.xml");
    assertThat(testPom, not(nullValue()));
    assertThat(testPom.exists(), is(true));
    MavenScroogeCompileMojo mojo = (MavenScroogeCompileMojo) lookupMojo("compile", testPom);
    assertThat(mojo, not(nullValue()));
  }

  /**
   * Test if scrooge can resolve idls transitively.
   * Ignored at the moment because not sure how to get the Aether components to inject
   * into the Mojo!
   */
  /* TODO: IGNORED SINCE NOT SURE HOW TO MAKE WORK WITH AETHER
  @Ignore
  @Test
  public void testIdlDependenciesResolvedTransitively() throws Exception {
    String testResourcesBaseDir = getBasedir() + "/src/test/resources/unit/project-idl-deps/";
    File testPom = new File(testResourcesBaseDir, "pom.xml");
    assertThat(testPom, not(nullValue()));
    assertThat(testPom.exists(), is(true));

    final MavenScroogeCompileMojo mojo = (MavenScroogeCompileMojo) lookupMojo("compile", testPom);
    mojo.setSession(newSession());
    assertThat(mojo, not(nullValue()));
    mojo.execute();
    // Check if both thrift artifacts have scrooge generated classes
    List<String> fileNames = Lists.newArrayList("/scrooge/com/twitter/hello/thriftjava/HelloMessage.java",
                       "/scrooge/com/twitter/person/thriftjava/Person.java");
    List<String> absExpectFiles = FluentIterable.from(fileNames).transform(new Function<String, String>() {
      @Nullable
      @Override
      public String apply(@Nullable String input) {
        return mojo.getOutputDirectory() + input;
      }
    }).toList();

    Collection<File> actualFiles = org.apache.commons.io.FileUtils.listFiles(
      mojo.getOutputDirectory(),
      null,
      true);

    List<String> absActualFiles = FluentIterable.from(actualFiles).transform(new Function<File, String>() {
      @Nullable
      @Override
      public String apply(@Nullable File input) {
        return input.getAbsolutePath();
      }
    }).toList();

    assertThat(absActualFiles, is(absExpectFiles));
  }
  */

  /**
   * Test if scrooge can resolve idls transitively.
   * Ignored at the moment because not sure how to get the Aether components to inject
   * into the Mojo!
   */
  /* TODO: IGNORED SINCE NOT SURE HOW TO MAKE WORK WITH AETHER
  @Ignore
  @Test
  public void testUnresolvedIdl() throws Exception{
    File testPom = new File(getBasedir(), "/src/test/resources/unit/unresolved-idl/pom.xml");
    assertNotNull(testPom);
    assertTrue(testPom.exists());
    MavenScroogeCompileMojo mojo = (MavenScroogeCompileMojo) lookupMojo("compile", testPom);
    mojo.setSession(newSession());
    try {
      mojo.execute();
      fail("Expected MojoExecution Exception");
    } catch (MojoExecutionException e) {
      String artifact_name= "com.twitter:person-java:jar:idl:0.0.1:compile";
      assertEquals(String.format("Could not resolve idl thrift dependency %s", artifact_name),
                   e.getMessage());
    }
  }
  */

}
