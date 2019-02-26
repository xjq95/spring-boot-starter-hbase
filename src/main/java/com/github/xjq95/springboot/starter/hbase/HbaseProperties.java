package com.github.xjq95.springboot.starter.hbase;

import javax.validation.constraints.NotNull;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.github.xjq95.springboot.starter.hbase.HbaseProperties.PREFIX;


@Data
@ConfigurationProperties(prefix = PREFIX)
public class HbaseProperties {
    static final String PREFIX = "hbase.configuration";

    @NotNull
    private String zkQuorum;

    private String zkPort = "2181";

    private String znodeParent = "/hbase";
}