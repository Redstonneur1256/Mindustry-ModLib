dependencies {
    // compileOnly because common and launcher classes are provided by the launcher
    compileOnlyApi(project(":Launcher"))
    compileOnly(libs.bundles.mindustry)
}

tasks.register("packJar", Jar::class.java) {
    archiveFileName.set("ModLib.jar")
    from(project(":Launcher").tasks.shadowJar)
    with(tasks.shadowJar.get())
}

sourceSets.main {
    resources.srcDirs("../assets/")
}

if (file("private.gradle").exists()) {
    apply(from = "private.gradle")
}
