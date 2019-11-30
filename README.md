

## Spring Cloud Sleuth Zipkin + RabbitMQ + MySQL Demo

在微服务系统中，随着业务的发展，系统会变得越来越大，那么各个服务之间的调用关系也就变得越来越复杂。一个 HTTP 请求会调用多个不同的微服务来处理返回最后的结果，在这个调用过程中，可能会因为某个服务出现网络延迟过高或发送错误导致请求失败，这个时候，对请求调用的监控就显得尤为重要了。Spring Cloud Sleuth 提供了分布式服务链路监控的解决方案。下面介绍 Spring Cloud Sleuth 整合 Zipkin 的解决方案。

### 简介

Spring Cloud Sleuth是一款针对Spring Cloud的分布式跟踪工具。它借鉴了Dapper、Zipkin和HTrace。

Zipkin 是 Twitter 的一个开源项目，它基于 Google Dapper 实现的。我们可以使用它来收集各个服务器上请求链路的跟踪数据，并通过它提供的 REST API 接口来辅助查询跟踪数据以实现对分布式系统的监控程序，从而及时发现系统中出现的延迟过高问题。除了面向开发的 API 接口之外，它还提供了方便的 UI 组件来帮助我们直观地搜索跟踪信息和分析请求链路明细，比如可以查询某段时间内各用户请求的处理时间等。

Zipkin 和 Config 结构类似，分为服务端 Server，客户端 Client，客户端就是各个微服务应用。

### 搭建 Zipkin 服务端

在 Spring Boot 2.0 版本之后，官方已不推荐自己搭建定制了，而是直接提供了编译好的 jar 包。详情可以查看官网：https://zipkin.io/pages/quickstart.html

本文介绍的还是自己搭建zipkin-server,原因本人是想结合生产能注册到eureka上统一展示管理

#### 创建 zipkin-server

引入依赖。pom文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>spring-cloud-sleuth-zipkin-demo</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <artifactId>server-zipkin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>server-zipkin</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!--排除这个slf4j-log4j12-->
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!--引入的zipkinServer依赖-->
        <!-- zipkin 服务类-->
        <dependency>
            <groupId>io.zipkin.java</groupId>
            <artifactId>zipkin-server</artifactId>
            <version>2.12.2</version>
        </dependency>
        <!-- zipkin 界面-->
        <dependency>
            <groupId>io.zipkin.java</groupId>
            <artifactId>zipkin-autoconfigure-ui</artifactId>
            <version>2.12.2</version>
        </dependency>
        <!-- 使用消息的方式收集数据（使用rabbitmq） -->
        <dependency>
            <groupId>io.zipkin.java</groupId>
            <artifactId>zipkin-autoconfigure-collector-rabbitmq</artifactId>
            <version>2.12.2</version>
        </dependency>
        <!-- zipkin 存储到数据库需要引入以下3个依赖 -->
        <dependency>
            <groupId>io.zipkin.java</groupId>
            <artifactId>zipkin-autoconfigure-storage-mysql</artifactId>
            <version>2.12.2</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>logback-classic</artifactId>
                    <groupId>ch.qos.logback</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.16</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

配置文件：

```yaml
#配置服务及端口
server:
  port: 9411 #官方用的是9411，咱们也用这个
spring:
  main:
    allow-bean-definition-overriding: false #zipkin启动报错 解决The bean 'characterEncodingFilter', defined in class path resource [zipkin/autoconfigure/ui/ZipkinUiAutoConfiguration.class], could not be registered. A bean with that name has already been defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/HttpEncodingAutoConfiguration.class] and overriding is disabled.Action:
  application:
    name: server-zipkin
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/zipkin?characterEncoding=utf8&useSSL=true&verifyServerCertificate=false
    username: root
    password: 123456
management:
  metrics:
    web:
      server:
        auto-time-requests: false #zipkin启动报错 解决,Prometheus requires that all meters with the same name have the same set of tag keys. There is already an existing meter named 'http_server_requests_seconds' containing tag keys [exception, method, outcome, status, uri]. The meter you are attempting to register has keys [method, status, uri].
zipkin:
  collector:
    rabbitmq:
      addresses: 192.168.41.16:5672
      password: guest
      username: guest
      virtual-host: /
      queue: zipkin
  storage:
    type: mysql
```

启动类：

```java
@EnableZipkinServer
@SpringBootApplication
public class ServerZipkinApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerZipkinApplication.class, args);
    }

    @Bean
    @Primary
    public MySQLStorage mySQLStorage(DataSource datasource) {
        return MySQLStorage.newBuilder().datasource(datasource).executor(Runnable::run).build();
    }
}
```

任一方式启动后，访问 http://localhost:9411，可以看到服务端已经搭建成功

![image-20191130114416161](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fx3lpa7gj327q0r0gpx.jpg)

### 搭建 Zipkin 客户端

创建两个服务，service-hello、service-hi，service-hello 实现一个 REST 接口 /hello，/hello/hi，该接口里调用/helloe/hi调用 service-hi 应用的接口。

#### 创建 service-hello

##### 引入依赖，pom 文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>spring-cloud-sleuth-zipkin-demo</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>service-hello</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>service-hello</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-zipkin</artifactId>
            <version>2.1.1.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

##### 配置文件：

