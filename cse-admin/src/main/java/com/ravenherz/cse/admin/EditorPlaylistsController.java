package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/editor")
public class EditorPlaylistsController {

    private final PlaylistDesk desk;

    public EditorPlaylistsController(PlaylistDesk desk) {
        this.desk = desk;
    }

    @GetMapping("/playlists")
    public String listPlaylists(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.list(request, response);
    }

    @GetMapping("/playlist/create")
    public String createPlaylistPage(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return desk.createPage(model, request, response);
    }

    @PostMapping("/playlist/create")
    public String createPlaylistSubmit(@RequestParam(value = "playlistId", required = false) String playlistId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "resourceId", required = false) List<String> resourceIds,
            @RequestParam(value = "titleOverride", required = false) List<String> titleOverrides,
            @RequestParam(value = "artistOverride", required = false) List<String> artistOverrides,
            @RequestParam(value = "coverResourceId", required = false) String coverResourceId,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.create(playlistId, title, description, resourceIds, titleOverrides, artistOverrides,
                coverResourceId, model, request, response);
    }

    @GetMapping("/playlist/edit")
    public String editPlaylistPage(@RequestParam(value = "id", required = false) String id, Model model,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.editPage(id, model, request, response);
    }

    @PostMapping("/playlist/save")
    public String savePlaylist(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "playlistId", required = false) String playlistId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "resourceId", required = false) List<String> resourceIds,
            @RequestParam(value = "titleOverride", required = false) List<String> titleOverrides,
            @RequestParam(value = "artistOverride", required = false) List<String> artistOverrides,
            @RequestParam(value = "coverResourceId", required = false) String coverResourceId,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.save(id, playlistId, title, description, resourceIds, titleOverrides, artistOverrides,
                coverResourceId, model, request, response);
    }

    @PostMapping("/playlist/delete")
    public String deletePlaylist(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        return desk.delete(id, returnGroup, request, response);
    }
}
