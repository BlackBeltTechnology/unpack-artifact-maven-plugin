package hu.blackbelt.maven.plugin.unpack;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class SourceTargetPath {
    String source;
    String destination;
}
