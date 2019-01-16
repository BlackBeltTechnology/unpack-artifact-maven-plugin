package hu.blackbelt.maven.plugin.unpack;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
public class Artifact {

    @Parameter(name = "groupId", required = true)
    String groupId;

    @Parameter(name = "artifactId", required = true)
    String artifactId;

    @Parameter(name = "version", required = true)
    String version;

    @Parameter(name = "extenstion", required = false)
    String extension;

    @Parameter(name = "classifier", required = false)
    String classifier;

    @Parameter(name = "resources", required = false)
    List<SourceTargetPath> resources;

}
