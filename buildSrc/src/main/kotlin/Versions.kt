/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version.
 */
object Versions {
    const val appcompat: String = "1.0.2"

    const val core_testing: String = "2.0.1"

    const val androidx_concurrent: String = "1.0.0-beta01" 

    const val constraintlayout: String = "1.1.3" 

    const val core_ktx: String = "1.0.2"

    const val androidx_lifecycle: String = "2.0.0"

    const val androidx_navigation: String = "2.1.0-beta02"

    const val androidx_paging: String = "2.1.0"

    const val androidx_room: String = "2.2.0-alpha01"

    const val espresso_core: String = "3.2.0"

    const val androidx_test_ext_junit: String = "1.1.1"

    const val androidx_work: String = "2.1.0" 

    const val aapt2: String = "3.5.0-rc01-5435860" 

    const val com_android_tools_build_gradle: String = "3.5.0-rc01" 

    const val lint_gradle: String = "26.5.0-rc01" 

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2" 

    const val junit_junit: String = "4.12" 

    const val org_jetbrains_kotlin: String = "1.3.41"

    const val kotlinx_coroutines_android: String = "1.3.0-RC" // available: "1.3.0-RC-1.3.50-eap-5"

    const val kotlinx_coroutines_core: String = "1.3.0-RC" // available: "1.3.0-RC-1.3.50-eap-5"

    const val kotlinx_serialization_runtime: String = "0.11.1" // available: "0.11.2-1.3.50-eap-5"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.4.1"

        const val currentVersion: String = "5.5.1"

        const val nightlyVersion: String = "5.7-20190731220046+0000"

        const val releaseCandidate: String = "5.6-rc-1"
    }
}
