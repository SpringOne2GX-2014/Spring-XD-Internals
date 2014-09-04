Spring XD vmstat Module
=======================

Demo of Spring XD custom source module. This module is only supported on Linux.

To build:

```
./gradlew xdModule
```

To install:

```
cp -r build/modules/source/vmstat $XD_HOME/xd/modules/source
```

Simple stream:

```
xd:> stream create --name v --definition "vmstat|log"
```

Filter 50% CPU usage:

```
xd:> stream create --name v --definition "vmstat|filter --expression='payload.userCpu > 50'|log"
```
