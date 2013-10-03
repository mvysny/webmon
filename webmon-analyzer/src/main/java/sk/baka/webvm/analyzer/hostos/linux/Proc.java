/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * WebMon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * WebMon. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.hostos.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sk.baka.webvm.analyzer.utils.Constants;
import sk.baka.webvm.analyzer.utils.MiscUtils;

/**
 * Represents selected stuff from the /proc filesystem.
 *
 * @author Martin Vysny
 */
public class Proc {

    private static final Logger log = Logger.getLogger(Proc.class.getName());

    /**
     * Parses the /proc/stat file on Linux.
     */
    public static final class Stat {

        private final long user;
        private final long nice;
        private final long system;
        private final long idle;

        public Stat(long user, long nice, long system, long idle) {
            this.user = user;
            this.nice = nice;
            this.system = system;
            this.idle = idle;
        }

        public static boolean isAvailable() {
            return PROC_STAT.exists();
        }
        private static final File PROC_STAT = new File("/proc/stat");

        public static Stat now() {
            try {
                final Scanner s = new Scanner(PROC_STAT);
                try {
                    for (; s.hasNextLine();) {
                        final String name = s.next();
                        if (name.equals("cpu")) {
                            return new Stat(s.nextLong(), s.nextLong(), s.nextLong(), s.nextLong());
                        }
                        s.nextLine(); // skip this line, try the next one
                    }
                    return null;
                } finally {
                    MiscUtils.closeQuietly(s);
                }
            } catch (IOException ex) {
                log.log(Level.INFO, "Failed to parse " + PROC_STAT, ex);
            }
            return null;
        }

        public long getTotal() {
            return user + nice + system + idle;
        }

        public int getCpuUsage(Stat prev) {
            final long dtotal = getTotal() - prev.getTotal();
            if (dtotal ==0) {
                return 0;
            }
            int cpuIdle = (int) (Constants.HUNDRED_PERCENT * (idle - prev.idle) / dtotal);
            if (cpuIdle < 0) {
                cpuIdle = 0;
            }
            // To compute the CPU usage, we have to perform:
            // (idle2-idle1)*HUNDRED_PERCENT/(user2+nice2+system2+idle2-user1-nice1-system1-idle1)
            return Constants.HUNDRED_PERCENT - cpuIdle;
        }
    }

    /**
     * Parses the /proc/diskstats file.
     */
    public static final class Diskstats {

        private final static File DISKSTATS = new File("/proc/diskstats");
        public final long weightedMillisSpentIO;
        public final long currentTimeMillis;

        public Diskstats(long weightedMillisSpentIO, long currentTimeMillis) {
            this.weightedMillisSpentIO = weightedMillisSpentIO;
            this.currentTimeMillis = currentTimeMillis;
        }
        private static final int DISKSTATS_DEVNAME = 2;
        private static final int DISKSTATS_MILLIS_SPENT_IO = 12;

        public static Diskstats now() {
            long weightedMillisSpentIOTotal = 0;
            try {
                final BufferedReader in = new BufferedReader(new FileReader(DISKSTATS));
                final long currentTimeMillis = System.currentTimeMillis();
                try {
                    for (String line = in.readLine(); line != null; line = in.readLine()) {
                        final StringTokenizer t = new StringTokenizer(line);
                        final List<Object> tokens = Collections.list(t);
                        final String devname = (String) tokens.get(DISKSTATS_DEVNAME);
                        if (Character.isDigit(devname.charAt(devname.length() - 1))) {
                            // ignore sda2 etc - we are interested in sda only
                            continue;
                        }
                        final long weightedMillisSpentIO = Long.parseLong((String) tokens.get(DISKSTATS_MILLIS_SPENT_IO));
                        weightedMillisSpentIOTotal += weightedMillisSpentIO;
                    }
                } finally {
                    MiscUtils.closeQuietly(in);
                }
                return new Diskstats(weightedMillisSpentIOTotal, currentTimeMillis);
            } catch (Exception ex) {
                log.log(Level.INFO, "Failed to parse " + DISKSTATS, ex);
                return null;
            }
        }

        public int getCpuIOUsage(Diskstats prev) {
            // To compute the CPU usage, we have to perform:
            // (weightedMillisSpentIO2-weightedMillisSpentIO1)*100/(currentTimeMillis2-currentTimeMillis1)
            final long sampleTimeDelta = currentTimeMillis - prev.currentTimeMillis;
            if (sampleTimeDelta < 0) {
                throw new IllegalArgumentException("Parameter prev: invalid value " + prev + ": must be sampled earlier than this: " + this);
            }
            long cpuSpentIO = (weightedMillisSpentIO - prev.weightedMillisSpentIO) * Constants.HUNDRED_PERCENT / sampleTimeDelta / Runtime.getRuntime().availableProcessors();
            return (int) cpuSpentIO;
        }

        @Override
        public String toString() {
            return "Diskstats{" + "weightedMillisSpentIO=" + weightedMillisSpentIO + ", currentTimeMillis=" + currentTimeMillis + '}';
        }
    }

