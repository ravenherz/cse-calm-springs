package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.security.AccountAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping("/editor/instance")
public class EditorInstanceController {

    private final AccountAccessor authSupport;
    private final EditorChrome editorChrome;
    private final InstanceCapture instanceCapture;

    public EditorInstanceController(AccountAccessor authSupport, EditorChrome editorChrome,
            InstanceCapture instanceCapture) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.instanceCapture = instanceCapture;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        editorChrome.apply(model, accessor);
        model.addAttribute("snapshot", instanceCapture.capture());
        return "/admin/editor-instance";
    }

    @GetMapping(value = "/snapshot", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> snapshot() {
        return ResponseEntity.ok(instanceCapture.capture());
    }
}
