package com.naijamart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NaijamartApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaijamartApplication.class, args);
    }

}