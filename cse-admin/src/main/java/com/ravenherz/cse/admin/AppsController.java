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

@Controller
@RequestMapping("/editor/apps")
public class AppsController {

    private final AppDesk desk;

    public AppsController(AppDesk desk) {
        this.desk = desk;
    }

    @GetMapping
    public String list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.list(request, response);
    }

    @PostMapping("/upload")
    public String upload(@RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            @RequestParam("file") MultipartFile file, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return desk.upload(slug, returnGroup, file, request, response);
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("slug") String slug,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.delete(slug, returnGroup, request, response);
    }

    @PostMapping("/store")
    public String store(@RequestParam("slug") String slug,
            @RequestParam(value = "storeEnabled", defaultValue = "false") boolean storeEnabled,
            @RequestParam(value = "storeOpen", defaultValue = "false") boolean storeOpen,
            @RequestParam(value = "maxDataKb", required = false) Integer maxDataKb,
            @RequestParam(value = "maxDocs", required = false) Integer maxDocs,
            @RequestParam(value = "maxBytesMb", required = false) Integer maxBytesMb,
            @RequestParam(value = "returnGroup", required = false) String returnGroup, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return desk.store(slug, storeEnabled, storeOpen, maxDataKb, maxDocs, maxBytesMb, returnGroup, request,
                response);
    }
}
