package com.ravenherz.build

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class PackRelease extends DefaultTask {
    @InputDirectory
    abstract DirectoryProperty getSrcDir()

    @Internal
    abstract DirectoryProperty getDistDir()

    @Input
    abstract Property<String> getArchiveSlug()

    @Input
    abstract Property<String> getArchiveDisplayName()

    @Input
    abstract Property<String> getArchiveVersion()

    @Input
    @Optional
    abstract Property<String> getArchiveAuthor()

    @Input
    @Optional
    abstract Property<String> getArchiveCompany()

    @Input
    @Optional
    abstract Property<String> getArchiveDescription()

    @Optional
    @OutputFile
    abstract RegularFileProperty getOutFile()

    @TaskAction
    void run() {
        def src = srcDir.get().asFile
        def index = new File(src, 'index.html')
        if (!index.exists()) {
            throw new IllegalStateException("Cannot find src/index.html (looked under ${src})")
        }

        def dist = distDir.get().asFile
        dist.mkdirs()

        def slug = archiveSlug.get()
        def displayName = archiveDisplayName.get()
        def ver = archiveVersion.get()
        def archiveName = "${slug}-${ver}.cseapp"

        File out
        if (outFile.isPresent()) {
            out = outFile.get().asFile
            out.parentFile?.mkdirs()
        } else {
            out = new File(dist, archiveName)
        }

        def stage = Files.createTempDirectory("${slug}-pack-").toFile()
        try {
            copyTree(src.toPath(), stage.toPath())
            stage.eachFileRecurse { f ->
                if (f.isFile() && f.name.endsWith('.bak')) {
                    f.delete()
                }
            }
            def i18n = new File(stage, 'i18n')
            if (i18n.isDirectory()) {
                i18n.listFiles()?.findAll { it.name.endsWith('.json') }?.each { it.delete() }
            }
            if (!new File(stage, 'index.html').exists()) {
                throw new IllegalStateException('Staging copy is missing index.html')
            }

            def manifest = [
                    slug   : slug,
                    name   : displayName,
                    version: ver
            ]
            def author = optionalText(archiveAuthor)
            def company = optionalText(archiveCompany)
            def description = optionalText(archiveDescription)
            if (author) {
                manifest.author = author
            }
            if (company) {
                manifest.company = company
            }
            if (description) {
                manifest.description = description
            }
            def manifestJson = JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + '\n'
            new File(stage, 'version.manifest').setText(manifestJson, 'UTF-8')

            if (out.exists()) {
                out.delete()
            }
            zipForwardSlash(stage, out)
        } finally {
            stage.deleteDir()
        }

        logger.quiet("Wrote ${out} (${out.length()} bytes)")
        logger.quiet("Upload this package in CSE Admin > Apps with slug ${slug}.")
    }

    private static String optionalText(Property<String> property) {
        if (property == null || !property.present) {
            return null
        }
        def value = property.get()
        return value == null || value.toString().trim().isEmpty() ? null : value.toString().trim()
    }

    private static void copyTree(Path from, Path to) {
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Files.createDirectories(to.resolve(from.relativize(dir).toString()))
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Files.copy(file, to.resolve(from.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private static void zipForwardSlash(File stage, File out) {
        def stageFull = stage.canonicalFile.toPath()
        new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out))).withCloseable { zip ->
            Files.walk(stageFull).filter { Files.isRegularFile(it) }.forEach { path ->
                def rel = stageFull.relativize(path).toString().replace('\\', '/')
                if (!rel || rel.contains('..')) {
                    throw new IllegalStateException("Bad zip entry name: ${rel}")
                }
                def entry = new ZipEntry(rel)
                zip.putNextEntry(entry)
                Files.copy(path, zip)
                zip.closeEntry()
            }
        }
    }
}
