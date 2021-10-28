# 0x01 HostCollision
用于host碰撞而生的小工具,专门检测渗透中需要绑定hosts才能访问的主机或内部系统

# 0x02 自言自语
写这个工具最主要是因为作者自己的使用问题还有解决一些特殊情况下的误报问题,于是它就诞生了 :)

支持动态多线程,设置代理,设置扫描协议 等等操作

# 0x03 编译方法

<details>
<summary><b>编译方法</b></summary>

这是一个 java maven项目

java版本为 1.8

导入idea,打开刚刚好下载好的源码

![](./images/1.png)

打开: /HostCollision/pom.xml 安装对应的包,第一次安装依赖包需要比较久,慢慢等不要急

![](./images/2.png)

![](./images/3.png)

编译文件地址: /HostCollision/target/HostCollision/

jar包地址: /HostCollision/target/HostCollision/HostCollision.jar

项目配置文件地址: /HostCollision/target/HostCollision/config.yml

接着拿着这个 HostCollision.jar 进行使用即可

</details>

# 0x04 使用方法

```
# 目录结构
├── HostCollision
│   ├── HostCollision.jar (主程序)
│   ├── config.yml (配置文件,保存着程序各种设置)
│   └── dataSource (程序进行host碰撞的数据来源)
│     ├── ipList.txt (输入ip地址,一行一个目标)
│     └── hostList.txt (输入host地址,一行一个目标)
```

## 0x04.1 帮助文档

```
命令: java -jar HostCollision.jar -h
```

![](./images/4.png)

## 0x04.2 基础使用方法

```
啥也不加会默认读取 config.yml 配置,运行程序

读取 dataSource 目录 ipList.txt 和 hostList.txt 的数据,进行遍历匹配访问

命令: java -jar HostCollision.jar

执行完毕以后会在根目录生成一个 年-月-日_8位随机数 的 csv/txt 文件
里面会保存碰撞成功的结果

没事可以看看 config.yml 文件, 里面保存了程序的各种配置, 可以自由搭配
```

扫描结果

![](./images/5.png)

![](./images/6.png)

## 0x04.3 命令行带参数

```
设置为:
    扫描协议为: http
    最大线程为: 1
    导出格式: csv 与 txt 全都要
    ip目录: 当前目录的ips.txt
    host目录: 当前目录的hosts.txt

命令: java -jar HostCollision.jar -sp "http" -t 1 -o "csv,txt" -ifp "./ips.txt" -hfp "./hosts.txt"
```