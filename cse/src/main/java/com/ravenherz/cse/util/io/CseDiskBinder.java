package com.ravenherz.cse.util.io;

import jakarta.servlet.ServletContext;
import org.springframework.stereotype.Component;

/**
 * Binds {@link CseDisk} to this WAR's Tomcat context before settings and MVC load instance paths.
 */
@Component("cseDiskBinder")
public class CseDiskBinder {

    public CseDiskBinder(ServletContext servletContext) {
        CseDisk.bindInstance(servletContext);
    }
}
