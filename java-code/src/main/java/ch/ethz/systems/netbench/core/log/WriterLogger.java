package ch.ethz.systems.netbench.core.log;

import ch.ethz.systems.netbench.core.log.Logger;

import java.io.Writer;
import java.io.IOException;

public class WriterLogger implements Logger {
    private Writer output;

    public WriterLogger(Writer output) throws NullPointerException {
        if(output == null) throw new NullPointerException();
        this.output = output;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        output.close();
    }

    // everything is an "Object", except when not,
    // so it is impossible to pass primitive (read: non-nullable) types to varargs functions.
    public void emitRow(Object... columns) throws IOException, NullPointerException {
        String subsequentSeparator = ",";
        String separator = "";

        for(Object col: columns) {
            if(col == null) throw new NullPointerException();

            this.output.write(separator + col);
            separator = subsequentSeparator;
        }
        this.output.write('\n');
    }
}
