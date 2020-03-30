plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:tree-psi"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
