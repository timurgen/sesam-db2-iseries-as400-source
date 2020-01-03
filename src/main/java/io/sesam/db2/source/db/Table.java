package io.sesam.db2.source.db;

import com.ibm.as400.access.AS400JDBCResultSet;
import com.ibm.as400.access.AS400JDBCResultSetMetaData;
import io.sesam.db2.source.controller.DB2Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Timur Samkharadze
 */
public class Table implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Table.class);
    public static final String COLUMN_NAME_REGEX = "^[a-zA-Z_][a-zA-Z0-9_]*$";

    private static final int BATCH_SIZE = Integer.parseInt(
            Optional.ofNullable(
                    System.getenv("DB2_BATCH_SIZE")
            ).orElse("100000"));

    private final Connection conn;
    private final String stmtStr;

    private PreparedStatement pStmt;
    private int offset;
    private int lastBatchSize;
    private AS400JDBCResultSetMetaData metaData;

    private List<String> takeColumns;
    private String sinceColumn;
    private String sinceValue;

    protected Table(String tableName, Connection conn) {

        this.conn = conn;

        this.stmtStr = String.format(
                "SELECT {columns} FROM %s {where} LIMIT %s OFFSET ?", tableName.strip(), Table.BATCH_SIZE
        );

        this.offset = 0;
        this.lastBatchSize = Table.BATCH_SIZE;
    }

    public Table takeOnly(String[] takeColumns) {
        if (null != this.takeColumns) {
            throw new IllegalStateException("method takeOnly already called for this object");
        }

        this.takeColumns = new ArrayList<>(takeColumns.length);

        for (String column : takeColumns) {
            if (!column.matches(COLUMN_NAME_REGEX) && !"*".equals(column)) {
                throw new IllegalArgumentException(String.format("column name %s is not allowed", column));
            }
            this.takeColumns.add(column);
        }
        return this;
    }

    public Table withSince(String columnName, String sinceValue) {
        if (null != columnName && !columnName.matches(COLUMN_NAME_REGEX)) {
            throw new IllegalArgumentException(String.format("column name %s is not allowed", columnName));
        }

        this.sinceColumn = columnName;
        this.sinceValue = sinceValue;
        return this;
    }


    public boolean next() {
        return !(this.lastBatchSize < Table.BATCH_SIZE);
    }

    /**
     * Method to fetch next batch of data from current table
     *
     * @return next batch of data
     */
    public List<Map<String, Object>> nextBatch() {
        if (null == this.pStmt) {
            this.prepareStatement();
        }

        this.setOffsetToStatement();

        AS400JDBCResultSet resultSet = (AS400JDBCResultSet) this.getResultSet();

        if (null == this.metaData) {
            this.populateMetadata(resultSet);
        }

        List<Map<String, Object>> resultList = this.fetchBatchData(resultSet);
        this.lastBatchSize = resultList.size();
        this.updateOffset();

        return resultList;
    }

    private void prepareStatement() {
        String columns = Optional.of(String.join(", ", this.takeColumns)).orElse("*");
        String whereClause = "";
        if (null != this.sinceColumn && null != this.sinceValue && !this.sinceColumn.isEmpty() && !this.sinceValue.isEmpty()) {
            LOG.info("since value {} found and will be used for column {}", this.sinceValue, this.sinceColumn);
            whereClause = String.format("WHERE %s >= %s", this.sinceColumn, this.sinceValue);
        }
        try {
            this.pStmt = this.conn.prepareStatement(
                    this.stmtStr.replace("{columns}", columns).replace("{where}", whereClause)
            );
        } catch (SQLException ex) {
            throw new RuntimeException("couldn't prepare statement due to", ex);
        }
    }

    private void setOffsetToStatement() {
        try {
            this.pStmt.setInt(1, this.offset);
        } catch (SQLException ex) {
            throw new RuntimeException("couldn't update offset in query due to", ex);
        }
    }

    private ResultSet getResultSet() {
        try {
            return this.pStmt.executeQuery();
        } catch (SQLException ex) {
            throw new RuntimeException("couldn't fetch result set due to", ex);
        }
    }

    private void populateMetadata(AS400JDBCResultSet resultSet) {
        try {
            this.metaData = (AS400JDBCResultSetMetaData) resultSet.getMetaData();
        } catch (SQLException ex) {
            throw new RuntimeException("couldn't obtain metadata from result set due to", ex);
        }
    }

    private List<Map<String, Object>> fetchBatchData(AS400JDBCResultSet resultSet) {
        List<Map<String, Object>> resultList = new ArrayList<>(Table.BATCH_SIZE);
        try {
            try (resultSet) {
                while (resultSet.next()) {
                    Map<String, Object> item = new HashMap<>(this.metaData.getColumnCount());
                    for (int i = 1; i <= this.metaData.getColumnCount(); i++) {
                        String colLabel = this.metaData.getColumnLabel(i);
                        switch (this.metaData.getColumnTypeName(i)) {
                            case "SMALLINT":
                            case "INTEGER":
                            case "INT":
                                item.put(colLabel, resultSet.getInt(colLabel));
                                break;
                            case "BIGINT":
                                item.put(colLabel, resultSet.getLong(colLabel));
                                break;
                            case "DECIMAL":
                            case "FLOAT":
                                item.put(colLabel, resultSet.getDouble(colLabel));
                                break;
                            case "GRAPHIC":
                            case "VARGRAPHIC":
                                item.put(colLabel, resultSet.getString(colLabel).strip());
                                break;
                            default:
                                String type = this.metaData.getColumnTypeName(i);
                                throw new RuntimeException(String.format("data type %s is not supported", type));

                        }

                        if (colLabel.equals(this.sinceColumn)) {
                            item.put("_updated", resultSet.getObject(colLabel));
                        }
                    }
                    resultList.add(item);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("couldn't process result set due to", ex);
        }

        return resultList;
    }

    private void updateOffset() {
        this.offset += this.lastBatchSize;
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }

}
