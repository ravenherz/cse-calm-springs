package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.security.AccountAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/editor/api")
public class EditorApiController {

    private final AccountAccessor authSupport;
    private final EditorChrome editorChrome;

    public EditorApiController(AccountAccessor authSupport, EditorChrome editorChrome) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        editorChrome.apply(model, accessor);
        model.addAttribute("apiSections", PublicApiDocs.sections());
        return "/admin/editor-api";
    }
}
