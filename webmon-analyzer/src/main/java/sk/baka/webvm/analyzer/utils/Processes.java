/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * Provides means of running a child process and capturing its stdout.
 * @author Martin Vysny
 */
public class Processes {
    /**
     * Runs given process and waits until the process terminates. STDERR and STDOUT are joined together and returned as a string buffer, along with the exit code.
     * @param path current working directory for the process, may be null.
     * @param args the command and its parameters
     * @return non-null result of the execution. Do not forget to call {@link Result#checkSuccess()}.
     * @throws IOException
     * @throws InterruptedException
     */
    public static Result executeAndWait(final String path, final String... args) throws IOException, InterruptedException {
        return executeAndWaitBuffered(new ByteArrayBuffer(), path, args);
    }

    /**
     * Runs given process and waits until the process terminates. STDERR and STDOUT are joined together and returned, along with the exit code.
     * @param buffer holds contents of STDERR and STDOUT.
     * @param path current working directory for the process, may be null.
     * @param args the command and its parameters
     * @return non-null result of the execution.
     * @throws IOException
     * @throws InterruptedException
     */
    public static Result executeAndWaitBuffered(final AbstractBuffer buffer, final String path, final String... args) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder();
        if (path != null) {
            pb.directory(new File(path));
        }
        final Process p = pb.command(args).redirectErrorStream(true).start();
        final Thread reader = new InputStreamPrinter(p.getInputStream(), buffer);
        reader.start();
        final int exitCode = p.waitFor();
//        reader.interrupt();  // do not interrupt or we may never see entire STDOUT/STDERR
        reader.join();
        return new Result(path, args, buffer, exitCode);
    }

    private static class InputStreamPrinter extends Thread {

        private final InputStream in;
        private final AbstractBuffer b;

        public InputStreamPrinter(InputStream in, AbstractBuffer b) {
            this.in = in;
            this.b = b;
        }

        @Override
        public void run() {
            try {
                final byte[] buffer = new byte[1024];
                final OutputStream writer = b.newWriter();
                while (!Thread.currentThread().isInterrupted()) {
                    final int read = in.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    writer.write(buffer, 0, read);
                }
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "InputStream drainer exited forcefully", ex);
            }
        }
    }
    private static final Logger log = Logger.getLogger(Processes.class.getName());
    /**
     * The result of the process execution.
     */
    public static final class Result {

        public final String path;
        public final String[] args;
        public final AbstractBuffer buffer;

        /**
         * Contains both stdout and stderr.
         * @return the process output as a UTF-8-encoded string.
         */
        public String getOutput() {
            try {
                return buffer.asString();
            } catch (IOException ex) {
                return "Failed to obtain stdout: " + MiscUtils.getStacktrace(ex);
            }
        }
        /**
         * The process exit code, generally 0 means success.
         */
        public final int exitCode;

        public Result(String path, String[] args, AbstractBuffer buffer, int exitCode) {
            this.buffer = buffer;
            this.exitCode = exitCode;
            this.path = path;
            this.args = args;
        }

        /**
         * Checks if the exit code was zero.
         * @return true if the exit code was zero, false otherwise.
         */
        public boolean isSuccessfull() {
            return isSuccessfull(null);
        }

        /**
         * Checks if the exit code was zero.
         * @param allowedExitCodes allowed process exit codes. exit code 0 is always allowed. May be null or empty.
         * @return true if the exit code was zero, false otherwise.
         */
        public boolean isSuccessfull(int... allowedExitCodes) {
            if (allowedExitCodes == null || allowedExitCodes.length == 0) {
                return exitCode == 0;
            }
            return exitCode == 0 || contains(allowedExitCodes, exitCode);
        }
        
        private static boolean contains(int[] array, int item) {
            for (int i : array) {
                if (i == item) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks the exit code and throws {@link RuntimeException} with all data if the exit code is not zero.
         * @return this
         */
        public Result checkSuccess() {
            return checkSuccess(null);
        }

        /**
         * Checks the exit code and throws {@link RuntimeException} with all data if the exit code is not zero.
         * @param allowedExitCodes allowed process exit codes. exit code 0 is always allowed. May be null or empty.
         * @return this
         */
        public Result checkSuccess(int... allowedExitCodes) {
            if (!isSuccessfull()) {
                String msg = "Failed to run " + Arrays.toString(args);
                if (path != null) {
                    msg += " in " + path;
                }
                msg += ": ErrorCode " + exitCode + ": " + getOutput();
                throw new RuntimeException(msg);
            }
            return this;
        }

        @Override
        public String toString() {
            return "Invocation: in '" + path + "': " + Arrays.toString(args) + " ended with exit code " + exitCode;
        }
    }

    /**
     * An abstract buffer.
     */
    public static abstract class AbstractBuffer {

        /**
         * Provides means of writing data to the buffer. Writing by multiple writers at once is unsupported and may result in weird results.
         * @return opened output stream.
         * @throws IOException on I/O error.
         */
        public abstract OutputStream newWriter() throws IOException;

        /**
         * Provides means of reading the data stored in the buffer. IOException should be thrown if no data was written yet to the buffer.
         * @return input stream, never null.
         * @throws IOException on I/O error.
         */
        public abstract InputStream newInputStream() throws IOException;

        /**
         * Returns entire buffer contents as a string. Expects UTF-8 encoding.
         * @return buffer contents.
         * @throws IOException on I/O error.
         */
        public String asString() throws IOException {
            return asString("UTF-8");
        }
        /**
         * Returns entire buffer contents as a string.
         * @param charset the expected character set encoding.
         * @return buffer contents.
         * @throws IOException on I/O error.
         */
        public String asString(String charset) throws IOException {
            final InputStreamReader r = new InputStreamReader(newInputStream(), charset);
            try {
                return IOUtils.toString(r);
            } finally {
                MiscUtils.closeQuietly(r);
            }
        }
    }

    /**
     * Represents a buffer stored in a file.
     */
    public static class FileBuffer extends AbstractBuffer {

        private final File redirect;

        public FileBuffer(File redirect) {
            this.redirect = redirect;
        }

        public InputStream newInputStream() throws IOException {
            return new FileInputStream(redirect);
        }

        @Override
        public OutputStream newWriter() throws IOException {
            return new FileOutputStream(redirect);
        }
    }

    /**
     * Represents a buffer stored in a byte array.
     */
    public static class ByteArrayBuffer extends AbstractBuffer {

        private volatile ByteArrayOutputStream buffer = null;

        public InputStream newInputStream() throws IOException {
            return new ByteArrayInputStream(buffer.toByteArray());
        }

        @Override
        public OutputStream newWriter() throws IOException {
            buffer = new ByteArrayOutputStream();
            return buffer;
        }
    }
}
