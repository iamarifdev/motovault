plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Report configured project dependencies, including dependencies added by plugins.
tasks.register("architectureModuleEdges") {
    val report = layout.buildDirectory.file("reports/architecture/module-edges.txt")
    outputs.file(report)
    outputs.upToDateWhen { false }
    doLast {
        val edges = rootProject.allprojects.flatMap { source ->
            source.configurations.flatMap { configuration ->
                configuration.dependencies.withType<org.gradle.api.artifacts.ProjectDependency>().map { dependency ->
                    "${source.path}\t${dependency.dependencyProject.path}"
                }.filter { edge -> edge.substringBefore('\t') != edge.substringAfter('\t') }
            }
        }.distinct().sorted()
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(edges.joinToString("\n", postfix = if (edges.isEmpty()) "" else "\n"))
        }
    }
}
