# Spring XD Source Module Template

## Introduction
[Spring XD](http://projects.spring.io/spring-xd/) ships with 
[many source modules](http://docs.spring.io/spring-xd/docs/1.0.0.RELEASE/reference/html/#sources)
including HTTP/REST, TCP, and various messaging systems such as JMS and RabbitMQ.
However, advanced users may encounter the need to create custom source
modules. This template project provides a starting point to create
custom source modules.

## Project Components
Source modules in Spring XD are [defined as Spring Integration output adapters](http://docs.spring.io/spring-xd/docs/1.0.0.RELEASE/reference/html/#streams). 
Therefore, the creation of a custom
module requires the wiring of a Spring Integration adapter and placing it in the
`$XD_HOME/xd/modules/source` directory.

### Gradle Build
The first place to start is the `build.gradle` file that ships with this project.
Other than setting dependencies, this build file contains a target called `xdModule`.
This will create a `build/modules/source/sample` directory which contains
the module artifacts. This directory should be moved/copied/sym-linked to
the `$XD_HOME/xd/modules/source` directory when the module is ready for use.

#### What To Change
The following sections of the Gradle build file should be customized for your
project:

On line 1, change `description` to match your module's description:

```
description = 'Spring XD Template Module' // TODO: Change this to match your module
```

On line 17, modify `moduleName` to match the name of your module:
 
```
moduleName = 'template' // TODO: change this to match the name of your module
```

On line 49, add any project dependencies, if any.

### Java Source

The Java class `ModuleTemplate` extends from a convenient super class `MessageProducerSupport`
which provides the ability to send messages as well as lifecycle hooks. Here are
the key highlights:

* `doStart` and `doStop` methods
* dedicated thread for creating source output
* `sendMessage` is invoked when the module has output
* the optional use of [`Tuple`](http://docs.spring.io/spring-xd/docs/1.0.0.RELEASE/reference/html/#tuples)

#### What To Change

Rename the class to match the name of your module.

On line 46, change `"source-template"` to match the desired name of your module's
execution thread.

Rename the `Runnable` class on line 62 to match the desired name of your module's
`Runnable` class.

Modify the `run` method starting at line 65 to perform your module's output routine.
Note that `sendMessage` must be invoked in order for your module's output to 
be sent to the channel.

### Module XML file
The module XML file consists of:

```
<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns:beans="http://www.springframework.org/schema/beans"
			 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 xmlns="http://www.springframework.org/schema/integration"
			 xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration.xsd">

	<channel id="output"/>

	<beans:bean class="hello.Sample">
		<beans:property name="autoStartup" value="false" />
		<beans:property name="outputChannel" ref="output" />
	</beans:bean>

</beans:beans>
```

The `<channel id="output"/>` element is defining the output channel
for this module. This is followed by a bean definition for the class 
`hello.Sample` which accepts the channel as property "outputChannel". 
The "autoStartup" property is set to "false" to ensure that the module 
is initialized when the channels are established.

#### What To Change

Rename the `template` directory in `modules/source/template/config` and
the `template.xml` file in this directory to match the name of your module.

On line 11, change `"hello.ModuleTemplate"` to match the name of your
module class.


