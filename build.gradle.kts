import java.nio.charset.StandardCharsets

plugins {
  java
  id("com.diffplug.spotless") version "8.7.0"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(26))
    vendor.set(JvmVendorSpec.AZUL)
  }
}

spotless {
  fun removeProjectDirPrefix(path: String): String = path.removePrefix("$projectDir/")

  encoding(StandardCharsets.UTF_8.displayName())

  java {
    target("src/**/java/de/bwravencl/**/*.java")
    eclipse("4.33").configFile("spotless.eclipseformat.xml")
    cleanthat()
        .sourceCompatibility(
            project.extensions.getByType(JavaPluginExtension::class).sourceCompatibility.toString()
        )
        .addMutators(listOf("SafeButNotConsensual", "SafeButControversial"))
        .excludeMutator("AvoidInlineConditionals")
    importOrderFile("spotless.importorder")
    removeUnusedImports()
    forbidWildcardImports()
    forbidModuleImports()
    licenseHeader(
        $$"""
        /*
         * Copyright (C) $YEAR Matteo Hausner
         *
         * This program is free software: you can redistribute it and/or modify it under
         * the terms of the GNU General Public License as published by the Free Software
         * Foundation, either version 3 of the License, or (at your option) any later
         * version.
         *
         * This program is distributed in the hope that it will be useful, but WITHOUT
         * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
         * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
         * details.
         *
         * You should have received a copy of the GNU General Public License along with
         * this program. If not, see <https://www.gnu.org/licenses/>.
         */


        """
            .trimIndent()
    )
    endWithNewline()
  }

  kotlinGradle {
    target("*.gradle.kts")
    ktfmt("0.64")
    endWithNewline()
  }

  format("onlyNewline") {
    target(
        "LICENSE",
        "**/*.gitignore",
        "**/*.md",
    )
    endWithNewline()
  }
}

tasks.named<Jar>("jar") {
  manifest {
    attributes(
        "Premain-Class" to "de.bwravencl.edtagent.EdtAgent",
        "Can-Retransform-Classes" to "true",
        "Can-Redefine-Classes" to "true",
    )
  }
}
