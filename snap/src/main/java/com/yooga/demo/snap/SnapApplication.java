package com.yooga.demo.snap;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class SnapApplication {

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(SnapApplication.class);
        //SpringApplication.run(SystemctlApplication.class, args);
        builder.headless(false)
                // .web(WebApplicationType.NONE)
                // .bannerMode(Banner.Mode.OFF)
                .run(args);
    }


    @Scheduled(cron = "0/15 * * * * ?")
    public void gc(){
        System.out.println("gc");
        System.gc();
    }



}
