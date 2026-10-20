package org.cabbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CabbookApplication {
    public static void main(String[] args) {
        SpringApplication.run(CabbookApplication.class, args);
    }
}
