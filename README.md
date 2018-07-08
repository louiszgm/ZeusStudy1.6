该示例对应的是书中的19.3节。

对应着混淆方案2的各个步骤：
  * 配置multidex，并且生成maindexlist.txt
  * 配置proguard
  * 提取Plugin1工程中mapping.txt中mypluginlibrary的部分，并在HostApp中的proguard文件apply

# 准备工作 #

## 如何使用该插件 ##
  * 将 **plugin.gradle** 放置插件工程根目录
  * 在插件工程中的 **build.gradle** 文件中引入

  ``` groovy
  apply from: 'plugin.gradle'
  ```


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

## 在Plugin1 assemble 之后，需要把Plugin1的apk拷贝至HostApp的assets目录，以及把mapping.txt中mupluginlibrary的部分提取出来放到HostApp根目录中 ##
在这个步骤，直接用的是android插件中提供的属性 **applicationVariants.all**. 因此，在Plugin1 第一次assemble之后，需要再执行一次。

这了为了方便拿到各个variant的一些路径，就使用这个。
``` groovy
applicationVariants.all { variant ->
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

            // generate mapping_pluginlibrary.txt to HostApp folder & config HostApp's Proguard File
            if (variant.mappingFile != null && variant.mappingFile.exists()) {
                generatePluginLibraryMappingFile(variant.mappingFile,"com.example.jianqiang.mypluginlibrary")
                setProguardFileWithConfig(new File("../HostApp/proguard-rules.pro"),"-applymapping mapping_pluginlibrary.txt")
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

# 将Plugin1工程的mapping.txt中mupluginlibrary的部分提取出来放到HostApp根目录中 #

这里只针对 **release** 的buildTypes 进行mapping文件进行了处理，因为我们只在该 **buildTypes** 上配置了混淆
``` groovy
def generatePluginLibraryMappingFile(File pluginMappingFile, String packageName){
    def pluginLibMappingFile = file("../HostApp/mapping_pluginlibrary.txt")
    boolean isPluginLibrary
    pluginLibMappingFile.withWriter { out ->
        pluginMappingFile.eachLine {
            if (it.startsWith(' ')){
                if (isPluginLibrary){
                    out.println it
                }
            }else {
                isPluginLibrary = false
                if (it.startsWith(packageName)){
                    isPluginLibrary = true
                    out.println it
                }
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
  * 在运行 **HostApp** 工程之前， 需要先执行 **Plugin1** 工程的 **Assemble** 任务。主要是把 **Plugin1** 工程的apk拷贝到 **HostApp** 中的assets文件夹并且将生成的 **mapping.txt** 中提取 **mypluginlibrary** 的部分到 **HostApp** 的根目录并且命名为 **mapping_pluginlibrary.txt**
