package io.github.qishr.cascara.test.common.junit.util;

import javax.tools.*;

// import io.github.qishr.cascara.common.content.type.ContentTypeStore;
import io.github.qishr.cascara.common.util.JarFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestModuleCompiler {

    public static Map<String, byte[]> compileSources(Map<String, String> sources, List<String> dependencies, String subprojectName) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java compiler not available. Ensure JDK is used.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(diagnostics, null, null))) {

            List<JavaFileObject> compilationUnits = sources.entrySet().stream()
                    .map(e -> new StringJavaFileObject(e.getKey(), e.getValue()))
                    .map(jfo -> (JavaFileObject) jfo)
                    .toList();





            // 1. Locate the exploded test output directory containing TestService
            // URL testLocation = TestService.class.getProtectionDomain().getCodeSource().getLocation();
            // String testClassesPath = URLDecoder.decode(testLocation.getPath(), StandardCharsets.UTF_8);

            // // Clean trailing slash if present for patch-module compliance
            // if (testClassesPath.endsWith("/") || testClassesPath.endsWith("\\")) {
            //     testClassesPath = testClassesPath.substring(0, testClassesPath.length() - 1);
            // }

            List<String> options = new ArrayList<>();


            // *** MODULEPATH ***
            // options.add("--module-path");
            // options.add(modulePath);
            // options.add("--add-modules");
            // // options.add("cascara.common,cascara.common.io,cascara.test.shared");
            // options.add("cascara.common,cascara.test.shared");

            // *** CLASSPATH ***
            // // Build a classpath that includes cascara.common and test classes
            // String classPath = commonPathString + File.pathSeparator + testClassesPath;
            // options.add("--class-path");
            // options.add(classPath);


            String argModulePath = "";
            String argAddModules = "";
            for (String mavenName : dependencies) {
                Path jarPath = TestArtifactResolver.resolveJarPath(mavenName, subprojectName);
                String javaName;
                try (JarFile jarFile = JarFile.open(jarPath)) {
                    javaName = jarFile.getModuleName();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to open " + jarPath + ": " + e.getMessage());
                }
                // System.out.println("Maven name: " + mavenName);
                // System.out.println("Java name : " + javaName);

                if (!argModulePath.isEmpty()) {
                    argModulePath += File.pathSeparator;
                }
                argModulePath += jarPath.toString();

                if (!argAddModules.isEmpty()) {
                    argAddModules += ",";
                }
                argAddModules += javaName;
            }

            options.add("--module-path");
            options.add(argModulePath);
            options.add("--add-modules");
            options.add(argAddModules);


            // System.out.println("CompilationTask Options:");
            // for (String s : options) {
            //     System.out.println("  " + s);
            // }



            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits
            );

            boolean success = task.call();

            if (!success) {
                StringBuilder sb = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    sb.append(d.toString()).append("\n");
                }
                throw new IllegalArgumentException(sb.toString());
            }

            // fileManager.close();

            return fileManager.getCompiledBytes();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("File manage exception: " + e.getMessage());
        }
    }

    private static class StringJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        protected StringJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, byte[]> compiledBytes = new HashMap<>();

        protected InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        public Map<String, byte[]> getCompiledBytes() {
            return compiledBytes;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                @Override
                public OutputStream openOutputStream() {
                    return new ByteArrayOutputStream() {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            compiledBytes.put(className.replace('.', '/') + ".class", toByteArray());
                        }
                    };
                }
            };
        }
    }
}