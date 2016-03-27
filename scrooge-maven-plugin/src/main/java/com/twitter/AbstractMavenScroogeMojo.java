package com.twitter;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.JavaScopes;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.list;
import static org.codehaus.plexus.util.FileUtils.*;


/**
 * Abstract Mojo implementation.
 * This class is extended by {@link MavenScroogeCompileMojo} and
 * {@link MavenScroogeTestCompileMojo} in order to override the specific configuration for
 * compiling the main or test classes respectively.
 */
abstract class AbstractMavenScroogeMojo extends AbstractMojo {

  private static final String THRIFT_FILE_SUFFIX = ".thrift";

  private static final String DEFAULT_INCLUDES = "**/*" + THRIFT_FILE_SUFFIX;

  /**
   * The current Maven project.
   */
  @Parameter( defaultValue = "${project}", readonly = true, required = true )
  protected MavenProject project;

  /**
   * A helper used to add resources to the project.
   */
  protected final MavenProjectHelper projectHelper;


  /**
   * A helper used to get all transative dependencies for the current project.
   */
  protected final ProjectDependenciesResolver projectDependenciesResolver;

  /**
   * The entry point to Aether, i.e. the component doing all the work.
   */
  protected final RepositorySystem repoSystem;

  /**
   * The current repository/network configuration of Maven.
   */
  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession session;

  /**
   * A set of include directories to pass to the thrift compiler.
   */
  @Parameter
  private Set<File> thriftIncludes = new HashSet<File>();

  /**
   * Which language the generated files should be ("scala")
   * {@code
   * <configuration>
   *     <language>scala</language>
   * </configuration>
   * }
   */
  @Parameter(defaultValue = "scala")
  private String language;

  /**
   * Command line options to pass to scrooge, e.g.
   * {@code
   * <thriftOpts>
   *   <thriftOpt>--finagle</thriftOpt>
   * </thriftOpts>
   *}
   */
  @Parameter
  private Set<String> thriftOpts = new HashSet<String>();

  /***
   * A set of forced libraries for inclusion into the plugin when determining
   * dependencies to search for IDL files in.
   */
  @Parameter
  private Set<Module> thriftLibraries = new HashSet<Module>();

  /**
   * A set of namespace mappings to pass to the thrift compiler, e.g.
   * {@code
   * <thriftNamespaceMappings>
   *   <thriftNamespaceMapping>
   *      <from>com.twitter</from>
   *      <to>com.twitter.thriftscala</to>
   *   </thriftNamespaceMapping>
   * </thriftNamespaceMappings>
   * }
   *
   * Only used by the scrooge generator, usually to avoid clashes with Java namespaces.
   */
  @Parameter
  private Set<ThriftNamespaceMapping> thriftNamespaceMappings = new HashSet<ThriftNamespaceMapping>();

  /**
   * A set of include patterns used to filter thrift files.
   */
  @Parameter
  private Set<String> includes = ImmutableSet.of(DEFAULT_INCLUDES);

  /**
   * A set of exclude patterns used to filter thrift files.
   * @parameter
   */
  private Set<String> excludes = ImmutableSet.of();

  /**
   * Whether or not to build the thrift extracted from dependencies, if any
   * {@code
   * <configuration>
   *     <buildExtractedThrift>false</buildExtractedThrift>
   * </configuration>
   * }
   */
  @Parameter
  private boolean buildExtractedThrift = true;

  /**
   * Whether or not to skip thrift generation if generated files are newer than source files.
   */
  @Parameter
  private boolean checkStaleness = true;

  /**
   * Delta to use for triggering thrift regeneration
   * @parameter
   */
  private long staleMillis = 0;

  private static Object lock = new Object();

  /**
   * Classifier for the idl thrift libraries
   * {@code
   * <classifier>idl</classifier>
   * }
   */
  @Parameter(property = "maven.scrooge.classifier", defaultValue = "idl" )
  protected String classifier;


  protected AbstractMavenScroogeMojo(MavenProjectHelper projectHelper, ProjectDependenciesResolver projectDependenciesResolver,
                                     RepositorySystem repoSystem) {
    this.projectHelper = projectHelper;
    this.projectDependenciesResolver = projectDependenciesResolver;
    this.repoSystem = repoSystem;
  }


