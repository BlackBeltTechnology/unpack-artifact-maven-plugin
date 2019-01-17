package hu.blackbelt.maven.plugin.unpack;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnpackMojoTest {

    @InjectMocks
    UnpackMojo target;

    File testFile;
    File outDir;

    @Mock
    RepositorySystem repoSystem;

    @Mock
    org.eclipse.aether.artifact.Artifact artifactMock;

    @Before
    public void before() throws URISyntaxException {
        //target = new UnpackMojo();
        testFile = getTestFile();
        outDir = new File(testFile.getParentFile(), "out");
        deleteFolder(outDir);
        outDir.mkdirs();
    }


    @Test
    public void testExecuteMojo() throws IOException, MojoExecutionException, URISyntaxException, MojoFailureException, ArtifactResolutionException {
        target.artifacts = ImmutableList.of(
                Artifact.builder().groupId("grup1").artifactId("artifact1")
                        .resources(ImmutableList.of(
                                SourceTargetPath.builder()
                                        .source("level1/level2/level3")
                                        .destination("l3_1")
                                        .build(),
                                SourceTargetPath.builder()
                                        .source("level1/level2/level3_2")
                                        .destination("l3_2")
                                        .build()
                                )
                        )
                        .build()
        );

        target.outDirectory = outDir;

        ArtifactResult artifactResult = new ArtifactResult(new ArtifactRequest());
        artifactResult.setArtifact(artifactMock);
        when(repoSystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class))).thenReturn(artifactResult);
        when(artifactMock.getFile()).thenReturn(testFile);

        target.execute();
        Assert.assertEquals("level3_2.file", FileUtils.readFileToString(new File(outDir, "l3_2/level3_2.file"), Charsets.UTF_8).trim());
    }

    @Test
    public void testUnzipFileLevel1() throws IOException, MojoExecutionException, URISyntaxException {
        target.unzipFile(testFile, outDir, "level1");
        Assert.assertEquals("level1.file", FileUtils.readFileToString(new File(outDir, "level1.file"), Charsets.UTF_8).trim());
    }

    @Test
    public void testUnzipFileRoot() throws IOException, MojoExecutionException, URISyntaxException {
        target.unzipFile(testFile, outDir, "");
        Assert.assertEquals("level1.file", FileUtils.readFileToString(new File(outDir, "level1/level1.file"), Charsets.UTF_8).trim());
    }

    @Test
    public void testExtractSameContent() throws IOException, MojoExecutionException, URISyntaxException {
        target.unzipFile(testFile, outDir, "");
        target.unzipFile(testFile, outDir, "");
        Assert.assertEquals("level1.file", FileUtils.readFileToString(new File(outDir, "level1/level1.file"), Charsets.UTF_8).trim());
    }

    @Test(expected = IOException.class)
    public void testExtractDifferentContent() throws IOException, MojoExecutionException, URISyntaxException {
        target.unzipFile(testFile, outDir, "");
        FileUtils.write(new File(outDir, "level1/level1.file"), "DIFF", Charsets.UTF_8);
        target.unzipFile(testFile, outDir, "");
    }

    private File getTestFile() throws URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource("testfile/test.zip").getFile());
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

}