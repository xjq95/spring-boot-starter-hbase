package com.github.xjq95.springboot.starter.hbase;

import org.apache.hadoop.hbase.client.BufferedMutator;

/**
 * @desc callback for hbase put delete and update
 */
public interface MutatorCallback {

    /**
     * 使用mutator api to update put and delete
     */
    void doInMutator(BufferedMutator mutator) throws Throwable;
}