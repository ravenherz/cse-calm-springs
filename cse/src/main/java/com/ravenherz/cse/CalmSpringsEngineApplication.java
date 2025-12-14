package com.ravenherz.cse;

import com.ravenherz.rhzwe.filters.ContentPrivateFilter;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Arrays;


@SpringBootApplication(exclude = {
		/*
		Exclude classes responsible for immediate mongodb connection establishment on startup
		since we have lazy initialization
		 */
		MongoAutoConfiguration.class,
		MongoDataAutoConfiguration.class,
		MongoReactiveAutoConfiguration.class
})
@RestController
@ComponentScan(basePackages = "com.ravenherz")
public class CalmSpringsEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(CalmSpringsEngineApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}

	@Bean
	public SpringResourceTemplateResolver templateResolver(){
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
		templateResolver.setPrefix("classpath:WEB-INF/themes");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCacheable(true);

		return templateResolver;
	}

	@Bean
	public SpringTemplateEngine templateEngine(){
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(templateResolver());
		templateEngine.setEnableSpringELCompiler(true);
		return templateEngine;
	}

	@Bean
	public FilterRegistrationBean<ContentPrivateFilter> privateFilter(){
		FilterRegistrationBean<ContentPrivateFilter> registrationBean
				= new FilterRegistrationBean<>();

		registrationBean.setFilter(new ContentPrivateFilter());
		registrationBean.addUrlPatterns("/content-private/*");
		registrationBean.setOrder(2);

		return registrationBean;
	}

//	@Bean
//	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> getWebServerFactoryCustomizer() {
//		return factory -> {
//			Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
//			httpConnector.setPort(8080);
//			factory.addConnectorCustomizers(connector -> connector.addUpgradeProtocol(new Http2Protocol()));
//			factory.addAdditionalTomcatConnectors(httpConnector);
//		};
//	}

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
		return factory ->
				factory.addConnectorCustomizers(connector -> {
					ProtocolHandler protocolHandler = connector.getProtocolHandler();
					if (protocolHandler instanceof AbstractHttp11Protocol<?> httpHandler) {
						Arrays
								.stream(httpHandler.findSslHostConfigs())
								.forEach(sslHostConfig -> sslHostConfig.setHonorCipherOrder(true));
					}
				});
	}

}
