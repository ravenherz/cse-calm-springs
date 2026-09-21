package com.ravenherz.cse.engine.io;

import com.ravenherz.cse.util.io.CseDisk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InstanceProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceProbe.class);
    private static final long GPU_CACHE_MS = 60_000L;
    private static final long CMD_TIMEOUT_MS = 2_000L;
    static final int NVIDIA_ATTEMPT_LIMIT = 5;
    private static final Set<String> SKIP_FS_TYPES = Set.of(
            "tmpfs", "devtmpfs", "devfs", "ramfs", "overlay", "squashfs", "aufs",
            "proc", "sysfs", "cgroup", "cgroup2", "autofs", "debugfs", "tracefs",
            "securityfs", "pstore", "bpf", "mqueue", "hugetlbfs", "configfs",
            "efivarfs", "binfmt_misc", "nsfs", "rpc_pipefs", "fusectl",
            "fuse.portal", "fuse.gvfsd-fuse", "iso9660", "udf");

    private static final Path[] OS_RELEASE_PATHS = {
            Path.of("/etc/os-release"),
            Path.of("/usr/lib/os-release")
    };
    private static final Path PROC_MEMINFO = Path.of("/proc/meminfo");
    private static final Path PROC_STAT = Path.of("/proc/stat");
    private static final long CPU_SAMPLE_STALE_MS = 15_000L;
    private static final long CPU_SAMPLE_SLEEP_MS = 100L;

    private volatile String cachedCpuName;
    private volatile String cachedOsLabel;
    private volatile long gpuCachedAt;
    private volatile List<Gpu> cachedGpus = List.of();
    private volatile boolean nvidiaLive;
    private final AtomicInteger nvidiaFailures = new AtomicInteger();
    private final Object cpuSampleLock = new Object();
    private CpuTimes prevHostCpu;
    private long prevHostSampleAt;
    private long prevProcessCpuNanos = -1;
    private long prevProcessWallNanos = -1;
    private long prevProcessSampleAt;

    public InstanceProbe() {
        sunOs();
    }

    int nvidiaFailureCount() {
        return nvidiaFailures.get();
    }

    public Snapshot capture() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sun = sunOs();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        int cores = Math.max(1, os.getAvailableProcessors());
        double beanSystem = sun == null ? os.getSystemLoadAverage() : sun.getCpuLoad();
        double beanProcess = sun == null ? -1 : sun.getProcessCpuLoad();
        double systemLoad = hostCpuLoad(beanSystem);
        double processLoad = processCpuLoad(sun, cores, beanProcess);
        double loadAverage = os.getSystemLoadAverage();
        PhysicalMemory physical = physicalMemory(
                readProcMeminfo(),
                sun == null ? 0 : sun.getTotalMemorySize(),
                sun == null ? 0 : sun.getFreeMemorySize());
        long physicalTotal = physical.total();
        long physicalFree = physical.free();
        long physicalUsed = physical.used();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        if (heapMax < 0) {
            heapMax = memory.getHeapMemoryUsage().getCommitted();
        }
        Path instancePath = CseDisk.cseRoot().toPath().toAbsolutePath();
        return new Snapshot(
                System.currentTimeMillis(),
                new Host(
                        hostName(),
                        osLabel(os),
                        os.getArch(),
                        CseDisk.instanceSlug(),
                        instancePath.toString(),
                        System.getProperty("java.version", ""),
                        formatDuration(runtime.getUptime())),
                new Cpu(
                        cpuName(os, cores),
                        cores,
                        ratio(systemLoad),
                        ratio(processLoad),
                        loadAverage < 0 ? null : loadAverage,
                        percent(systemLoad),
                        percent(processLoad)),
                new Memory(
                        physicalTotal,
                        physicalUsed,
                        physicalFree,
                        bytesLabel(physicalTotal),
                        bytesLabel(physicalUsed),
                        bytesLabel(physicalFree),
                        percentOf(physicalUsed, physicalTotal),
                        heapUsed,
                        heapMax,
                        bytesLabel(heapUsed),
                        bytesLabel(heapMax),
                        percentOf(heapUsed, heapMax)),
                disks(instancePath),
                gpus());
    }

    private double hostCpuLoad(double fallback) {
        CpuTimes first = readProcStat();
        if (first == null) {
            return fallback;
        }
        long at = System.currentTimeMillis();
        CpuTimes prev;
        long prevAt;
        synchronized (cpuSampleLock) {
            prev = prevHostCpu;
            prevAt = prevHostSampleAt;
        }
        if (prev != null && at - prevAt <= CPU_SAMPLE_STALE_MS) {
            double usage = cpuUsage(prev, first);
            synchronized (cpuSampleLock) {
                prevHostCpu = first;
                prevHostSampleAt = at;
            }
            return usage >= 0 ? usage : fallback;
        }
        try {
            Thread.sleep(CPU_SAMPLE_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (cpuSampleLock) {
                prevHostCpu = first;
                prevHostSampleAt = at;
            }
            return fallback;
        }
        CpuTimes second = readProcStat();
        synchronized (cpuSampleLock) {
            prevHostCpu = second == null ? first : second;
            prevHostSampleAt = System.currentTimeMillis();
        }
        double usage = cpuUsage(first, second);
        return usage >= 0 ? usage : fallback;
    }

    private double processCpuLoad(com.sun.management.OperatingSystemMXBean sun, int cores, double fallback) {
        long cpuNanos = sun == null ? -1 : sun.getProcessCpuTime();
        long wall = System.nanoTime();
        long at = System.currentTimeMillis();
        long prevCpu;
        long prevWall;
        long prevAt;
        synchronized (cpuSampleLock) {
            prevCpu = prevProcessCpuNanos;
            prevWall = prevProcessWallNanos;
            prevAt = prevProcessSampleAt;
            prevProcessCpuNanos = cpuNanos;
            prevProcessWallNanos = wall;
            prevProcessSampleAt = at;
        }
        if (prevCpu >= 0 && prevWall > 0 && at - prevAt <= CPU_SAMPLE_STALE_MS) {
            double sampled = processCoreLoad(cpuNanos - prevCpu, wall - prevWall);
            if (sampled >= 0) {
                return meterRatio(sampled);
            }
        }
        if (fallback < 0) {
            return fallback;
        }
        return meterRatio(fallback * Math.max(1, cores));
    }

    private static CpuTimes readProcStat() {
        try {
            if (!Files.isRegularFile(PROC_STAT)) {
                return null;
            }
            return parseProcStat(Files.readString(PROC_STAT));
        } catch (Exception ignored) {
            return null;
        }
    }

    static CpuTimes parseProcStat(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (!line.startsWith("cpu ") && !line.startsWith("cpu\t")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 5) {
                return null;
            }
            long user = parseLongValue(parts[1]);
            long nice = parseLongValue(parts[2]);
            long system = parseLongValue(parts[3]);
            long idle = parseLongValue(parts[4]);
            long iowait = parts.length > 5 ? Math.max(0, parseLongValue(parts[5])) : 0;
            long irq = parts.length > 6 ? Math.max(0, parseLongValue(parts[6])) : 0;
            long softirq = parts.length > 7 ? Math.max(0, parseLongValue(parts[7])) : 0;
            long steal = parts.length > 8 ? Math.max(0, parseLongValue(parts[8])) : 0;
            if (user < 0 || nice < 0 || system < 0 || idle < 0) {
                return null;
            }
            long total = user + nice + system + idle + iowait + irq + softirq + steal;
            return new CpuTimes(idle, total);
        }
        return null;
    }

    static double cpuUsage(CpuTimes prev, CpuTimes now) {
        if (prev == null || now == null) {
            return -1;
        }
        long deltaTotal = now.total() - prev.total();
        long deltaIdle = now.idle() - prev.idle();
        if (deltaTotal <= 0) {
            return -1;
        }
        if (deltaIdle < 0) {
            deltaIdle = 0;
        }
        if (deltaIdle > deltaTotal) {
            deltaIdle = deltaTotal;
        }
        return 1.0 - (deltaIdle / (double) deltaTotal);
    }

    static double processCoreLoad(long deltaCpuNanos, long deltaWallNanos) {
        if (deltaCpuNanos < 0 || deltaWallNanos <= 0) {
            return -1;
        }
        return deltaCpuNanos / (double) deltaWallNanos;
    }

    static double meterRatio(double coreLoad) {
        if (coreLoad < 0 || Double.isNaN(coreLoad)) {
            return -1;
        }
        return Math.min(1.0, coreLoad);
    }

    static PhysicalMemory physicalMemory(String meminfo, long fallbackTotal, long fallbackFree) {
        PhysicalMemory parsed = parseMeminfo(meminfo);
        if (parsed != null) {
            return parsed;
        }
        long total = Math.max(0, fallbackTotal);
        long free = Math.max(0, fallbackFree);
        long used = total > 0 ? Math.max(0, total - free) : 0;
        return new PhysicalMemory(total, used, free);
    }

    private static String readProcMeminfo() {
        try {
            if (!Files.isRegularFile(PROC_MEMINFO)) {
                return "";
            }
            return Files.readString(PROC_MEMINFO);
        } catch (Exception ignored) {
            return "";
        }
    }

    static PhysicalMemory parseMeminfo(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        long totalKb = -1;
        long availableKb = -1;
        long freeKb = -1;
        long buffersKb = 0;
        long cachedKb = 0;
        long reclaimableKb = 0;
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            long kb = parseMeminfoKb(line.substring(colon + 1));
            if (kb < 0) {
                continue;
            }
            switch (key) {
                case "MemTotal" -> totalKb = kb;
                case "MemAvailable" -> availableKb = kb;
                case "MemFree" -> freeKb = kb;
                case "Buffers" -> buffersKb = kb;
                case "Cached" -> cachedKb = kb;
                case "SReclaimable" -> reclaimableKb = kb;
                default -> {
                }
            }
        }
        if (totalKb <= 0) {
            return null;
        }
        long availKb = availableKb;
        if (availKb < 0) {
            if (freeKb < 0) {
                return null;
            }
            availKb = freeKb + buffersKb + cachedKb + reclaimableKb;
        }
        long total = totalKb * 1024L;
        long free = Math.max(0, availKb * 1024L);
        return new PhysicalMemory(total, Math.max(0, total - free), free);
    }

    private static long parseMeminfoKb(String value) {
        if (value == null) {
            return -1;
        }
        String trimmed = value.trim();
        int end = 0;
        while (end < trimmed.length() && Character.isDigit(trimmed.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        try {
            return Long.parseLong(trimmed.substring(0, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<Disk> disks(Path instancePath) {
        FileStore instanceStore = null;
        try {
            instanceStore = Files.getFileStore(instancePath);
        } catch (Exception ignored) {
        }
        List<VolumeDraft> drafts = new ArrayList<>();
        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            try {
                long total = store.getTotalSpace();
                if (total <= 0) {
                    continue;
                }
                long usable = store.getUsableSpace();
                long unallocated = unallocatedSpace(store, usable);
                long used = allocatedBytes(total, unallocated, usable);
                long free = Math.max(0, usable);
                drafts.add(new VolumeDraft(
                        storeName(store),
                        store.name() == null ? "" : store.name().trim(),
                        store.type() == null ? "" : store.type(),
                        total,
                        used,
                        free,
                        instanceStore != null && store.equals(instanceStore)));
            } catch (IOException ignored) {
            }
        }
        return compactDisks(drafts);
    }

    private List<Gpu> gpus() {
        List<Gpu> nvidia = readNvidia();
        if (!nvidia.isEmpty()) {
            nvidiaLive = true;
            cachedGpus = nvidia;
            gpuCachedAt = System.currentTimeMillis();
            return nvidia;
        }
        long now = System.currentTimeMillis();
        if (nvidiaLive && now - gpuCachedAt < GPU_CACHE_MS && !cachedGpus.isEmpty()) {
            return cachedGpus;
        }
        nvidiaLive = false;
        if (now - gpuCachedAt < GPU_CACHE_MS && !cachedGpus.isEmpty()) {
            return cachedGpus;
        }
        List<Gpu> fallback = readOsGpus();
        cachedGpus = fallback;
        gpuCachedAt = now;
        return fallback;
    }

    private List<Gpu> readNvidia() {
        if (nvidiaFailures.get() >= NVIDIA_ATTEMPT_LIMIT) {
            return List.of();
        }
        String raw = exec("nvidia-smi",
                "--query-gpu=name,memory.total,memory.used,utilization.gpu,driver_version",
                "--format=csv,noheader,nounits");
        if (raw.isBlank() || raw.toLowerCase(Locale.ROOT).contains("not found")
                || raw.toLowerCase(Locale.ROOT).contains("not supported")) {
            int fails = nvidiaFailures.incrementAndGet();
            if (fails == NVIDIA_ATTEMPT_LIMIT) {
                LOGGER.info("nvidia-smi unavailable after {} attempts; GPU list will use the OS inventory",
                        NVIDIA_ATTEMPT_LIMIT);
            }
            return List.of();
        }
        nvidiaFailures.set(0);
        List<Gpu> gpus = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String[] parts = line.split(",");
            if (parts.length < 2) {
                continue;
            }
            String name = parts[0].trim();
            if (name.isEmpty()) {
                continue;
            }
            long totalMib = parseLong(parts, 1);
            long usedMib = parseLong(parts, 2);
            int util = (int) parseLong(parts, 3);
            String driver = parts.length > 4 ? parts[4].trim() : "";
            long total = totalMib > 0 ? totalMib * 1024L * 1024L : 0;
            long used = usedMib > 0 ? usedMib * 1024L * 1024L : 0;
            gpus.add(new Gpu(
                    name,
                    total,
                    used,
                    bytesLabel(total),
                    bytesLabel(used),
                    percentOf(used, total),
                    util >= 0 ? util : null,
                    driver.isEmpty() ? null : driver,
                    true));
        }
        return List.copyOf(gpus);
    }

    private List<Gpu> readOsGpus() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return readWindowsGpus();
        }
        if (os.contains("mac")) {
            return readMacGpus();
        }
        return readLinuxGpus();
    }

    private List<Gpu> readWindowsGpus() {
        String raw = exec("powershell", "-NoProfile", "-Command",
                "Get-CimInstance Win32_VideoController | Select-Object Name, AdapterRAM, DriverVersion | ConvertTo-Csv -NoTypeInformation");
        if (raw.isBlank()) {
            return List.of();
        }
        List<Gpu> gpus = new ArrayList<>();
        String[] lines = raw.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            List<String> cols = csv(lines[i]);
            if (cols.isEmpty() || cols.get(0).isBlank()) {
                continue;
            }
            long ram = cols.size() > 1 ? parseLongValue(cols.get(1)) : 0;
            String driver = cols.size() > 2 ? cols.get(2).trim() : "";
            gpus.add(new Gpu(
                    cols.get(0).trim(),
                    ram,
                    0,
                    ram > 0 ? bytesLabel(ram) : null,
                    null,
                    ram > 0 ? 0 : null,
                    null,
                    driver.isEmpty() ? null : driver,
                    false));
        }
        return List.copyOf(gpus);
    }

    private List<Gpu> readLinuxGpus() {
        String raw = exec("lspci", "-mm");
        if (raw.isBlank()) {
            return List.of();
        }
        List<Gpu> gpus = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (!lower.contains("vga") && !lower.contains("3d") && !lower.contains("display")) {
                continue;
            }
            String name = lspciName(line);
            if (name.isBlank()) {
                continue;
            }
            gpus.add(new Gpu(name, 0, 0, null, null, null, null, null, false));
        }
        return List.copyOf(gpus);
    }

    private List<Gpu> readMacGpus() {
        String raw = exec("system_profiler", "SPDisplaysDataType", "-detailLevel", "mini");
        if (raw.isBlank()) {
            return List.of();
        }
        List<Gpu> gpus = new ArrayList<>();
        String current = null;
        String vram = null;
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.endsWith(":") && line.startsWith("        ") && !line.startsWith("         ")) {
                if (current != null) {
                    gpus.add(macGpu(current, vram));
                }
                current = trimmed.substring(0, trimmed.length() - 1).trim();
                vram = null;
            } else if (trimmed.startsWith("VRAM") && trimmed.contains(":")) {
                vram = trimmed.substring(trimmed.indexOf(':') + 1).trim();
            }
        }
        if (current != null) {
            gpus.add(macGpu(current, vram));
        }
        return List.copyOf(gpus);
    }

    private static Gpu macGpu(String name, String vram) {
        long bytes = parseSizeLabel(vram);
        return new Gpu(name, bytes, 0, bytes > 0 ? bytesLabel(bytes) : vram,
                null, null, null, null, false);
    }

    private String cpuName(OperatingSystemMXBean os, int cores) {
        String cached = cachedCpuName;
        if (cached != null) {
            return cached;
        }
        String name = readCpuName();
        if (name == null || name.isBlank()) {
            name = os.getArch() + " · " + cores + (cores == 1 ? " thread" : " threads");
        }
        cachedCpuName = name;
        return name;
    }

    private String readCpuName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            String fromProc = cpuinfoModel();
            if (!fromProc.isBlank()) {
                return fromProc;
            }
        }
        if (os.contains("mac")) {
            String brand = exec("sysctl", "-n", "machdep.cpu.brand_string");
            if (!brand.isBlank()) {
                return brand;
            }
        }
        String ident = System.getenv("PROCESSOR_IDENTIFIER");
        return ident == null ? "" : ident.trim();
    }

    private static String cpuinfoModel() {
        try {
            for (String line : Files.readAllLines(Path.of("/proc/cpuinfo"))) {
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                if ("model name".equals(key) || "hardware".equals(key) || "cpu model".equals(key)) {
                    String value = line.substring(colon + 1).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String osLabel(OperatingSystemMXBean os) {
        if (cachedOsLabel != null) {
            return cachedOsLabel;
        }
        String name = os.getName() == null ? "" : os.getName().trim();
        String version = os.getVersion() == null ? "" : os.getVersion().trim();
        String kernel = (name + " " + version).trim();
        if (name.toLowerCase(Locale.ROOT).contains("linux")) {
            String pretty = readOsReleasePretty();
            if (!pretty.isBlank()) {
                cachedOsLabel = joinOsAndKernel(pretty, version);
                return cachedOsLabel;
            }
        }
        cachedOsLabel = kernel;
        return cachedOsLabel;
    }

    private static String readOsReleasePretty() {
        for (Path path : OS_RELEASE_PATHS) {
            try {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String pretty = prettyOsFromRelease(Files.readString(path));
                if (!pretty.isBlank()) {
                    return pretty;
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    static String joinOsAndKernel(String pretty, String version) {
        if (pretty == null || pretty.isBlank()) {
            return "";
        }
        if (version == null || version.isBlank() || pretty.contains(version)) {
            return pretty;
        }
        return pretty + " (" + version + ")";
    }

    static String prettyOsFromRelease(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String pretty = "";
        String name = "";
        String versionId = "";
        String version = "";
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = unquoteOsRelease(line.substring(eq + 1).trim());
            switch (key) {
                case "PRETTY_NAME" -> pretty = value;
                case "NAME" -> name = value;
                case "VERSION_ID" -> versionId = value;
                case "VERSION" -> version = value;
                default -> {
                }
            }
        }
        if (!pretty.isBlank()) {
            return pretty;
        }
        if (!name.isBlank() && !versionId.isBlank()) {
            return name + " " + versionId;
        }
        if (!name.isBlank() && !version.isBlank()) {
            return name + " " + version;
        }
        return name;
    }

    static String unquoteOsRelease(String value) {
        if (value == null || value.length() < 2) {
            return value == null ? "" : value;
        }
        char quote = value.charAt(0);
        if ((quote == '"' || quote == '\'') && value.charAt(value.length() - 1) == quote) {
            return value.substring(1, value.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\");
        }
        return value;
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String storeName(FileStore store) {
        String name = store.toString();
        if (name == null || name.isBlank()) {
            return store.name();
        }
        return name;
    }

    static List<Disk> compactDisks(List<VolumeDraft> drafts) {
        Map<String, VolumeDraft> byKey = new LinkedHashMap<>();
        for (VolumeDraft draft : drafts) {
            if (draft == null || draft.total <= 0 || skipVolume(draft.type, draft.name, draft.device)) {
                continue;
            }
            String device = volumeDevice(draft.name, draft.device);
            String key = volumeKey(device, draft.type, draft.total, draft.free);
            VolumeDraft existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, new VolumeDraft(draft.name, device, draft.type,
                        draft.total, draft.used, draft.free, draft.instance));
                continue;
            }
            byKey.put(key, new VolumeDraft(
                    preferVolumeName(existing.name, draft.name),
                    existing.device,
                    existing.type,
                    existing.total,
                    existing.used,
                    existing.free,
                    existing.instance || draft.instance));
        }
        List<Disk> disks = new ArrayList<>();
        for (VolumeDraft draft : byKey.values()) {
            disks.add(new Disk(
                    draft.name,
                    draft.total,
                    draft.used,
                    draft.free,
                    bytesLabel(draft.total),
                    bytesLabel(draft.used),
                    bytesLabel(draft.free),
                    percentOf(draft.used, draft.total),
                    draft.instance));
        }
        disks.sort(Comparator
                .comparing((Disk disk) -> !disk.instance())
                .thenComparing(Disk::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(disks);
    }

    static long allocatedBytes(long total, long unallocated, long usable) {
        long freeBlocks = usable;
        if (unallocated > freeBlocks) {
            freeBlocks = unallocated;
        }
        if (freeBlocks < 0) {
            freeBlocks = 0;
        }
        return Math.max(0, total - freeBlocks);
    }

    private static long unallocatedSpace(FileStore store, long usable) {
        try {
            return store.getUnallocatedSpace();
        } catch (IOException ignored) {
            return usable;
        }
    }

    static boolean skipVolume(String type, String displayName, String device) {
        String fs = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (SKIP_FS_TYPES.contains(fs)) {
            return true;
        }
        String mount = mountPoint(displayName).toLowerCase(Locale.ROOT).replace('\\', '/');
        String dev = device == null ? "" : device.toLowerCase(Locale.ROOT);
        if (mount.equals("/dev") || mount.startsWith("/dev/")
                || mount.equals("/run") || mount.startsWith("/run/")
                || mount.startsWith("/proc") || mount.startsWith("/sys")
                || mount.startsWith("/snap/") || mount.startsWith("/var/lib/snapd/")) {
            return true;
        }
        return dev.startsWith("/dev/loop");
    }

    static String volumeDevice(String displayName, String storeDevice) {
        if (storeDevice != null && !storeDevice.isBlank() && isPhysicalDevice(storeDevice)) {
            return storeDevice.trim();
        }
        String name = displayName == null ? "" : displayName.trim();
        int paren = name.lastIndexOf(" (");
        if (paren > 0 && name.endsWith(")")) {
            String inside = name.substring(paren + 2, name.length() - 1).trim();
            if (isPhysicalDevice(inside)) {
                return inside;
            }
        }
        return storeDevice == null ? "" : storeDevice.trim();
    }

    static String volumeKey(String device, String type, long total, long free) {
        if (isPhysicalDevice(device)) {
            return "dev:" + device;
        }
        return "anon:" + (type == null ? "" : type) + "|" + total + "|" + free;
    }

    static boolean isPhysicalDevice(String device) {
        if (device == null || device.isBlank()) {
            return false;
        }
        String value = device.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/dev/loop")) {
            return false;
        }
        if (lower.startsWith("/dev/")) {
            return true;
        }
        if (value.length() >= 2 && Character.isLetter(value.charAt(0)) && value.charAt(1) == ':') {
            return true;
        }
        return value.contains(":") && !lower.equals("udev") && !lower.equals("tmpfs");
    }

    static String preferVolumeName(String current, String candidate) {
        if (current == null || current.isBlank()) {
            return candidate;
        }
        if (candidate == null || candidate.isBlank()) {
            return current;
        }
        int currentRank = volumeRank(current);
        int candidateRank = volumeRank(candidate);
        if (candidateRank < currentRank) {
            return candidate;
        }
        if (candidateRank == currentRank && candidate.length() < current.length()) {
            return candidate;
        }
        return current;
    }

    static int volumeRank(String name) {
        String mount = mountPoint(name);
        if ("/".equals(mount) || mount.matches("[A-Za-z]:\\\\?")) {
            return 0;
        }
        return Math.max(1, mount.length());
    }

    static String mountPoint(String name) {
        if (name == null) {
            return "";
        }
        int paren = name.lastIndexOf(" (");
        if (paren > 0 && name.endsWith(")")) {
            return name.substring(0, paren);
        }
        return name;
    }

    record VolumeDraft(String name, String device, String type, long total, long used, long free, boolean instance) {
    }

    record PhysicalMemory(long total, long used, long free) {
    }

    record CpuTimes(long idle, long total) {
    }

    private static com.sun.management.OperatingSystemMXBean sunOs() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        return os instanceof com.sun.management.OperatingSystemMXBean sun ? sun : null;
    }

    static String exec(String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Thread reader = new Thread(() -> {
                try {
                    process.getInputStream().transferTo(buffer);
                } catch (IOException ignored) {
                }
            });
            reader.setDaemon(true);
            reader.start();
            if (!process.waitFor(CMD_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return "";
            }
            reader.join(200);
            return buffer.toString(StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    static String bytesLabel(long bytes) {
        if (bytes < 0) {
            return "—";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        String formatted = value >= 10
                ? String.format(Locale.ROOT, "%.1f", value)
                : String.format(Locale.ROOT, "%.2f", value);
        int dot = formatted.indexOf('.');
        if (dot >= 0) {
            while (formatted.endsWith("0")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
            if (formatted.endsWith(".")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
        }
        return formatted + " " + units[unit];
    }

    static String formatDuration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long days = seconds / 86_400;
        long hours = (seconds % 86_400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    static Integer percentOf(long used, long total) {
        if (total <= 0) {
            return null;
        }
        return (int) Math.round(Math.min(100, Math.max(0, (used * 100.0) / total)));
    }

    private static Double ratio(double value) {
        return value < 0 || Double.isNaN(value) ? null : Math.min(1, Math.max(0, value));
    }

    private static Integer percent(double value) {
        Double ratio = ratio(value);
        return ratio == null ? null : (int) Math.round(ratio * 100);
    }

    private static long parseLong(String[] parts, int index) {
        if (index >= parts.length) {
            return -1;
        }
        return parseLongValue(parts[index]);
    }

    private static long parseLongValue(String raw) {
        if (raw == null) {
            return -1;
        }
        String digits = raw.replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return (long) Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static long parseSizeLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        long parsed = parseLongValue(raw);
        if (parsed < 0) {
            return 0;
        }
        if (lower.contains("tb")) {
            return parsed * 1024L * 1024L * 1024L * 1024L;
        }
        if (lower.contains("gb")) {
            return parsed * 1024L * 1024L * 1024L;
        }
        if (lower.contains("mb")) {
            return parsed * 1024L * 1024L;
        }
        return parsed;
    }

    static String lspciName(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
                continue;
            }
            if (ch == ' ' && !quoted) {
                if (current.length() > 0) {
                    cols.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            cols.add(current.toString());
        }
        if (cols.size() >= 4) {
            return (cols.get(2) + " " + cols.get(3)).trim();
        }
        return line.trim();
    }

    static List<String> csv(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                    continue;
                }
                quoted = !quoted;
                continue;
            }
            if (ch == ',' && !quoted) {
                cols.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        cols.add(current.toString());
        return cols;
    }

    public record Snapshot(long sampledAt, Host host, Cpu cpu, Memory memory, List<Disk> disks, List<Gpu> gpus) {
    }

    public record Host(String name, String os, String arch, String context, String diskRoot, String java, String uptime) {
    }

    public record Cpu(String name, int cores, Double systemLoad, Double processLoad, Double loadAverage,
                      Integer systemPercent, Integer processPercent) {
    }

    public record Memory(long totalBytes, long usedBytes, long freeBytes, String totalLabel, String usedLabel,
                         String freeLabel, Integer percent, long heapUsedBytes, long heapMaxBytes,
                         String heapUsedLabel, String heapMaxLabel, Integer heapPercent) {
    }

    public record Disk(String name, long totalBytes, long usedBytes, long freeBytes, String totalLabel,
                       String usedLabel, String freeLabel, Integer percent, boolean instance) {
    }

    public record Gpu(String name, long memoryTotalBytes, long memoryUsedBytes, String memoryTotalLabel,
                      String memoryUsedLabel, Integer memoryPercent, Integer utilization, String driver,
                      boolean live) {
    }
}
