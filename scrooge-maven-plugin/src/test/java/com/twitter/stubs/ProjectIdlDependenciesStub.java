package com.twitter.stubs;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public class ProjectIdlDependenciesStub extends MavenProjectStub {
  /**
   *  Defines a ProjectStub with attributes from configuration.
   */
  public ProjectIdlDependenciesStub() {
    MavenXpp3Reader pomReader = new MavenXpp3Reader();
    Model model;
    try {
      model = pomReader.read(ReaderFactory.newXmlReader(new File(getBasedir(), "pom.xml")));
      setModel(model);

      setGroupId(model.getGroupId());
      setArtifactId(model.getArtifactId());
      setVersion(model.getVersion());
      setName(model.getName());
      setUrl(model.getUrl());
      setPackaging(model.getPackaging());
      setDependencies(model.getDependencies());

      Build build = new Build();
      build.setFinalName(getArtifactId() + "-" + getVersion());
      build.setDirectory(getBasedir() + "target");
      setBuild(build);
      setFile(new File(getBasedir(), "pom.xml"));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (XmlPullParserException e) {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} */
  @Override
  public File getBasedir() {
    return new File(super.getBasedir() + "/src/test/resources/unit/project-idl-deps");
  }
}
