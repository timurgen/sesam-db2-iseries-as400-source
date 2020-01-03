package io.sesam.db2.source.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import io.sesam.db2.source.db.DB2IAS400Connector;
import io.sesam.db2.source.db.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Timur Samkharadze
 */
@RestController
public class DB2Controller {

    @Autowired
    private DB2IAS400Connector dbConnector;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(DB2Controller.class);

    @RequestMapping(value = {"/datasets/{table}/entities"}, method = {RequestMethod.GET})
    public void getTableData(
            @PathVariable String table,
            @RequestParam(required = false) String takeOnly,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String sinceColumn,
            HttpServletResponse response) throws IOException {

        Table tableObj;
        long rowCounter = 0;

        LOG.info("serving request to fetch data from {} table, columns {}", table, takeOnly);
        try {
            tableObj = dbConnector.fetchTable(table)
                    .takeOnly(Optional.ofNullable(takeOnly).orElse("*").split(","))
                    .withSince(sinceColumn, since);
        } catch (ClassNotFoundException | SQLException ex) {
            response.sendError(500, ex.getMessage());
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getWriter();

        writer.print('[');
        boolean isFirst = true;

        while (tableObj.next()) {
            long startTime = System.nanoTime();
            List<Map<String, Object>> batch = tableObj.nextBatch();
            if (batch.isEmpty()) {
                LOG.warn("empty batch, break fetching");
                break;
            }
            for (var item : batch) {
                if (!isFirst) {
                    writer.print(',');
                } else {
                    isFirst = false;
                }
                rowCounter++;
                writer.print(MAPPER.writeValueAsString(item));
                item.clear();
            }
            long elapsedTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.info("batch of size {} processed successfully in {} ms", batch.size(), elapsedTimeMs);
            batch.clear();
        }

        writer.print(']');
        writer.flush();
        try {
            tableObj.close();
        } catch (SQLException ex) {
            LOG.error("couldn't close DB connection due to", ex);
        }
        LOG.info("sucessfully processed {} rows", rowCounter);
    }
}
