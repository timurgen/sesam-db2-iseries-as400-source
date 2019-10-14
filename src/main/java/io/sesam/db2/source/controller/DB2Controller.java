package io.sesam.db2.source.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import io.sesam.db2.source.db.DB2IAS400Connector;
import io.sesam.db2.source.db.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Timur Samkharadze
 */
@RestController
public class DB2Controller {

    @Autowired
    private DB2IAS400Connector dbConnector;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(DB2Controller.class);

    @RequestMapping(value = {"/datasets/{table}/entities"}, method = {RequestMethod.GET})
    public void getTableData(@PathVariable String table, HttpServletResponse response) throws IOException {
        Table tableObj;
        long rowCounter = 0;
        LOG.info("serving request to fetch data from {} table", table);
        try {
            tableObj = dbConnector.fetchTable(table);
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
            }
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
