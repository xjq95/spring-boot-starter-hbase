package com.github.xjq95.springboot.starter.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(HbaseProperties.class)
@ConditionalOnClass(HbaseProperties.class)
@ConditionalOnProperty(prefix = HbaseProperties.PREFIX, name = "zk-quorum")
public class HBaseAutoConfiguration {

    private final HbaseProperties hbaseProperties;

    @Autowired
    public HBaseAutoConfiguration(HbaseProperties hbaseProperties) {
        this.hbaseProperties = hbaseProperties;
    }

    @Bean
    HbaseTemplate hbaseTemplate() {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", hbaseProperties.getZkQuorum());
        conf.set("hbase.zookeeper.property.clientPort", hbaseProperties.getZkPort());
        conf.set("zookeeper.znode.parent", hbaseProperties.getZnodeParent());
        return new HbaseTemplate(conf);
    }
}