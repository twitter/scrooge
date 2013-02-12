# maven-scrooge-plugin

## Using the plugin
Here is an example of how to add maven-scrooge-plugin into your maven project.

    <dependencies>
      ...
      <!-- dependencies where we want to extract thrift files from -->
      <dependency>
        <groupId>com.twitter</groupId>
        <artifactId>foo-thrift-only</artifactId>
        <version>4.3.0</version>
        <classifier>idl</classifier>
      </dependency>
      <dependency>
        <groupId>com.twitter</groupId>
        <artifactId>bar-thrift-only</artifactId>
        <version>7.8.0</version>
        <!-- not an idl classifer, need to be added to whitelist below -->
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
              <from>com.twitter.old</from>
              <to>com.twitter.new</to>
            </thriftNamespaceMapping>
          </thriftNamespaceMappings>
          <thriftOpts>
            <!-- add other Scrooge command line options using thriftOpts -->
            <thriftOpt>--finagle</thriftOpt>
            <thriftOpt>--ostrich</thriftOpt>
            <thriftOpt>-v</thriftOpt>
          </thriftOpts>
          <dependencyConfig> <!-- this is the whitelist -->
            <include>bar-thrift-only</include>
          </dependencyConfig>
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