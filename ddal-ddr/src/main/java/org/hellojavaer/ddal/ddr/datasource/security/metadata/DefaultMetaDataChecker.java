/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hellojavaer.ddal.ddr.datasource.security.metadata;

import org.hellojavaer.ddal.ddr.datasource.exception.IllegalMetaDataException;
import org.hellojavaer.ddal.ddr.utils.DDRJSONUtils;
import org.hellojavaer.ddal.ddr.utils.DDRStringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author <a href="mailto:hellojavaer@gmail.com">Kaiming Zou</a>,created on 25/12/2016.
 */
public class DefaultMetaDataChecker implements MetaDataChecker {

    private static final String              MYSQL_AND_ORACLE = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? ";
    private static final String              SQL_SERVER       = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_CATALOG = ? ";

    private static final Map<String, String> map              = new LinkedHashMap<String, String>();

    /**
     *  检查指定schema下是否包含指定的table
     * 
     * @throws IllegalMetaDataException
     */
    @Override
    public void check(Connection conn, String scName, Set<String> tables) throws IllegalMetaDataException {
        if (scName == null) {
            throw new IllegalArgumentException("[Check MetaData Failed] parameter 'scName' can't be null");
        }
        if (tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("[Check MetaData Failed] parameter 'tables' can't be empty");
        }
        try {
            Set<String> set = getAllTables(conn, scName);
            if (set == null || set.isEmpty()) {
                throw new IllegalMetaDataException(
                                                   "[Check MetaData Failed] Schema:'"
                                                           + scName
                                                           + "' has nothing tables. but in your configuration it requires tables:"
                                                           + DDRJSONUtils.toJSONString(tables));
            }
            if (tables != null && !tables.isEmpty() && !set.containsAll(tables)) {
                throw new IllegalMetaDataException("[Check MetaData Failed] Schema:'" + scName + "' only has tables:"
                                                   + DDRJSONUtils.toJSONString(set)
                                                   + ", but in your configuration it requires tables:"
                                                   + DDRJSONUtils.toJSONString(tables));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> getAllTables(Connection conn, String scName) throws SQLException {
        String sql = get(conn);
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, scName);
        ResultSet rs = statement.executeQuery();
        Set<String> tabs = new HashSet<>();
        while (rs.next()) {
            tabs.add(DDRStringUtils.toLowerCase(rs.getString(1)));
        }
        return tabs;
    }

    private static String get(Connection conn) {
        String connPackage = conn.getClass().getPackage().getName();
        if (connPackage.contains("mysql")) {
            return MYSQL_AND_ORACLE;
        } else if (connPackage.contains("oracle")) {
            return MYSQL_AND_ORACLE;
        } else if (connPackage.contains("sqlserver")) {
            return SQL_SERVER;
        } else {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (connPackage.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return MYSQL_AND_ORACLE;
        }
    }

    public static void registerQueryMetaDataSQL(String keyWord, String sql) {
        map.put(keyWord, sql);
    }
}
