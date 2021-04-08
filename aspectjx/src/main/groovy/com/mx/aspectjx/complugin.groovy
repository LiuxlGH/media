package com.mx.aspectjx

import org.gradle.api.Plugin
import org.gradle.api.Project

class ComPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        android.applicationVariants.all { variant ->
            def buildType = variant.buildType.name
            if (buildType == "release") {
                variant.getPackageApplication().outputDirectory = new File(buildDir.absolutePath + "/releaseApk")
            }
            variant.outputs.all { output ->
                def fileName
                if (outputFileName != null && outputFileName.endsWith('.apk')) {
                    //这里修改apk文件名
                    if (variant.buildType.name.equals('release')) {
                        fileName = "sbjl-${variant.productFlavors[0].name}-${defaultConfig.versionCode}-release.apk"
                    } else if (variant.buildType.name.equals('debug')) {
                        fileName = "sbjl-${variant.productFlavors[0].name}-${defaultConfig.versionName}-debug.apk"
                    }
                    outputFileName = fileName
                    println(buildDir.absolutePath + "/releaseApk/" + fileName)
                }
            }
        }
    }
}