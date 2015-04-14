package com.twitter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;


public class MavenScroogeCompilerUtil {
  /**
   * Resolves an idl jar for the artifact.
   * @return Returns idl artifact
   * @throws MojoExecutionException is idl jar is not present for the artifact.
   */
  public static Artifact getIdlArtifact(Artifact artifact, ArtifactFactory artifactFactory,
                                        ArtifactResolver artifactResolver,
                                        ArtifactRepository localRepository,
                                        List<ArtifactRepository> remoteRepos,
                                        String classifier)
    throws MojoExecutionException {
    Artifact idlArtifact = artifactFactory.createArtifactWithClassifier(
                                             artifact.getGroupId(),
                                             artifact.getArtifactId(),
                                             artifact.getVersion(),
                                             "jar",
                                             classifier);
    try {
      artifactResolver.resolve(idlArtifact, remoteRepos, localRepository);
      return idlArtifact;
    } catch (final ArtifactResolutionException e) {
      throw new MojoExecutionException(
        "Failed to resolve one or more idl artifacts:\n\n" + e.getMessage(), e);
    } catch (final ArtifactNotFoundException e) {
      throw new MojoExecutionException(
      "Failed to resolve one or more idl artifacts:\n\n" + e.getMessage(), e);
    }
  }
}
