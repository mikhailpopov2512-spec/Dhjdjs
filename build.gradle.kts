// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register("fixGitWrapper") {
    doLast {
        println("=== REGENERATING GRADLE WRAPPER JAR DIRECTLY FROM OFFICIAL GRADLE DIST ZIP ===")
        val jarFile = file("gradle/wrapper/gradle-wrapper.jar")
        jarFile.parentFile.mkdirs()
        
        val zipUrl = "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
        println("Downloading official Gradle distribution from: $zipUrl")
        
        val tempZipFile = file("build/tmp/gradle-dist.zip")
        tempZipFile.parentFile.mkdirs()
        
        try {
            java.net.URL(zipUrl).openStream().use { input ->
                tempZipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("Downloaded distribution successfully! Size: ${tempZipFile.length()} bytes")
        } catch (e: Exception) {
            println("Download failed: ${e.message}")
            throw e
        }
        
        // Extract the gradle-wrapper.jar from the zip
        println("Extracting gradle-wrapper.jar from distribution zip...")
        var extracted = false
        try {
            java.util.zip.ZipFile(tempZipFile).use { zip ->
                val entries = zip.entries().asSequence().toList()
                val jarEntries = entries.filter { it.name.endsWith(".jar") }
                println("Searching through ${jarEntries.size} jars in the distribution for embedded gradle-wrapper.jar resource...")
                
                for (jarEntry in jarEntries) {
                    val tempJarBytes = zip.getInputStream(jarEntry).readAllBytes()
                    val bais = java.io.ByteArrayInputStream(tempJarBytes)
                    
                    try {
                        java.util.zip.ZipInputStream(bais).use { zis ->
                            var ze = zis.nextEntry
                            while (ze != null) {
                                if (ze.name.endsWith("gradle-wrapper.jar")) {
                                    println("\nSUCCESS! Found embedded wrapper jar resource inside: ${jarEntry.name} -> ${ze.name} (${ze.size} bytes)")
                                    
                                    // Extract this resource exactly!
                                    jarFile.outputStream().use { output ->
                                        zis.copyTo(output)
                                    }
                                    extracted = true
                                    break
                                }
                                ze = zis.nextEntry
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore zip parse errors
                    }
                    if (extracted) break
                }
            }
        } catch (e: Exception) {
            println("Extraction failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            tempZipFile.delete()
        }



        
        if (!extracted) {
            throw GradleException("Failed to extract gradle-wrapper.jar!")
        }
        
        // Re-write the jar to inject the Manifest with Main-Class
        println("\n=== INJECTING MAIN-CLASS TO MANIFEST ===")
        val tempEnrichedJarFile = file("build/tmp/gradle-wrapper-enriched.jar")
        tempEnrichedJarFile.parentFile.mkdirs()
        
        try {
            java.util.zip.ZipFile(jarFile).use { zip ->
                java.util.zip.ZipOutputStream(tempEnrichedJarFile.outputStream()).use { zos ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        
                        if (entry.name == "META-INF/MANIFEST.MF") {
                            // Write our custom manifest with Main-Class
                            zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
                            val customManifest = "Manifest-Version: 1.0\nImplementation-Title: Gradle Wrapper\nMain-Class: org.gradle.wrapper.GradleWrapperMain\n\n"
                            zos.write(customManifest.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        } else {
                            // Copy original entry exactly
                            zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
                            zip.getInputStream(entry).use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            
            // Replace the original jar with our enriched jar
            tempEnrichedJarFile.copyTo(jarFile, overwrite = true)
            tempEnrichedJarFile.delete()
            println("Successfully injected Main-Class! Enriched jar size: ${jarFile.length()} bytes")
        } catch (e: Exception) {
            println("Failed to inject Main-Class: ${e.message}")
            e.printStackTrace()
            throw e
        }
        
        // Inspect manifest
        println("\n=== INSPECTING EXTRACTED JAR MANIFEST ===")
        try {
            java.util.zip.ZipFile(jarFile).use { zip ->
                val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF")
                if (manifestEntry != null) {
                     zip.getInputStream(manifestEntry).bufferedReader().useLines { lines ->
                         lines.forEach { println("Manifest Line: $it") }
                     }
                } else {
                     println("No Manifest found!")
                }
            }
        } catch (e: Exception) {
            println("Manifest read failed: ${e.message}")
        }
        
        // Try running gradlew
        println("\nTesting ./gradlew execution:")
        val gradlewFile = file("gradlew")
        gradlewFile.setExecutable(true)
        val process = java.lang.ProcessBuilder("./gradlew", "--version")
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { println("GradlewOutput: $it") }
        }
        val exitCode = process.waitFor()
        println("Exit code: $exitCode")
        if (exitCode != 0) {
            throw GradleException("Failed to run gradlew script!")
        }
        println("Success! The official extracted gradle-wrapper.jar works perfectly!")
    }
}








