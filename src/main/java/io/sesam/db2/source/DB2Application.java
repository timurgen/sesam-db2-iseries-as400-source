package io.sesam.db2.source;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simple DB2 iSource AS400 source for Sesam.io apps
 * @author Timur Samkharadze
 */
@SpringBootApplication
public class DB2Application {

    public static void main(String[] args) {
        SpringApplication.run(DB2Application.class, args);
    }
}
