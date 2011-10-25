package com.gigaspaces.example.dto;

import com.gigaspaces.datasource.*;
import com.j_spaces.core.client.SQLQuery;

import java.util.List;
import java.util.Properties;

public class MockEDS implements BulkDataPersister, SQLDataProvider {


    public void executeBulk(List<BulkItem> bulk) throws DataSourceException {
        System.out.println("executeBulk(List<BulkItem> bulk)");
        throw new DataSourceException();
    }

    public DataIterator iterator(SQLQuery sqlQuery) throws DataSourceException {
        System.out.println("iterator(SQLQuery sqlQuery)");
        return null;
    }

    public void init(Properties prop) throws DataSourceException {
        System.out.println("init(Properties prop)");
    }

    public DataIterator<Object> initialLoad() throws DataSourceException {
        System.out.println("initialLoad()");
        return null;
    }

    public void shutdown() throws DataSourceException {
        System.out.println("shutdown()");
    }
}
