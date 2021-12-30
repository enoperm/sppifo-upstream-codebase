package ch.ethz.systems.netbench.core.log;

import java.io.IOException;

public interface Logger {
    void emitRow(Object... columns) throws IOException;
}
