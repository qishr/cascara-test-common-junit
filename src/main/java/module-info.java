module cascara.test.common.junit {
    requires transitive cascara.common;
    requires java.compiler;
    requires org.junit.jupiter.api;

    exports io.github.qishr.cascara.test.common.junit.util;

    opens io.github.qishr.cascara.test.common.junit.util to org.junit.platform.commons;
}
