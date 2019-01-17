package hu.blackbelt.maven.plugin.unpack;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mojo(
        name = "unpack",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class UnpackMojo extends AbstractMojo{

    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    @Component
    public RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    public RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    public List<RemoteRepository> repositories;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    public File outDirectory;

    @Parameter(name="artifacts", readonly = true)
    List<Artifact> artifacts;

    private Log log = new MavenLog(getLog());

    static {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
    }


    /**
     * Creating schema artifact from the given
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Running unpack");

        for (Artifact artifact : artifacts) {
            try {
                File baseZip = getArtifact(artifact);
                for (SourceTargetPath sourceTargetPath : artifact.getResources()) {
                    unzipFile(baseZip, new File(outDirectory, sourceTargetPath.getDestination()), sourceTargetPath.getSource());
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Could not unpack artifact", e);
            }
        }

    }

    /**
     * Get the given artifact as file from maven repository.
     * @param schemaArtifact
     * @return
     * @throws MojoExecutionException
     */
    protected File getArtifact(Artifact schemaArtifact) throws MojoExecutionException {
            org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(
                    schemaArtifact.getGroupId(),
                    schemaArtifact.getArtifactId(),
                    schemaArtifact.getClassifier(),
                    schemaArtifact.getExtension(),
                    schemaArtifact.getVersion());

            ArtifactRequest req = new ArtifactRequest().setRepositories(this.repositories).setArtifact(artifact);
            ArtifactResult resolutionResult;

            try {
                resolutionResult = this.repoSystem.resolveArtifact(this.repoSession, req);

            } catch (ArtifactResolutionException e) {
                throw new MojoExecutionException("Artifact " + schemaArtifact.toString() + " could not be resolved.", e);
            }

            // The file should exists, but we never know.
            File file = resolutionResult.getArtifact().getFile();
            if (file == null || !file.exists()) {
                log.warn("Artifact " + schemaArtifact.toString() + " has no attached file.");
            }
            return file;
    }

    /**
     * Unzip a given ZIP file to given directory. If the ZIP contains .pevious named file read the content and returns that.
     * @param zipFile
     * @param toDirectory
     * @return
     * @throws IOException
     */
    public void unzipFile(File zipFile, File toDirectory, String pathInZip) throws IOException, MojoExecutionException {
        log.info("Unzipping: " + zipFile + " to " + toDirectory);

        toDirectory.mkdirs();

        ZipFile zf = new ZipFile(zipFile);

        Enumeration<ZipEntry> zen = (Enumeration<ZipEntry>) zf.entries();
        while (zen.hasMoreElements()) {
            ZipEntry zipEntry = zen.nextElement();

            String targetPath = zipEntry.getName();
            boolean unpackFile = true;

            if (!isPathStartWith(zipEntry.getName(), pathInZip)) {
                unpackFile = false;
            } else {
                targetPath = makePathRelative(targetPath, pathInZip);
            }

            if (unpackFile) {

                File targetFile = new File(toDirectory, targetPath);

                if (zipEntry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    log.info("Unpacking: " + zipEntry.getName() + " to " + targetPath);

                    targetFile.getParentFile().mkdirs();

                    if (targetFile.exists()) {
                        log.info("File exists, checking content");

                        File unp = File.createTempFile("check", targetFile.getName());
                        FileUtils.copyInputStreamToFile(zf.getInputStream(zipEntry), unp);

                        try {
                            if (!FileUtils.contentEquals(unp, targetFile)) {
                                throw new IOException("File exists with different content: " + targetFile);
                            }
                        } finally {
                            unp.delete();
                        }
                    } else {
                        log.info(zipEntry.getSize() + " bytes written");
                        FileUtils.copyInputStreamToFile(zf.getInputStream(zipEntry), targetFile);
                    }
                }
            }
        }
        zf.close();
    }

    boolean isPathStartWith(String path, String basePath) {
        boolean match = true;
        if (basePath != null && !basePath.equals("") && !basePath.equals("/")) {

            List<String> fullParts = ImmutableList.copyOf(Splitter.on("/").omitEmptyStrings().split(path));
            List<String> baseParts = ImmutableList.copyOf(Splitter.on("/").omitEmptyStrings().split(basePath));

            if (fullParts.size() < baseParts.size()) {
                return false;
            }

            int idx = 0;
            match = true;
            while (idx < baseParts.size() && match) {
                if (!fullParts.get(idx).equals(baseParts.get(idx))) {
                    match = false;
                }
                idx++;
            }
        }
        return match;
    }

    String makePathRelative(String path, String basePath) {
        if (!isPathStartWith(path, basePath)) {
            throw  new IllegalArgumentException("Path is not part of of: " + path + " " + basePath);
        }
        List<String> fullParts = ImmutableList.copyOf(Splitter.on("/").omitEmptyStrings().split(path));
        List<String> baseParts = ImmutableList.copyOf(Splitter.on("/").omitEmptyStrings().split(basePath));
        return Joiner.on("/").join(fullParts.subList(baseParts.size(), fullParts.size()));
    }

}
