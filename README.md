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

# 自动生成Plugin1的maindexlist.txt  #

# 拷贝Plugin1工程的mapping.txt到HostApp根目录 #

# 配置Plugin1工程和HostApp工程的proguard-rules.pro文件 #

