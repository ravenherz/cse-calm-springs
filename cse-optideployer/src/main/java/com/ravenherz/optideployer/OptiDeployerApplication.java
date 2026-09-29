package com.ravenherz.optideployer;

import jakarta.servlet.HttpConstraintElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.annotation.ServletSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class OptiDeployerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OptiDeployerApplication.class);
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
        SpringApplication.run(OptiDeployerApplication.class, args);
    }
}
