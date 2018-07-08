该示例对应的是书中的19.3节。

对应着混淆方案2的各个步骤：
  * 配置multidex，并且生成maindexlist.txt
  * 配置proguard

# 准备工作 #
## 配置dexOptions ##

``` groovy
    dexOptions {
        javaMaxHeapSize "4g"
        preDexLibraries = false

        additionalParameters += '--multi-dex'
        additionalParameters += '--main-dex-list=maindexlist.txt'
        additionalParameters += '--minimal-main-dex'
        additionalParameters += '--set-max-idx-number=20000'
    }
```

## 配置工程的sourceSets ##

``` groovy
sourceSets {
    main {
        java {
            srcDir 'src/main/java'
        }
    }
}
```

# 各任务hook的时机 #
主要是自定义了如下几个任务
  * collectMaindexlist
  * configProguardDontShrink
  
## 在Plugin1 **prebuild** 之前，需要生成 **maindexlist.txt** 和 配置proguard文件的 **-dontshrink** ##

``` groovy
preBuild.doFirst{
    collectMainDexList.execute()
    configProguardDontShrink.execute()
}
```

## 在Plugin1 assemble 之后，需要把Plugin1的apk拷贝至HostApp的assets目录，以及拷贝mappin.txt ##
在这个步骤，直接用的是android插件中提供的属性 **applicationVariants.all**. 因此，在Plugin1 第一次assemble之后，需要再执行一次。

这了为了方便拿到各个variant的一些路径，就使用这个。
``` groovy
applicationVariants.all { variant ->
        // checking for mapping file and copy to HostApp folder
        if (variant.mappingFile != null && variant.mappingFile.exists()) {
            copy {
                from variant.mappingFile.absolutePath
                into "../HostApp"
                rename 'mapping.txt', pluginMappingName
            }
            setProguardFileWithConfig(new File("../HostApp/proguard-rules.pro"),"-applymapping $pluginMappingName")
        }

        if (variant.name == "release") {

            // rename release apk name and copy to HostApp's assets
            variant.outputs.each { output ->
                def file = output.outputFile
                output.outputFile = new File(file.parent,
                        "plugin1.apk")
                copy {
                    from output.outputFile.absolutePath
                    into "../HostApp/src/main/assets"
                }
            }
        }

    }
```

# 自动生成Plugin1的maindexlist.txt  #
在 **prebuild** 的任务之前，执行我们的任务 **collectMainDexList**

**collectMaindexlist** 任务主要是用来扫描 **Plugin1** 的源码，并且生成对应的 ***.class** 以及对应的内部类。

``` groovy
task collectMainDexList {
    println sourceSets.asList()
    sourceSets.iterator().each {
        wEachClassWithInnerClassToFile('.', 'maindexlist', '.txt', it.allJava.files, 10)
    }
}

def wEachClassWithInnerClassToFile(
        def directory, def fileName, def extension, def javaFiles, def innerClassCount) {
    new File("$directory/$fileName$extension").withWriter { out ->
        javaFiles.iterator().each {
            String fullpath = it.canonicalPath
            // "java"字符串的长度 + 后面一个path字符的长度 = 5
            int packageStartIndex = fullpath.indexOf('java') + 5
            String reference = fullpath.substring(packageStartIndex).replace("java", "class")
            println "Class in $name: $reference"
            out.println reference
            //write inner class placeholder
            for (int i = 1; i < innerClassCount + 1; i++) {
                String innerClass = reference.replace('.', "\$$i.")
                out.println innerClass
            }
        }
    }
}
```

# 拷贝Plugin1工程的mapping.txt到HostApp根目录 #

这里只针对 **release** 的buildTypes 进行mapping文件的拷贝，因为我们只在该 **buildTypes** 上配置了混淆
``` groovy
def pluginMappingName = "mapping_$project.name" + ".txt"

applicationVariants.all { variant ->
        if (variant.name == "release") {
            // checking for mapping file and copy to HostApp folder
            if (variant.mappingFile != null && variant.mappingFile.exists()) {
                copy {
                    from variant.mappingFile.absolutePath
                    into "../HostApp"
                    rename 'mapping.txt', pluginMappingName
                }
            }
        }

    }
```

# 配置Plugin1工程和HostApp工程的proguard-rules.pro文件 #
**proguard-rules.pro** 文件需要加入 **-dontshrink** 配置

``` groovy
task configProguardDontShrink {
    setProguardFileWithConfig(new File("./proguard-rules.pro"),"-dontshrink")
    setProguardFileWithConfig(new File("../HostApp/proguard-rules.pro"),"-dontshrink")
}

def setProguardFileWithConfig(File proguardFile, String config) {
    boolean shouldAddConfig = true
    if (proguardFile.exists()) {
        proguardFile.eachLine {
            if (it.trim() == "$config") {
                shouldAddConfig = false
            }
        }

        if (shouldAddConfig) {
            proguardFile << "\n$config"
        }
    }

}
```

# 注意事项 #
  * 在运行 **HostApp** 工程之前， 需要先执行 **Plugin1** 工程的 **Assemble** 任务。主要是把 **Plugin1** 工程的apk拷贝到 **HostApp** 里面去，并且将生成的 **mapping.txt** 拷贝到 **HostApp** 的根目录并且命名为 **mapping_Plugin1.txt**
