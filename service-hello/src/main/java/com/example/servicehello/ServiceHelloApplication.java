package com.example.servicehello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
@RestController
public class ServiceHelloApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceHelloApplication.class, args);
    }

    private static final Logger LOG = Logger.getLogger(ServiceHelloApplication.class.getName());


    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String home(){
        LOG.log(Level.INFO, "hello is being called");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
        throw new RuntimeException("测试异常");
//        return "hi i'm service-hello!";
    }

    @RequestMapping(value = "/hello/hi", method = RequestMethod.GET)
    public String info(){
        LOG.log(Level.INFO, "hello/hi is being called");
        return restTemplate.getForObject("http://localhost:8482/hi",String.class);
    }

    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
