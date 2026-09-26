package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/editor/themes")
public class ThemesController {

    private final ThemeDesk desk;

    public ThemesController(ThemeDesk desk) {
        this.desk = desk;
    }

    @GetMapping
    public String list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.list(request, response);
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.upload(file, returnGroup, request, response);
    }

    @PostMapping("/activate")
    public String activate(@RequestParam("themeId") String themeId,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.activate(themeId, returnGroup, request, response);
    }

    @PostMapping("/reorder")
    public String reorder(@RequestParam("themeIds") List<String> themeIds,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.reorder(themeIds, returnGroup, request, response);
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("themeId") String themeId,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.delete(themeId, returnGroup, request, response);
    }
}
