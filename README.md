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

# 自动生成Plugin1的maindexlist.txt  #
在 **prebuild** 的任务之前，执行我们的任务 **collect
MainDexList**

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

collectMaindexlist.depensOn(preBuild)
```

# 拷贝Plugin1工程的mapping.txt到HostApp根目录 #

这里只针对 **release** 的buildTypes 进行mapping文件的拷贝，因为我们只在该 **buildTYpes** 上配置了混淆
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
task configProguard {
    dontshrink(new File("./proguard-rules.pro"))
    dontshrink(new File("../HostApp/proguard-rules.pro"))
}

def dontshrink(File proguardFile) {
    boolean shouldAddConfig = true
    if (proguardFile.exists()) {
        proguardFile.eachLine {
            if (it.trim() == "-dontshrink") {
                println "contain dontshrink "
                shouldAddConfig = false
            }
        }

        if (shouldAddConfig) {
            proguardFile << "\n-dontshrink"
        }
    }

}
configProguard.dependsOn(preBuild)
```

# 注意事项 #
  * 在运行 **HostApp** 工程之前， 需要先执行 **Plugin1** 工程的 **Assemble** 任务。主要是把 **Plugin1** 工程的apk拷贝到 **HostApp** 里面去，并且将生成的 **mapping.txt** 拷贝到 **HostApp** 的根目录并且命名为 **mapping_Plugin1.txt**
