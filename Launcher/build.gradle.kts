dependencies {
    compileOnly(libs.bundles.mindustry)

    api(project(":Common"))
    api(libs.mixin)
    api(libs.bundles.asm)
    api(libs.accesswidener)
    api(libs.gson)
    api(libs.hjson)
}

tasks.jar {
    manifest.attributes("Main-Class" to "fr.redstonneur1256.modlib.launcher.ModLibLauncher")
}

tasks.shadowJar {
    archiveFileName.set("ModLib-launcher.jar")
}

if(file("private.gradle").exists()) {
    apply(from = "private.gradle")
}
