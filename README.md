## Getting Started

#### 1. hbase 客户端版本如果不对应，可在maven 依赖中自行修改

#### 2. 项目通过Maven 打包发布后，直接在maven中引入，然后在application.yml中配置
          hbase:
            configuration:
              zk-quorum: 192.168.24.226,192.168.23.229,192.168.23.224
          
## Authors

* **xu jiaqi** 

## License

This project is licensed under the unlicense - see the [LICENSE.md](LICENSE.md) file for details