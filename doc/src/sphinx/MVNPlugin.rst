Maven Plugin
============

Depending on the options used to generate the code, a few dependencies
need to be declared.

::

    <dependency>
      <groupId>org.apache.thrift</groupId>
      <artifactId>libthrift</artifactId>
      <version>0.8.0</version>
    </dependency>
    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>scrooge-core_2.9.2</artifactId>
      <version>3.3.2</version>
    </dependency>
    <!-- needed if the "--finagle" flag is provided -->
    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>finagle-thrift_2.9.2</artifactId>
      <version>6.5.1</version>
    </dependency>


In order to generate code with the maven plugin the following plugin
configuration will need to be added to the build/plugins section of your
pom.xml file.

::

    <plugin>
      <groupId>com.twitter</groupId>
      <artifactId>scrooge-maven-plugin</artifactId>
      <version>3.14.1</version>
      <configuration>
        <thriftNamespaceMappings>
          <thriftNamespaceMapping>
            <from>com.twitter.demo</from>
            <to>com.twitter.mydemo.renamed</to>
          </thriftNamespaceMapping>
        </thriftNamespaceMappings>
        <language>scala</language> <!-- default is scala -->
        <thriftOpts>
          <!-- add other Scrooge command line options using thriftOpts -->
          <thriftOpt>--finagle</thriftOpt>
        </thriftOpts>
        <!-- tell scrooge to extract thrifts from these artifacts -->
        <dependencyIncludes>
          <include>event-logger-thrift</include>
        </dependencyIncludes>
        <!-- tell scrooge to not to build the extracted thrift files (defaults to true) -->
        <buildExtractedThrift>false</buildExtractedThrift>
      </configuration>
      <executions>
        <execution>
          <id>thrift-sources</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>compile</goal>
          </goals>
        </execution>
        <execution>
          <id>thrift-test-sources</id>
          <phase>generate-test-sources</phase>
          <goals>
            <goal>testCompile</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
