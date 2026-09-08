package com.ravenherz.build

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class PackCsetheme extends DefaultTask {
    @InputDirectory
    abstract DirectoryProperty getSrcDir()

    @Internal
    abstract DirectoryProperty getDistDir()

    @Input
    abstract Property<String> getThemeId()

    @Input
    abstract Property<String> getThemeVersion()

    @InputFiles
    @Optional
    abstract ConfigurableFileCollection getOverlayCss()

    @InputFiles
    @Optional
    abstract ConfigurableFileCollection getOverlaySchemas()

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getOverlaySharedDir()

    @Optional
    @OutputFile
    abstract RegularFileProperty getOutFile()

    @TaskAction
    void run() {
        def src = srcDir.get().asFile
        def themeJson = new File(src, 'theme.json')
        if (!themeJson.isFile()) {
            throw new GradleException("Missing ${themeJson}")
        }

        def id = themeId.get()
        def ver = themeVersion.get()
        def dist = distDir.get().asFile
        dist.mkdirs()
        File out
        if (outFile.isPresent()) {
            out = outFile.get().asFile
            out.parentFile?.mkdirs()
        } else {
            out = new File(dist, "${id}-${ver}.csetheme")
        }

        def stage = Files.createTempDirectory("${id}-csetheme-").toFile()
        try {
            copyTree(src.toPath(), stage.toPath(), true)
            if (overlaySharedDir.present) {
                def shared = overlaySharedDir.get().asFile
                if (shared.isDirectory()) {
                    copyTree(shared.toPath(), stage.toPath(), false)
                }
            }
            overlayCss.files.each { File css ->
                def dest = new File(stage, "css/${css.name}")
                dest.parentFile.mkdirs()
                Files.copy(css.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            overlaySchemas.files.each { File css ->
                def dest = new File(stage, "css/color-schemas/${css.name}")
                dest.parentFile.mkdirs()
                Files.copy(css.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            def stagedSchemas = new File(stage, 'css/color-schemas')
            def schemaFiles = stagedSchemas.isDirectory() ? stagedSchemas.listFiles()?.findAll { it.name.endsWith('.css') } : []
            if (schemaFiles.size() < 1) {
                throw new GradleException("Need at least one css/color-schemas/*.css")
            }
            def manifest = new JsonSlurper().parse(themeJson)
            def shell = (manifest.shell ?: '').toString()
            def forbidden = []
            stage.eachFileRecurse { f ->
                if (!f.isFile()) {
                    return
                }
                def rel = stage.toPath().relativize(f.toPath()).toString().replace('\\', '/')
                if (rel.startsWith('admin/') || rel.startsWith('fragments/')) {
                    forbidden.add(rel)
                }
                if (rel.endsWith('.js') && !(rel.startsWith('js/') && !rel.substring(3).contains('/'))) {
                    forbidden.add(rel)
                }
            }
            if (!forbidden.isEmpty()) {
                throw new GradleException("Theme pack cannot include ${forbidden}")
            }
            if ((shell == 'own' || shell == 'js') && !new File(stage, 'index.html').isFile()) {
                throw new GradleException("Own-shell / JS theme ${id} needs src/index.html")
            }
            if (shell == 'js' && !new File(stage, 'js/theme.js').isFile()) {
                throw new GradleException("JS theme ${id} needs src/js/theme.js")
            }
            if (out.exists()) {
                out.delete()
            }
            zipForwardSlash(stage, out)
        } finally {
            stage.deleteDir()
        }
        logger.quiet("Wrote ${out} (${out.length()} bytes)")
        logger.quiet("Theme pack id ${id} (.csetheme).")
    }

    private static void copyTree(Path from, Path to, boolean overwrite) {
        Files.walk(from).forEach { path ->
            def dest = to.resolve(from.relativize(path).toString())
            if (Files.isDirectory(path)) {
                Files.createDirectories(dest)
            } else if (overwrite || !Files.exists(dest)) {
                Files.createDirectories(dest.parent)
                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private static void zipForwardSlash(File stage, File out) {
        def stageFull = stage.canonicalFile.toPath()
        new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out))).withCloseable { zip ->
            Files.walk(stageFull).filter { Files.isRegularFile(it) }.forEach { path ->
                def rel = stageFull.relativize(path).toString().replace('\\', '/')
                zip.putNextEntry(new ZipEntry(rel))
                Files.copy(path, zip)
                zip.closeEntry()
            }
        }
    }
}
