package com.ravenherz.cse.util.video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Minimal ELF64 shared objects so headless Linux can load bytedeco's ffmpeg
 * without X11, Pulse, or ALSA packages. Symbols bind lazily; file transcode
 * never calls those device backends.
 */
final class LinuxSharedStubs {

    private static final List<String> NAMES = List.of(
            "libxcb.so.1",
            "libxcb-shm.so.0",
            "libasound.so.2",
            "libpulse.so.0");

    private LinuxSharedStubs() {
    }

    static void install(Path dir) throws IOException {
        for (String name : NAMES) {
            Files.write(dir.resolve(name), elf(name));
        }
    }

    static byte[] elf(String soname) {
        byte[] nameBytes = soname.getBytes(StandardCharsets.US_ASCII);
        int ehsize = 64;
        int phsize = 56;
        int phnum = 2;
        int phoff = ehsize;
        int dynOff = phoff + phnum * phsize;
        int dynCount = 7;
        int dynSize = dynCount * 16;
        int hashOff = dynOff + dynSize;
        int hashSize = 16;
        int strtabOff = hashOff + hashSize;
        int strtabSize = 1 + nameBytes.length + 1;
        int symtabOff = strtabOff + strtabSize;
        int symtabSize = 24;
        int total = symtabOff + symtabSize;

        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(new byte[] {0x7f, 'E', 'L', 'F', 2, 1, 1, 0});
        buf.put(new byte[8]);
        buf.putShort((short) 3);
        buf.putShort((short) 62);
        buf.putInt(1);
        buf.putLong(0);
        buf.putLong(phoff);
        buf.putLong(0);
        buf.putInt(0);
        buf.putShort((short) ehsize);
        buf.putShort((short) phsize);
        buf.putShort((short) phnum);
        buf.putShort((short) 64);
        buf.putShort((short) 0);
        buf.putShort((short) 0);

        // PT_LOAD — writable so ld.so can fill DT_DEBUG
        buf.putInt(1);
        buf.putInt(6);
        buf.putLong(0);
        buf.putLong(0);
        buf.putLong(0);
        buf.putLong(total);
        buf.putLong(total);
        buf.putLong(0x1000);

        buf.putInt(2);
        buf.putInt(6);
        buf.putLong(dynOff);
        buf.putLong(dynOff);
        buf.putLong(dynOff);
        buf.putLong(dynSize);
        buf.putLong(dynSize);
        buf.putLong(8);

        buf.putLong(14); // DT_SONAME
        buf.putLong(1);
        buf.putLong(4); // DT_HASH
        buf.putLong(hashOff);
        buf.putLong(5); // DT_STRTAB
        buf.putLong(strtabOff);
        buf.putLong(6); // DT_SYMTAB
        buf.putLong(symtabOff);
        buf.putLong(10); // DT_STRSZ
        buf.putLong(strtabSize);
        buf.putLong(11); // DT_SYMENT
        buf.putLong(24);
        buf.putLong(0);
        buf.putLong(0);

        buf.putInt(1); // nbucket
        buf.putInt(1); // nchain
        buf.putInt(0);
        buf.putInt(0);

        buf.put((byte) 0);
        buf.put(nameBytes);
        buf.put((byte) 0);
        buf.put(new byte[symtabSize]);
        return buf.array();
    }
}
