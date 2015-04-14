package com.twitter.stubs;

import org.apache.maven.plugin.testing.stubs.ArtifactStub;

import java.util.List;

public class IdlDepArtifactStub extends ArtifactStub {
  /**
   *  Defines a ArtifactStub with configured dependency.
   */
  private List<String> idlDeps;
  private boolean resolved = false;

  @Override
  public List getDependencyTrail() {
    return idlDeps;
  }

  /**
   * Set the idl dependencies using the parameter idlDeps.
   * @param idlDeps
   */
  public void setIdlDeps(List<String> idlDeps) {
    this.idlDeps = idlDeps;
  }

  public String getId() {
    return this.getGroupId() + ":" + this.getArtifactId();
  }

  /**
   * Set the if artifact is resolved.
   * @param resolved
   */
  public void setResolved(boolean resolved) {
    this.resolved = resolved;
  }

  public boolean isResolved() {
    return this.resolved;
  }
}
