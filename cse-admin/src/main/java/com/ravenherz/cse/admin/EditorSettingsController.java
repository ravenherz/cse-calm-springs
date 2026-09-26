package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.security.AccountAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/editor")
public class EditorSettingsController {

    private final AccountAccessor authSupport;
    private final EditorChrome editorChrome;
    private final SettingsForm settingsForm;

    public EditorSettingsController(AccountAccessor authSupport, EditorChrome editorChrome,
            SettingsForm settingsForm) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.settingsForm = settingsForm;
    }

    @GetMapping("/settings")
    public String settingsPage(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        editorChrome.apply(model, accessor);
        settingsForm.fill(model);
        return "/admin/editor-settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam Map<String, String> params, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        settingsForm.save(params, model);
        editorChrome.apply(model, accessor);
        settingsForm.fill(model);
        return "/admin/editor-settings";
    }
}
