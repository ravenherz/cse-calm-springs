package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.engine.io.InstanceProbe;
import com.ravenherz.cse.engine.io.InstanceProbe.Snapshot;
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
public class EditorInstanceController extends AbstractController {

    private final InstanceProbe instanceProbe;

    public EditorInstanceController(InstanceProbe instanceProbe) {
        this.instanceProbe = instanceProbe;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        addEditorChrome(model, accessor);
        model.addAttribute("snapshot", instanceProbe.capture());
        return "/admin/editor-instance";
    }

    @GetMapping(value = "/snapshot", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Snapshot> snapshot() {
        return ResponseEntity.ok(instanceProbe.capture());
    }
}
