# maven-unpack-artifact-plugin

More customizable maven unpack plugin. The goal of this packing to use instead of dependency plugin whic cannot unpack files from artifacts without full path.


Usage:

```

            <plugin>
                <groupId>hu.blackbelt</groupId>
                <artifactId>unpack-artifact-maven-plugin</artifactId>
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <id>copy-files-from-jar</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>unpack</goal>
                        </goals>

                        <configuration>
                            <outDirectory>${basedir}/src/main</outDirectory>
                            <artifacts>
                                <artifact>
                                    <groupId>hu.blackbelt</groupId>
                                    <artifactId>testartifact</artifactId>
                                    <version>${judo.expression.version}</version>
                                    <resources>
                                        <resource>
                                            <source>jar/path</source>
                                            <destination>target/path</destination>
                                        </resource>
                                   </resources>
                                </artifact>
                            </artifacts>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

```

This example unpack jar/path entries from testartifact into target/path folder recursively. The unpack checking the files, and if there is difference in the content the plugin fails.

