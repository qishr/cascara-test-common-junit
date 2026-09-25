package io.github.qishr.cascara.test.common.junit.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.util.Cascara;

public class VfsTestBase {
    private static final Level SPL_REPORTING_LEVEL = Level.INFO;
    private static final Level TEST_REPORTING_LEVEL = Level.INFO;

    private ServiceProviderLayer rootLayer;
    private FileSystem vFs;

    @TempDir
    public Path tempDir;

    public Reporter reporter;
    public Reporter splReporter;
    public ServiceProviderLayer spl;

    @BeforeEach
    protected void setUp() throws IOException {
        // Points ZipFS to a writable temp file path provided by JUnit
        Path zipFile = tempDir.resolve("cascara-vfs.zip");
        URI uri = URI.create("jar:" + zipFile.toUri());

        vFs = FileSystems.newFileSystem(uri, Map.of("create", "true"));
        Path vHome = vFs.getPath("/.cascara/0.10");
        Files.createDirectories(vHome);

        Cascara.setHomePath(vHome);

        // modulepathDir = Cascara.getModulePath();
        Files.createDirectories(Cascara.getModulePath());

        // Reporter for tests
        reporter = new StandardReporter().setLevel(TEST_REPORTING_LEVEL);

        splReporter = new StandardReporter().setLevel(SPL_REPORTING_LEVEL);
        rootLayer = ServiceProviderLayer.getRoot(splReporter);

        spl = rootLayer.create("baseTestLayer");
    }

    @AfterEach
    protected void tearDown() throws IOException {
        rootLayer.remove(spl.getName());
        if (vFs != null && vFs.isOpen()) {
            vFs.close();
        }
    }

    void copyToVfsAndRegister(String mavenName, ServiceProviderLayer layer) throws IOException {
        Path testJarPath = TestArtifactResolver.resolveJarPath(mavenName);
        Path vfsTestJarPath = Cascara.getModulePath().resolve(testJarPath.getFileName().toString());
        Files.copy(testJarPath, vfsTestJarPath);
        layer.registerJar(vfsTestJarPath);
    }

}