  /**
   * Executes the mojo.
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      Set<File> thriftFiles = findThriftFiles();

      final File outputDirectory = getOutputDirectory();
      ImmutableSet<File> outputFiles = findGeneratedFilesInDirectory(getOutputDirectory());

      Set<String> compileRoots = new HashSet<String>();
      compileRoots.add("scrooge");

      if (thriftFiles.isEmpty()) {
        getLog().info("No thrift files to compile.");
      } else if (checkStaleness && ((lastModified(thriftFiles) + staleMillis) < lastModified(outputFiles))) {
        getLog().info("Generated thrift files up to date, skipping compile.");
        attachFiles(compileRoots);
      } else {
        outputDirectory.mkdirs();

        // Quick fix to fix issues with two mvn installs in a row (ie no clean)
        cleanDirectory(outputDirectory);

        getLog().info(format("compiling thrift files %s with Scrooge", thriftFiles));
        synchronized(lock) {
          ScroogeRunner runner = new ScroogeRunner();
          Map<String, String> thriftNamespaceMap = new HashMap<String, String>();
          for (ThriftNamespaceMapping mapping : thriftNamespaceMappings) {
            thriftNamespaceMap.put(mapping.getFrom(), mapping.getTo());
          }

          // Include thrifts from resource as well.
          Set<File> includes = thriftIncludes;
          includes.add(getResourcesOutputDirectory());

          // Include thrift root
          final File thriftSourceRoot = getThriftSourceRoot();
          if (thriftSourceRoot != null && thriftSourceRoot.exists()) {
            includes.add(thriftSourceRoot);
          }

          runner.compile(
                  getLog(),
                  new File(outputDirectory, "scrooge"),
                  thriftFiles,
                  includes,
                  thriftNamespaceMap,
                  language,
                  thriftOpts);
        }
        attachFiles(compileRoots);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("An IO error occured", e);
    } catch (DependencyResolutionException e) {
      throw new MojoExecutionException("An dependency exception error occured", e);
    } catch (DependencyCollectionException e) {
      throw new MojoExecutionException("An dependency exception error occured", e);
    } catch (org.eclipse.aether.resolution.DependencyResolutionException e) {
      throw new MojoExecutionException("An dependency exception error occured", e);
    }
  }

  /**
   * Where our local thrift files live.
   */
  protected abstract File getThriftSourceRoot();

  /**
   * Where our generated files go.
   */
  protected abstract File getOutputDirectory();

  /**
   * Where all our thrift files (from references, dependencies, local) get copied.
   */
  protected abstract File getResourcesOutputDirectory();

  /**
   * Add newly created files to the project.
   * @return A set of directories that contain generated source files.
   */
  protected abstract void attachFiles(Set<String> compileRoots);

  /**
   * Thrift files from referenced projects.
   */
  protected abstract List<File> getReferencedThriftFiles() throws IOException;

  /**
   * Get the last modified time for a set of files.
   */
  private long lastModified(Set<File> files) {
    long result = 0;
    for (File file : files) {
      if (file.lastModified() > result)
        result = file.lastModified();
    }
    return result;
  }

  /**
   * build a complete set of local files, files from referenced projects, and dependencies.
   */
  private Set<File> findThriftFiles() throws IOException, MojoExecutionException, DependencyResolutionException, DependencyCollectionException, org.eclipse.aether.resolution.DependencyResolutionException {
    final File thriftSourceRoot = getThriftSourceRoot();
    Set<File> thriftFiles = Sets.newHashSet();
    if (thriftSourceRoot != null && thriftSourceRoot.exists()) {
      thriftFiles.addAll(findThriftFilesInDirectory(thriftSourceRoot));
    }
    getLog().info("finding thrift files in dependencies");
    extractFilesFromDependencies(findThriftDependencies(), getResourcesOutputDirectory());
    if (buildExtractedThrift && getResourcesOutputDirectory().exists()) {
      thriftFiles.addAll(findThriftFilesInDirectory(getResourcesOutputDirectory()));
    }
    getLog().info("finding thrift files in referenced (reactor) projects");
    thriftFiles.addAll(getReferencedThriftFiles());
    return thriftFiles;
  }



