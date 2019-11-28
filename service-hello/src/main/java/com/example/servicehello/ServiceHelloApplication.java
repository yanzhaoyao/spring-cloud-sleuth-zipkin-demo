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
        LOG.log(Level.INFO, "hi is being called");
        return "hi i'm service-bbb!";
    }

    @RequestMapping(value = "/bbb", method = RequestMethod.GET)
    public String info(){
        LOG.log(Level.INFO, "info is being called");
        return restTemplate.getForObject("http://localhost:7778/aaa",String.class);
    }

    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
