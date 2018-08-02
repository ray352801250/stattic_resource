package com.dodoca;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

public class StaticResourceServerInitializer extends SpringBootServletInitializer{
	
	@Override  
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {  
        return application.sources(StaticResourceServerApplication.class);  
    }
	
}
