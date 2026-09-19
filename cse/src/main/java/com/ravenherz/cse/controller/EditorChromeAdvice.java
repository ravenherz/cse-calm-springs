package com.ravenherz.cse.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class EditorChromeAdvice {

    @ModelAttribute
    public void editorNav(HttpServletRequest request, Model model) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        boolean adminTool = uri.contains("/editor/account")
                || uri.contains("/editor/roles")
                || uri.contains("/editor/settings")
                || uri.contains("/editor/site-data")
                || uri.contains("/editor/api")
                || uri.contains("/editor/logs")
                || uri.contains("/editor/transcode")
                || uri.contains("/editor/instance");
        model.addAttribute("navResources", uri.contains("/editor") && !adminTool);
        model.addAttribute("navAccounts", uri.contains("/editor/account"));
        model.addAttribute("navRoles", uri.contains("/editor/roles"));
        model.addAttribute("navSettings", uri.contains("/editor/settings"));
        model.addAttribute("navSiteData", uri.contains("/editor/site-data"));
        model.addAttribute("navApi", uri.contains("/editor/api"));
        model.addAttribute("navLogs", uri.contains("/editor/logs"));
        model.addAttribute("navTranscode", uri.contains("/editor/transcode"));
        model.addAttribute("navInstance", uri.contains("/editor/instance"));
    }
}
