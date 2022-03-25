package io.github.alt_groovy.bus

import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment
import io.github.alt_groovy.bus.config.Reference
import org.springframework.boot.SpringApplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.Configuration

@SpringBootApplication
@Configuration
public class Application extends SpringBootServletInitializer{

    public static void main(String[] args) {
        System.setProperty('jasypt.encryptor.password',  Reference.ALGO_START);
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        System.setProperty('jasypt.encryptor.password',  Reference.ALGO_START);
        return application
                .environment(new StandardEncryptableEnvironment())
                .sources(Application.class);
    }
}