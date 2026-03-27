package com.ravenherz.cse;

import com.ravenherz.rhzwe.filters.ContentPrivateFilter;
import jakarta.servlet.*;
import jakarta.servlet.annotation.ServletSecurity;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
		MongoReactiveAutoConfiguration.class,
		/*
		Exclude whitelabel error pages
		 */
		ErrorMvcAutoConfiguration.class
})
@RestController
@ComponentScan(basePackages = "com.ravenherz")
public class CalmSpringsEngineApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(CalmSpringsEngineApplication.class);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);

		HttpConstraintElement forceHttpsConstraint = new HttpConstraintElement(
				ServletSecurity.TransportGuarantee.CONFIDENTIAL);
		ServletSecurityElement securityElement = new ServletSecurityElement(forceHttpsConstraint);

		servletContext.getServletRegistrations().values().stream()
				.filter(r -> r instanceof ServletRegistration.Dynamic)
				.map(r -> (ServletRegistration.Dynamic) r)
				.forEach(r -> r.setServletSecurity(securityElement));
	}

	public static void main(String[] args) {
		SpringApplication.run(CalmSpringsEngineApplication.class, args);
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


	//private String resolveError(int statusCode) {
	//	return "/?error=%s".formatted(statusCode);
	//}

	/*
	External container customization
	 */
	//@Bean
	//public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
	//	String defaultError = "/?error=-1";
	//	return factory -> {
	//		factory.addErrorPages(
	//				Arrays.stream(HttpStatus.values()).map(i -> {
	//					return new ErrorPage(i, resolveError(i.value()));
	//				}).toArray(ErrorPage[]::new)
	//		);
	//		factory.addErrorPages(new ErrorPage(defaultError));
	//		factory.addConnectorCustomizers(connector -> {
	//			ProtocolHandler protocolHandler = connector.getProtocolHandler();
	//			if (protocolHandler instanceof AbstractHttp11Protocol<?> httpHandler) {
	//				Arrays
	//						.stream(httpHandler.findSslHostConfigs())
	//						.forEach(sslHostConfig -> sslHostConfig.setHonorCipherOrder(true));
	//			}
	//		});
	//		factory.addContextCustomizers(context -> {
	//			SecurityConstraint constraint = new SecurityConstraint();
	//			constraint.setUserConstraint("CONFIDENTIAL");
	//			SecurityCollection collection = new SecurityCollection();
	//			collection.addPattern("/*");
	//			constraint.addCollection(collection);
	//			context.addConstraint(constraint);
	//		});
	//	};
	//}

	//@Override
	//protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	//	return application.sources(CalmSpringsEngineApplication.class);
	//}



}