  /**
   * Iterate through dependencies
   */
  private Set<Artifact> findThriftDependencies() throws IOException, MojoExecutionException, DependencyResolutionException, DependencyCollectionException, org.eclipse.aether.resolution.DependencyResolutionException {
    Set<Artifact> thriftDependencies = new HashSet<Artifact>();

    DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project,
                                                                                        session);
    DependencyResolutionResult result = projectDependenciesResolver.resolve(request);

    Set<Artifact> transitiveDeps = FluentIterable.from(result.getDependencies()).transform(DEPENDENCY_TO_ARTIFACT).toSet();

    for (Artifact artifact : transitiveDeps) {

      // This artifact has an idl classifier or is forced into inclusion
      if (isIdlCalssifier(artifact, classifier) || isForcedInclusion(artifact)) {
        thriftDependencies.add(artifact);

        CollectRequest collectRequest = new CollectRequest()
                .setRoot(new Dependency(artifact, JavaScopes.COMPILE)).setRepositories(project.getRemoteProjectRepositories());
        DependencyRequest dependencyRequest = new DependencyRequest().setCollectRequest(collectRequest);
        Set<Artifact> idlTransitiveDeps = FluentIterable.from(repoSystem.resolveDependencies(session, dependencyRequest).getArtifactResults())
                .transform(ARTIFACT_RESULT_TO_ARTIFACT).toSet();
        thriftDependencies.addAll(idlTransitiveDeps);

      }
    }
    return thriftDependencies;
  }

  private boolean isForcedInclusion(final Artifact artifact) {
    return FluentIterable.from(thriftLibraries).anyMatch(new Predicate<Module>() {
      @Override
      public boolean apply(@Nullable Module input) {
        if (input == null || input.getGroupId() == null || input.getArtifactId() == null) {
          return false;
        }
        return input.getArtifactId().equals(artifact.getArtifactId()) && input.getGroupId().equals(artifact.getGroupId());
      }
    });
  }

  /**
   * Copy thrift files from dependency jars to {@link #getResourcesOutputDirectory()}.
   * @param dependencies A set of jar artifacts ths project depends on.
   * @param destFolder The directory to copy any found files into.
   */
  private void extractFilesFromDependencies(Collection<Artifact> dependencies, File destFolder)
    throws IOException, MojoExecutionException{
    for (Artifact idlArtifact : dependencies) {

      File dep = idlArtifact.getFile();
      if (dep.isFile() && dep.canRead()) {
        getLog().info("Extracting thrift files from " + dep.getCanonicalPath());
        JarFile jar = new JarFile(dep);
        for (JarEntry entry : list(jar.entries())) {
          if (entry.getName().endsWith(THRIFT_FILE_SUFFIX)) {
            File destination = new File(destFolder, entry.getName());

            if (destination.isFile() && dep.lastModified() <= destination.lastModified()) {
              if (!haveSameContents(destination, jar, entry)) {
                throw new IOException(format("extracting %s from %s would overwrite %s", entry.getName(), dep.getCanonicalPath(), destination.getCanonicalPath()));
              } else {
                getLog().info(format("skipping extraction of %s from %s", entry.getName(), dep.getCanonicalPath()));
              }
            } else {
              if (destination.isFile()) {
                getLog().warn(format("overwriting %s with %s", entry.getName(), destination.getCanonicalPath()));
              } else {
                getLog().info(format("extracting %s to %s", entry.getName(), destination.getCanonicalPath()));
              }
              copyStreamToFile(new RawInputStreamFacade(jar.getInputStream(entry)), destination);
              if (!destination.setLastModified(dep.lastModified()))
                getLog().warn(format("fail to set last modified time for %s", destination.getCanonicalPath()));
            }
          }
        }
      } else {
        getLog().warn(format("dep %s isn't a file or can't be read", dep.getCanonicalPath()));
      }
    }
  }

  private boolean haveSameContents(File file, final JarFile jar, final JarEntry entry) throws IOException {
    HashFunction hashFun = Hashing.md5();
    HashCode fileHash = Files.hash(file, hashFun);
    HashCode streamHash = ByteStreams.hash(new InputSupplier<InputStream>() {
      public InputStream getInput() throws IOException { return jar.getInputStream(entry); }
    }, hashFun);
    return fileHash.equals(streamHash);
  }

  /**
   * Find all {@code .java} and {@code .scala} files in the given directory.
   */
  private ImmutableSet<File> findGeneratedFilesInDirectory(File directory) throws IOException {
    if (directory == null || !directory.isDirectory())
      return ImmutableSet.of();
    List<File> sourceFilesInDirectory = getFiles(directory, "**/*.java", null);
    sourceFilesInDirectory.addAll(getFiles(directory, "**/*.scala", null));
    return ImmutableSet.copyOf(sourceFilesInDirectory);
  }

  /**
   * Find all {@code .thrift} files in the given directory.
   */
  private ImmutableSet<File> findThriftFilesInDirectory(File directory) throws IOException {
    checkNotNull(directory);
    checkArgument(directory.isDirectory(), "%s is not a directory", directory);
    List<File> thriftFilesInDirectory = getFiles(directory, Joiner.on(",").join(includes), Joiner.on(",").join(excludes));
    return ImmutableSet.copyOf(thriftFilesInDirectory);
  }

  /**
   * Walk project references recursively, building up a list of thrift files they provide, starting
   * with an empty file list.
   */
  protected List<File> getRecursiveThriftFiles(MavenProject project, String outputDirectory) throws IOException {
    return getRecursiveThriftFiles(project, outputDirectory, new ArrayList<File>());
  }

  /**
   * Walk project references recursively, adding thrift files to the provided list.
   */
  List<File> getRecursiveThriftFiles(MavenProject project, String outputDirectory, List<File> files) throws IOException {
    HashFunction hashFun = Hashing.md5();
    File dir = new File(new File(project.getFile().getParent(), "target"), outputDirectory);
    if (dir.exists()) {
      URI baseDir = getFileURI(dir);
      for (File f : findThriftFilesInDirectory(dir)) {
        URI fileURI = getFileURI(f);
        String relPath = baseDir.relativize(fileURI).getPath();
        File destFolder = getResourcesOutputDirectory();
        destFolder.mkdirs();
        File destFile = new File(destFolder, relPath);
        if (!destFile.exists() || (destFile.isFile() && !Files.hash(f, hashFun).equals(Files.hash(destFile, hashFun)))) {
          getLog().info(format("copying %s to %s", f.getCanonicalPath(), destFile.getCanonicalPath()));
          copyFile(f, destFile);
        }
        files.add(destFile);
      }
    }
    Map<String, MavenProject> refs = project.getProjectReferences();
    for (String name : refs.keySet()) {
      getRecursiveThriftFiles(refs.get(name), outputDirectory, files);
    }
    return files;
  }

  URI getFileURI(File file) throws IOException {
    try {
      return new URI("file://"+(file.getCanonicalPath().replace("\\","/")));
    } catch (URISyntaxException urie) {
      throw new IOException("error forming URI for file transfer: " + urie);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected String getClassifier() {
    return classifier;
  }

  /**
   Returns true if artifact has idl classifier
   */
  private boolean isIdlCalssifier(Artifact artifact, String classifier) {
    return classifier.equalsIgnoreCase(artifact.getClassifier());
  }

  private static final Function<Dependency, Artifact> DEPENDENCY_TO_ARTIFACT =  new Function<Dependency, Artifact>() {
    @Nullable
    @Override
    public Artifact apply(@Nullable Dependency dependency) {
      if (dependency == null) {
        return null;
      }
      return dependency.getArtifact();
    }
  };

  private static final Function<ArtifactResult, Artifact> ARTIFACT_RESULT_TO_ARTIFACT =  new Function<ArtifactResult, Artifact>() {
    @Nullable
    @Override
    public Artifact apply(@Nullable ArtifactResult artifactResult) {
      if (artifactResult == null) {
        return null;
      }
      return artifactResult.getArtifact();
    }
  };


}
