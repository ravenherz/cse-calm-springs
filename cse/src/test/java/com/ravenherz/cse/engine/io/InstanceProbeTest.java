package com.ravenherz.cse.engine.io;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceProbeTest {

    @Test
    void snapshotReportsCpuMemoryAndDisks() {
        InstanceProbe.Snapshot snapshot = new InstanceProbe().capture();
        assertNotNull(snapshot.host());
        assertFalse(snapshot.host().name().isBlank());
        assertFalse(snapshot.host().os().isBlank());
        assertTrue(snapshot.cpu().cores() >= 1);
        assertFalse(snapshot.cpu().name().isBlank());
        assertTrue(snapshot.memory().heapMaxBytes() > 0);
        assertNotNull(snapshot.disks());
        assertNotNull(snapshot.gpus());
    }

    @Test
    void formatsBytesAndUptime() {
        assertEquals("512 B", InstanceProbe.bytesLabel(512));
        assertEquals("1.5 KB", InstanceProbe.bytesLabel(1536));
        assertEquals("1 GB", InstanceProbe.bytesLabel(1024L * 1024 * 1024));
        assertEquals("45s", InstanceProbe.formatDuration(45_000));
        assertEquals("3m 5s", InstanceProbe.formatDuration(185_000));
        assertEquals("2h 1m", InstanceProbe.formatDuration(7_260_000));
        assertEquals("1d 2h", InstanceProbe.formatDuration(93_600_000));
    }

    @Test
    void percentClampsAndHandlesEmptyTotals() {
        assertEquals(0, InstanceProbe.percentOf(0, 100));
        assertEquals(50, InstanceProbe.percentOf(50, 100));
        assertEquals(100, InstanceProbe.percentOf(200, 100));
        assertEquals(null, InstanceProbe.percentOf(10, 0));
    }

    @Test
    void cpuUsageMatchesGlancesHostAndPerCoreProcess() {
        InstanceProbe.CpuTimes idle = InstanceProbe.parseProcStat(
                "cpu  100 0 50 850 0 0 0 0 0 0\ncpu0 100 0 50 850 0 0 0 0 0 0\n");
        InstanceProbe.CpuTimes busy = InstanceProbe.parseProcStat(
                "cpu  400 0 150 900 50 0 0 0 0 0\n");
        assertEquals(850, idle.idle());
        assertEquals(1000, idle.total());
        assertEquals(0.9, InstanceProbe.cpuUsage(idle, busy), 0.001);
        InstanceProbe.CpuTimes wait = InstanceProbe.parseProcStat("cpu  100 0 50 850 200 0 0 0");
        assertEquals(1.0, InstanceProbe.cpuUsage(idle, wait), 0.001);
        assertEquals(1.0, InstanceProbe.processCoreLoad(1_000_000_000L, 1_000_000_000L), 0.001);
        assertEquals(0.125, InstanceProbe.processCoreLoad(125_000_000L, 1_000_000_000L), 0.001);
        assertEquals(1.0, InstanceProbe.meterRatio(0.125 * 8), 0.001);
        assertEquals(1.0, InstanceProbe.meterRatio(4.0), 0.001);
        assertEquals(-1.0, InstanceProbe.meterRatio(-1), 0.001);
        assertNull(InstanceProbe.parseProcStat("cpu0 1 2 3 4\n"));
    }

    @Test
    void parseMeminfoUsesAvailableNotFree() {
        InstanceProbe.PhysicalMemory mem = InstanceProbe.parseMeminfo("""
                MemTotal:       16357700 kB
                MemFree:          209715 kB
                MemAvailable:    9437184 kB
                Buffers:          524288 kB
                Cached:          8388608 kB
                """);
        long total = 16_357_700L * 1024;
        long available = 9_437_184L * 1024;
        assertEquals(total, mem.total());
        assertEquals(available, mem.free());
        assertEquals(total - available, mem.used());
        assertEquals("15.6 GB", InstanceProbe.bytesLabel(total));
        assertTrue(mem.used() < total - (209_715L * 1024));
    }

    @Test
    void parseMeminfoFallsBackWithoutAvailable() {
        InstanceProbe.PhysicalMemory mem = InstanceProbe.parseMeminfo("""
                MemTotal:       1000000 kB
                MemFree:         100000 kB
                Buffers:          50000 kB
                Cached:          400000 kB
                SReclaimable:     20000 kB
                """);
        long total = 1_000_000L * 1024;
        long available = 570_000L * 1024;
        assertEquals(total, mem.total());
        assertEquals(available, mem.free());
        assertEquals(total - available, mem.used());
        assertNull(InstanceProbe.parseMeminfo(""));
        assertNull(InstanceProbe.parseMeminfo("# comment only\n"));
        InstanceProbe.PhysicalMemory fallback = InstanceProbe.physicalMemory("", 16_000, 4_000);
        assertEquals(16_000, fallback.total());
        assertEquals(12_000, fallback.used());
        assertEquals(4_000, fallback.free());
    }

    @Test
    void nvidiaSmiStopsAfterFiveMisses() {
        InstanceProbe probe = new InstanceProbe();
        for (int i = 0; i < InstanceProbe.NVIDIA_ATTEMPT_LIMIT + 3; i++) {
            probe.capture();
        }
        int fails = probe.nvidiaFailureCount();
        assertTrue(fails == 0 || fails == InstanceProbe.NVIDIA_ATTEMPT_LIMIT, "fails=" + fails);
    }

    @Test
    void parsesOsReleasePrettyName() {
        String text = """
                NAME="Ubuntu"
                VERSION="24.04.3 LTS (Noble Numbat)"
                ID=ubuntu
                PRETTY_NAME="Ubuntu 24.04.3 LTS"
                VERSION_ID="24.04"
                """;
        assertEquals("Ubuntu 24.04.3 LTS", InstanceProbe.prettyOsFromRelease(text));
    }

    @Test
    void osReleaseFallsBackToNameAndVersionId() {
        assertEquals("Debian GNU/Linux 12",
                InstanceProbe.prettyOsFromRelease("NAME=\"Debian GNU/Linux\"\nVERSION_ID=\"12\"\n"));
        assertEquals("", InstanceProbe.prettyOsFromRelease("# comment only\n"));
        assertEquals("Alpine Linux", InstanceProbe.unquoteOsRelease("\"Alpine Linux\""));
        assertEquals("Ubuntu 24.04.3 LTS (6.8.0-139-generic)",
                InstanceProbe.joinOsAndKernel("Ubuntu 24.04.3 LTS", "6.8.0-139-generic"));
        assertEquals("Ubuntu 24.04.3 LTS",
                InstanceProbe.joinOsAndKernel("Ubuntu 24.04.3 LTS", ""));
    }

    @Test
    void allocatedBytesMatchesDfUsedNotReserved() {
        long total = 1000;
        long actualUsed = 110;
        long reserved = 50;
        long unallocated = total - actualUsed;
        long usable = unallocated - reserved;
        assertEquals(actualUsed, InstanceProbe.allocatedBytes(total, unallocated, usable));
        assertEquals(actualUsed + reserved, total - usable);
        assertEquals(600, InstanceProbe.allocatedBytes(1000, -1, 400));
        assertEquals(1000, InstanceProbe.allocatedBytes(1000, 0, 0));
        assertEquals(200, InstanceProbe.allocatedBytes(1000, 0, 800));
    }

    @Test
    void compactDisksDropsBindMountsAndVirtualFs() {
        long raid = 915L * 1024 * 1024 * 1024;
        long used = 57L * 1024 * 1024 * 1024;
        long free = 858L * 1024 * 1024 * 1024;
        List<InstanceProbe.Disk> disks = InstanceProbe.compactDisks(List.of(
                draft("/ (/dev/md0p1)", "/dev/md0p1", "ext4", raid, used, free, false),
                draft("/dev (udev)", "udev", "devtmpfs", 7L * 1024 * 1024 * 1024, 0, 7L * 1024 * 1024 * 1024, false),
                draft("/run (tmpfs)", "tmpfs", "tmpfs", 1566L * 1024 * 1024, 1299L * 1024, 1566L * 1024 * 1024, false),
                draft("/home (/dev/md0p1)", "/dev/md0p1", "ext4", raid, used, free, false),
                draft("/var/cse (/dev/md0p1)", "/dev/md0p1", "ext4", raid, used, free, true),
                draft("/snap/core24/1224 (/dev/loop1)", "/dev/loop1", "squashfs", 66_900_000, 66_900_000, 0, false),
                draft("/data (/dev/sdb1)", "/dev/sdb1", "xfs", 100L * 1024 * 1024 * 1024, 10L * 1024 * 1024 * 1024,
                        90L * 1024 * 1024 * 1024, false)));
        assertEquals(List.of("/ (/dev/md0p1)", "/data (/dev/sdb1)"),
                disks.stream().map(InstanceProbe.Disk::name).toList());
        assertTrue(disks.get(0).instance());
        assertFalse(disks.get(1).instance());
    }

    private static InstanceProbe.VolumeDraft draft(String name, String device, String type,
            long total, long used, long free, boolean instance) {
        return new InstanceProbe.VolumeDraft(name, device, type, total, used, free, instance);
    }

    @Test
    void parsesLspciAndCsv() {
        assertEquals("Intel Corporation TigerLake-LP GT2",
                InstanceProbe.lspciName("\"00:02.0\" \"VGA compatible controller\" \"Intel Corporation\" \"TigerLake-LP GT2\""));
        List<String> cols = InstanceProbe.csv("\"NVIDIA RTX\",\"8589934592\",\"32.0.15\"");
        assertEquals(List.of("NVIDIA RTX", "8589934592", "32.0.15"), cols);
    }
}
