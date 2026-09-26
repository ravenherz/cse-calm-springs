package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.EntityId;
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

@Controller
@RequestMapping("/editor/scripting")
public class EditorScriptingController {

    private final AccountAccessor authSupport;
    private final EditorChrome editorChrome;
    private final ScriptDesk scripts;

    public EditorScriptingController(AccountAccessor authSupport, EditorChrome editorChrome, ScriptDesk scripts) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.scripts = scripts;
    }

    @GetMapping
    public String editor(@RequestParam(name = "script", required = false) String script,
            @RequestParam(name = "new", required = false) String creating,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        if (creating != null) {
            return "redirect:/editor/sections/scripts/create";
        }
        if (script != null && !script.isBlank()) {
            return "redirect:/editor/sections/scripts/edit/" + script.trim();
        }
        return "redirect:/editor/sections/scripts";
    }

    @GetMapping("/runs")
    public String runs(Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        editorChrome.apply(model, accessor);
        model.addAttribute("scriptTab", "runs");
        model.addAttribute("scriptRuns", scripts.runs());
        return "/admin/editor-scripting";
    }

    @PostMapping
    public String save(@RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "scriptId", required = false) String scriptId,
            @RequestParam(name = "folder", required = false) String folder,
            @RequestParam(name = "source", required = false) String source,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        ScriptEditorPage page = scripts.save(id, scriptId, folder, source, accessor.getId());
        if (!page.getError().isEmpty()) {
            apply(model, accessor, "editor", page);
            return "/admin/editor-scripting";
        }
        return "redirect:/editor/scripting?script=" + page.getEntityId();
    }

    @PostMapping("/delete")
    public String delete(@RequestParam(name = "id", required = false) String id,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        scripts.delete(id);
        return "redirect:/editor/scripting";
    }

    @PostMapping("/run")
    public String run(@RequestParam(name = "id", required = false) String id,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String error = scripts.run(id);
        if (error != null && id != null && !id.isBlank()) {
            return "redirect:/editor/sections/scripts/edit/" + id.trim();
        }
        if (error != null) {
            model.addAttribute("scriptError", error);
            model.addAttribute("scriptRuns", scripts.runs());
            editorChrome.apply(model, accessor);
            return "/admin/editor-scripting";
        }
        return "redirect:/editor/scripting/runs";
    }

    private void apply(Model model, AccountEntity accessor, String tab, ScriptEditorPage page) {
        editorChrome.apply(model, accessor);
        model.addAttribute("scriptTab", tab);
        model.addAttribute("scriptTree", page.getTree());
        model.addAttribute("scriptEntityId", page.getEntityId());
        model.addAttribute("scriptId", page.getScriptId());
        model.addAttribute("scriptFolder", page.getFolder());
        model.addAttribute("scriptSource", page.getSource());
        model.addAttribute("scriptError", page.getError());
        model.addAttribute("scriptForm", page.isForm());
    }
}
