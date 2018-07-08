该示例对应的是书中的19.3节。

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

# 配置Plugin1工程和HostApp工程的proguard-rules.pro文件 #

