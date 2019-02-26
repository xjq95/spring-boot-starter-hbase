package com.github.xjq95.springboot.starter.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.util.Assert;

@Slf4j
public class HbaseTemplate implements HbaseOperations {

    private Configuration configuration;

    private volatile Connection connection;

    public HbaseTemplate(Configuration configuration) {
        this.setConfiguration(configuration);
        Assert.notNull(configuration, " a valid configuration is required");
    }

    @Override
    public <T> T execute(String tableName, TableCallback<T> action) throws Throwable {
        Assert.notNull(action, "Callback object must not be null");
        Assert.notNull(tableName, "No table specified");
        Table table = null;
        try {
            table = this.getConnection().getTable(TableName.valueOf(tableName));
            return action.doInTable(table);
        } finally {
            if (null != table) {
                try {
                    table.close();
                } catch (IOException e) {
                    log.error("hbase资源释放失败");
                }
            }
        }
    }

    @Override
    public <T> List<T> find(String tableName, String family, final RowMapper<T> action) throws Throwable {
        Scan scan = new Scan();
        scan.setCaching(5000);
        scan.addFamily(Bytes.toBytes(family));
        return this.find(tableName, scan, action);
    }

    @Override
    public <T> List<T> find(String tableName, String family, String qualifier, final RowMapper<T> action) throws Throwable {
        Scan scan = new Scan();
        scan.setCaching(5000);
        scan.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
        return this.find(tableName, scan, action);
    }

    @Override
    public <T> List<T> find(String tableName, final Scan scan, final RowMapper<T> action) throws Throwable {
        return this.execute(tableName, table -> {
            int caching = scan.getCaching();
            // 如果caching未设置(默认是1)，将默认配置成5000
            if (caching == 1) {
                scan.setCaching(5000);
            }
            try (ResultScanner scanner = table.getScanner(scan)) {
                List<T> rs = new ArrayList<>();
                int rowNum = 0;
                for (Result result : scanner) {
                    rs.add(action.mapRow(result, rowNum++));
                }
                return rs;
            }
        });
    }

    @Override
    public <T> T get(String tableName, String rowName, final RowMapper<T> mapper) throws Throwable {
        return this.get(tableName, rowName, null, null, mapper);
    }

    @Override
    public <T> T get(String tableName, String rowName, String familyName, final RowMapper<T> mapper) throws Throwable {
        return this.get(tableName, rowName, familyName, null, mapper);
    }

    @Override
    public <T> T get(String tableName, final String rowName, final String familyName, final String qualifier, final RowMapper<T> mapper) throws Throwable {
        return this.execute(tableName, table -> {
            Get get = new Get(Bytes.toBytes(rowName));
            if (StringUtils.isNotBlank(familyName)) {
                byte[] family = Bytes.toBytes(familyName);
                if (StringUtils.isNotBlank(qualifier)) {
                    get.addColumn(family, Bytes.toBytes(qualifier));
                } else {
                    get.addFamily(family);
                }
            }
            Result result = table.get(get);
            return mapper.mapRow(result, 0);
        });
    }

    @Override
    public void execute(String tableName, MutatorCallback action) throws Throwable {
        Assert.notNull(action, "Callback object must not be null");
        Assert.notNull(tableName, "No table specified");

        BufferedMutator mutator = null;
        try {
            BufferedMutatorParams mutatorParams = new BufferedMutatorParams(TableName.valueOf(tableName));
            mutator = this.getConnection().getBufferedMutator(mutatorParams.writeBufferSize(3 * 1024 * 1024));
            action.doInMutator(mutator);
        } finally {
            if (null != mutator) {
                try {
                    mutator.flush();
                    mutator.close();
                } catch (IOException e) {
                    log.error("hbase mutator资源释放失败");
                }
            }
        }
    }

    @Override
    public void saveOrUpdate(String tableName, final Mutation mutation) throws Throwable {
        this.execute(tableName, mutator -> {
            mutator.mutate(mutation);
        });
    }

    @Override
    public void saveOrUpdates(String tableName, final List<Mutation> mutations) throws Throwable {
        this.execute(tableName, mutator -> {
            mutator.mutate(mutations);
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void put(String tableName, String rowName, String familyName, String qualifier, byte[] value) throws Throwable {
        Assert.hasLength(rowName);
        Assert.hasLength(familyName);
        Assert.hasLength(qualifier);
        Assert.notNull(value);
        this.execute(tableName, htable -> {
            Put put = new Put(rowName.getBytes()).addColumn(familyName.getBytes(), qualifier.getBytes(), value);
            htable.put(put);
            return null;
        });
    }

    @Override
    public void delete(String tableName, String rowName, String familyName) throws Throwable {
        delete(tableName, rowName, familyName, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void delete(String tableName, String rowName, String familyName, String qualifier) throws Throwable {
        Assert.hasLength(rowName);
        Assert.hasLength(familyName);
        execute(tableName, htable -> {
            Delete delete = new Delete(rowName.getBytes());
            byte[] family = familyName.getBytes();

            if (qualifier != null) {
                delete.addColumn(family, qualifier.getBytes());
            } else {
                delete.addFamily(family);
            }

            htable.delete(delete);
            return null;
        });
    }

    private Connection getConnection() {
        if (null == this.connection) {
            synchronized (this) {
                if (null == this.connection) {
                    try {
                        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(200, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
                        // init pool
                        poolExecutor.prestartCoreThread();
                        this.connection = ConnectionFactory.createConnection(configuration, poolExecutor);
                    } catch (IOException e) {
                        log.error("hbase connection资源池创建失败");
                    }
                }
            }
        }
        return this.connection;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}