    /**
     * The contents of the Linux /proc/[pid]/stat file.
     */
    public static final class PidStat {

        public final long utimeJiffies;
        public final long stimeJiffies;
        /**
         * RSS, Resident Set Size: number of pages the process has in real
         * memory. This is just the pages which count toward text, data, or
         * stack space. This does not include pages which have not been
         * demand-loaded in, or which are swapped out.
         */
        public final int rssPages;

        /**
         * Returns the RSS field in bytes.
         *
         * @return RSS in bytes.
         */
        public long getRSSAsBytes() {
            if (MemoryLinuxStrategy.PAGE_SIZE < 0) {
                throw new IllegalStateException("Invalid state: Linux page size not available");
            }
            return (long) rssPages * MemoryLinuxStrategy.PAGE_SIZE;
        }

        public PidStat(long utimeJiffies, long stimeJiffies, int rssPages) {
            this.utimeJiffies = utimeJiffies;
            this.stimeJiffies = stimeJiffies;
            this.rssPages = rssPages;
        }

        public static PidStat now(int pid) {
            final File pidstat = new File("/proc/" + pid + "/stat");
            final String[] stat;
            try {
                final Scanner s = new Scanner(pidstat);
                try {
                    stat = s.nextLine().trim().split("\\s+");
                } finally {
                    MiscUtils.closeQuietly(s);
                }
            } catch (IOException ex) {
                log.log(Level.INFO, "Failed to parse " + pidstat, ex);
                return null;
            }
            final long utimeJiffies = Long.parseLong(stat[13]);
            final long stimeJiffies = Long.parseLong(stat[14]);
            final int rssPages = Integer.parseInt(stat[23]);
            return new PidStat(utimeJiffies, stimeJiffies, rssPages);
        }

        @Override
        public String toString() {
            return "PidStat{" + "utimeJiffies=" + utimeJiffies + ", stimeJiffies=" + stimeJiffies + ", rssPages=" + rssPages + '}';
        }
    }

    /**
     * A file in the form of NAME: VALUE. Immutable, thread-safe.
     */
    public static final class LinuxProperties {
        private final Map<String, String> properties;

        public LinuxProperties(Map<String, String> properties) {
            this.properties = new HashMap<String, String>(properties);
        }
        public static final LinuxProperties EMPTY = new LinuxProperties(Collections.<String, String>emptyMap());
        
        /**
         * Parses given file. Fails if the file is unreadable or has invalid/unparsable contents. Returns null if the file does not exist.
         * @param file the file to parse, not null.
         * @return properties, null if the file does not exist.
         * @throws RuntimeException if the parsing fails.
         */
        public static LinuxProperties parse(File file) {
            final Map<String, String> props = new HashMap<String, String>();
            try {
                final Scanner s = new Scanner(file);
                try {
                    for (; s.hasNextLine();) {
                        final String line = s.nextLine().trim();
                        if (MiscUtils.isBlank(line)) {
                            continue;
                        }
                        String name = line.split("\\s+")[0];
                        final String value = line.substring(name.length()).trim();
                        if (name.endsWith(":")) {
                            name = name.substring(0, name.length() - 1);
                        }
                        props.put(name, value);
                    }
                } finally {
                    MiscUtils.closeQuietly(s);
                }
            } catch (FileNotFoundException ex) {
                log.log(Level.CONFIG, "File " + file + " does not exist", ex);
                return null;
            }
            return new LinuxProperties(props);
        }
        
        public long getValueInBytes(String name) {
            String value = properties.get(name);
            if (value == null) {
                throw new IllegalArgumentException("Parameter name: invalid value " + name + ": not present in properties. Available properties: " + properties.keySet());
            }
            return parseValueInBytes(value);
        }
        
        private static long parseValueInBytes(String value) {
            long multiplier = 1;
            if (value.toLowerCase().endsWith("kb")) {
                multiplier = 1024;
                value = value.substring(0, value.length() - 2).trim();
            }
            return multiplier * Long.parseLong(value);
        }

        public long getValueInBytesZero(String name) {
            String value = properties.get(name);
            if (value == null) {
                return 0;
            }
            return parseValueInBytes(value);
        }

        public Long getValueInBytesNull(String name) {
            String value = properties.get(name);
            if (value == null) {
                return null;
            }
            return parseValueInBytes(value);
        }

        public boolean isEmpty() {
            return properties.isEmpty();
        }
    }
    
    /**
     * The /proc/[pid]/status file contents. Immutable, thread-safe.
     */
    public static final class PidStatus {
        public final LinuxProperties props;

        public PidStatus(LinuxProperties props) {
            this.props = Objects.requireNonNull(props);
        }

        public static PidStatus now(int pid) {
            final File f = new File("/proc/" + pid + "/status");
            return new PidStatus(LinuxProperties.parse(f));
        }

        public Long getVmSwapNull() {
            return props.getValueInBytes("VmSwap");
        }
        public Long getVmPTENull() {
            return props.getValueInBytes("VmPTE");
        }
    }
}