```yaml
server:
  port: 8482
spring:
  application:
    name: service-hello
  rabbitmq:
    host: 192.168.41.16
    port: 5672
    username: guest
    password: guest
  zipkin:
    rabbitmq:
      queue: zipkin
  sleuth:
    sampler:
      #采样率，推荐0.1，百分之百收集的话存储可能扛不住
      probability: 1.0 #Sleuth 默认采样算法的实现是 Reservoir sampling，具体的实现类是 PercentageBasedSampler，默认的采样比例为: 0.1，即 10%。我们可以通过 spring.sleuth.sampler.probability 来设置，所设置的值介于 0 到 1 之间，1 则表示全部采集
```

##### 启动类

```java
@SpringBootApplication
@RestController
public class ServiceHelloApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceHelloApplication.class, args);
    }

    private static final Logger LOG = Logger.getLogger(ServiceHelloApplication.class.getName());


    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String home(){
        LOG.log(Level.INFO, "hello is being called");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
        throw new RuntimeException("测试异常");
//        return "hi i'm service-hello!";
    }

    @RequestMapping(value = "/hello/hi", method = RequestMethod.GET)
    public String info(){
        LOG.log(Level.INFO, "hello/hi is being called");
        return restTemplate.getForObject("http://localhost:8483/hi",String.class);
    }

    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
```

#### 创建 service-hi

##### service2 的 pom.xml 文件:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>spring-cloud-sleuth-zipkin-demo</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <artifactId>service-hi</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>service-hi</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!--引入的zipkin依赖-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-zipkin</artifactId>
            <version>2.1.1.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

##### 配置文件如下：

```yaml
server:
  port: 8482
spring:
  application:
    name: service2
  zipkin:
    base-url: http://localhost:9411/
```

##### 启动类如下：

```java
@SpringBootApplication
@RestController
public class ServiceHiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceHiApplication.class, args);
    }

    private static final Logger LOG = Logger.getLogger(ServiceHiApplication.class.getName());


    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @RequestMapping(value = "/hi", method = RequestMethod.GET)
    public String info() {
        LOG.log(Level.INFO, "calling trace service-hi ");
        return "i'm service-hi";
    }

    @RequestMapping(value = "/hi/hello", method = RequestMethod.GET)
    public String callHome() {
        LOG.log(Level.INFO, "calling trace service-hello  ");
        return restTemplate.getForObject("http://localhost:8482/hello", String.class);
    }
}
```

#### 验证

依次启动 zipkin-server、service-hello、service-hi，

访问 http://localhost:8481/hello/hi

![image-20191130115631572](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fxgbt6rvj30n805eq37.jpg)

访问 http://localhost:8482/hi/hello

![image-20191130115721865](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fxh6xjicj31380e6dhr.jpg)

接口访问已经成功，此时，我们查看一下控制台的日志输出：

![image-20191130115807096](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fxhzo3xpj326q0eaq9l.jpg)

从上面的控制台输出内容中，我们可以看到多了一些如 [service-hello,3c15adfc71e4da46,266c1eb7011e3baf,true] 的日志信息，而这些元素正是实现分布式服务跟踪的重要组成部分，每个值的含义如下：

第一个值：service-hello，它记录了应用的名称

第二个值：3c15adfc71e4da46，是 Spring Cloud Sleuth 生成的一个 ID，称为 Trace ID，它用来标识一

请求链路。一条请求链路中包含一个 Trace ID，多个 Span ID。

第三个值：266c1eb7011e3baf，是 Spring Cloud Sleuth 生成的另外一个 ID，称为 Span ID，它表示一个基本的工作单元，比如发送一个 HTTP 请求。

第四个值：true，它表示是否要将该信息输出到 Zipkin Server 中来收集和展示。

上面四个值中的 Trace ID 和 Span ID 是 Spring Cloud Sleuth 实现分布式服务跟踪的核心。在一次请求中，会保持并传递同一个 Trance ID，从而将整个fenbu分布于不同微服务进程中的请求跟踪信息串联起来。

下面我们访问 Zipkin Server 端，http://localhost:9411/

![image-20191130120436894](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fxoqonm3j31m60u0aia.jpg)

有些小伙伴第一次进来发现服务名下并没有看到我们的应用，这是为什么呢？

这是因为 Spring Cloud Sleuth 采用了抽样收集的方式来为跟踪信息打上收集标记，也就是上面看到的第四个值。为什么要使用抽样收集呢？理论上应该是收集的跟踪信息越多越好，可以更好的反映出系统的实际运行情况，但是在高并发的分布式系统运行时，大量请求调用会产生海量的跟踪日志信息，如果过多的收集，会对系统性能造成一定的影响，所以 Spring Cloud Sleuth 采用了抽样收集的方式。

既然如此，那么我们就需要把上面第四个值改为 true，开发过程中，我们一般都是收集全部信息。

Sleuth 默认采样算法的实现是 Reservoir sampling，具体的实现类是 PercentageBasedSampler，默认的采样比例为: 0.1，即 10%。我们可以通过 spring.sleuth.sampler.probability 来设置，所设置的值介于 0 到 1 之间，1 则表示全部采集

本文是直接设置spring.sleuth.sampler.probability=1.0，采样率100%



链路追踪详情

![image-20191130120642086](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fxqws7hvj327e0nw42c.jpg)

![image-20191130120653069](https://tva1.sinaimg.cn/large/006tNbRwgy1g9fxr3nsgej31qo0u010m.jpg)



demo源码地址：

git@github.com:yanzhaoyao/spring-cloud-sleuth-zipkin-demo.git