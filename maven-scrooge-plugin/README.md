# maven-scrooge-plugin

## Using the plugin
Here is an example of how to add maven-scrooge-plugin into your maven project.

    <dependencies>
      <dependency>
        <groupId>com.foocorp</groupId>
        <artifactId>foo-thrift-jar</artifactId>
        <classifier>idl</classifier>
      </dependency>
    </dependencies>
    <plugins>
      ...
      <plugin>
        <groupId>com.twitter</groupId>
        <artifactId>maven-scrooge-plugin</artifactId>
        <version>3.0.6-SNAPSHOT</version>
        <configuration>
          <thriftNamespaceMappings>
            <thriftNamespaceMapping>
              <from>mythrift.bird</from>
              <to>mythrift.bird_renamed</to>
            </thriftNamespaceMapping>
          </thriftNamespaceMappings>
          <thriftOpts>
            <!-- add other Scrooge command line options using thriftOpts -->
            <thriftOpt>--finagle</thriftOpt>
            <thriftOpt>--ostrich</thriftOpt>
            <thriftOpt>-v</thriftOpt>
          </thriftOpts>
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
    </plugins>