package io.github.qishr.cascara.test.common.junit.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class TestModulePackager {

    public static void createSyntheticModuleJar(Path destinationJarPath, String minCascaraVersion, Map<String, String> sources, List<String> dependencies, String subprojectName) throws IOException {
        // 1. Compile source code in memory
        Map<String, byte[]> classOutputs = TestModuleCompiler.compileSources(
            sources, dependencies, subprojectName
        );

        // 2. Build manifest attributes
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (minCascaraVersion != null) {
            manifest.getMainAttributes().putValue("Min-Cascara-Version", minCascaraVersion);
        }

        // 3. Stream compiled entries directly into the ZipFS destination path
        try (OutputStream os = Files.newOutputStream(destinationJarPath);
             JarOutputStream jos = new JarOutputStream(os, manifest)) {

            for (Map.Entry<String, byte[]> entry : classOutputs.entrySet()) {
                JarEntry jarEntry = new JarEntry(entry.getKey());
                jos.putNextEntry(jarEntry);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }
}