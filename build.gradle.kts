plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.test) apply false
}

buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk18on:1.86")
            force("org.bouncycastle:bcpkix-jdk18on:1.86")
            force("org.bitbucket.b_c:jose4j:0.9.7")
            force("org.jdom:jdom2:2.0.6.1")
            force("org.apache.commons:commons-lang3:3.20.0")
            force("org.apache.httpcomponents:httpclient:4.5.14")
        }
    }
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk18on:1.86")
            force("org.bouncycastle:bcpkix-jdk18on:1.86")
            force("org.bitbucket.b_c:jose4j:0.9.7")
            force("org.jdom:jdom2:2.0.6.1")
            force("org.apache.commons:commons-lang3:3.20.0")
            force("org.apache.httpcomponents:httpclient:4.5.14")
        }
    }
}