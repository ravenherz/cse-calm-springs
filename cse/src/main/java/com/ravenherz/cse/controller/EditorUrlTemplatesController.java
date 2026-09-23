package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.StoredIds;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.Event;
import com.ravenherz.cse.dal.dto.basic.HistoryData;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import com.ravenherz.cse.dal.dto.basic.enums.EventType;
import com.ravenherz.cse.present.EditorTree;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.util.UrlTemplateIds;
import com.ravenherz.cse.util.imaging.UrlTemplateImages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/editor")
public class EditorUrlTemplatesController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditorUrlTemplatesController.class);

    @Autowired
    private ResourceGroupIndex resourceGroupIndex;

    @GetMapping("/url-templates")
    public String listUrlTemplates(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        response.sendRedirect(request.getContextPath() + EditorTree.URL_TEMPLATES_HREF);
        return null;
    }

    @GetMapping("/url-template/create")
    public String createPage(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        addForm(model, accessor, null);
        return "/admin/editor-url-template-create";
    }

    @PostMapping("/url-template/create")
    public String createSubmit(@RequestParam(value = "urlTemplateId", required = false) String urlTemplateId,
            @RequestParam(value = "urlDefaultText", required = false) String urlDefaultText,
            @RequestParam(value = "urlPattern", required = false) String urlPattern,
            @RequestParam(value = "urlImage", required = false) String existingImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        String normalizedId = UrlTemplateIds.normalize(urlTemplateId);
        String imageData;
        try {
            imageData = UrlTemplateImages.keepOrEncode(existingImage, uploadedBytes(image));
        } catch (IOException e) {
            UrlTemplateEntity draft = formTemplate(normalizedId, urlDefaultText, urlPattern, existingImage);
            model.addAttribute("error", "Could not read image. Use JPEG or PNG, 256×256 px max.");
            addForm(model, accessor, draft);
            return "/admin/editor-url-template-create";
        }
        UrlTemplateEntity draft = formTemplate(normalizedId, urlDefaultText, urlPattern, imageData);
        if (!UrlTemplateIds.isValid(normalizedId)) {
            model.addAttribute("error", "Template id must be lowercase letters, numbers, and hyphens.");
            addForm(model, accessor, draft);
            return "/admin/editor-url-template-create";
        }
        if (urlPattern == null || urlPattern.isBlank()) {
            model.addAttribute("error", "URL pattern is required");
            addForm(model, accessor, draft);
            return "/admin/editor-url-template-create";
        }
        if (serviceProvider.getUrlTemplateService().getByUrlTemplateId(normalizedId) != null) {
            model.addAttribute("error", "A URL template with this id already exists");
            addForm(model, accessor, draft);
            return "/admin/editor-url-template-create";
        }
        UrlTemplateData data = new UrlTemplateData();
        data.setUrlDefaultText(blankToNull(urlDefaultText));
        data.setUrlPattern(urlPattern.trim());
        data.setUrlImage(blankToNull(imageData));
        UrlTemplateEntity entity = new UrlTemplateEntity(normalizedId, data, accessor);
        applyAccess(request, entity);
        try {
            serviceProvider.getUrlTemplateService().insert(entity);
            resourceGroupIndex.contentChanged();
        } catch (Exception e) {
            LOGGER.error("Failed to create URL template: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to create URL template: " + e.getMessage());
            addForm(model, accessor, entity);
            return "/admin/editor-url-template-create";
        }
        response.sendRedirect(request.getContextPath() + EditorTree.URL_TEMPLATES_HREF);
        return null;
    }

    @GetMapping("/url-template/edit")
    public String editPage(@RequestParam(value = "id", required = false) String id,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        UrlTemplateEntity template = loadTemplate(id);
        if (template == null) {
            error(404, request, response);
            return null;
        }
        addForm(model, accessor, template);
        return "/admin/editor-url-template-edit";
    }

    @PostMapping("/url-template/save")
    public String save(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "urlTemplateId", required = false) String urlTemplateId,
            @RequestParam(value = "urlDefaultText", required = false) String urlDefaultText,
            @RequestParam(value = "urlPattern", required = false) String urlPattern,
            @RequestParam(value = "urlImage", required = false) String existingImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        UrlTemplateEntity template = loadTemplate(id);
        if (template == null) {
            error(404, request, response);
            return null;
        }
        String normalizedId = UrlTemplateIds.normalize(urlTemplateId);
        String imageData;
        try {
            imageData = UrlTemplateImages.keepOrEncode(existingImage, uploadedBytes(image));
        } catch (IOException e) {
            model.addAttribute("error", "Could not read image. Use JPEG or PNG, 256×256 px max.");
            addForm(model, accessor, template);
            return "/admin/editor-url-template-edit";
        }
        template.setUrlTemplateId(normalizedId);
        if (template.getUrlTemplateData() == null) {
            template.setUrlTemplateData(new UrlTemplateData());
        }
        template.getUrlTemplateData().setUrlDefaultText(urlDefaultText);
        template.getUrlTemplateData().setUrlPattern(urlPattern);
        template.getUrlTemplateData().setUrlImage(imageData);
        if (!UrlTemplateIds.isValid(normalizedId)) {
            model.addAttribute("error", "Template id must be lowercase letters, numbers, and hyphens.");
            addForm(model, accessor, template);
            return "/admin/editor-url-template-edit";
        }
        if (urlPattern == null || urlPattern.isBlank()) {
            model.addAttribute("error", "URL pattern is required");
            addForm(model, accessor, template);
            return "/admin/editor-url-template-edit";
        }
        UrlTemplateEntity clash = serviceProvider.getUrlTemplateService().getByUrlTemplateId(normalizedId);
        if (clash != null && template.getId() != null && !clash.getId().equals(template.getId())) {
            model.addAttribute("error", "A URL template with this id already exists");
            addForm(model, accessor, template);
            return "/admin/editor-url-template-edit";
        }
        template.getUrlTemplateData().setUrlDefaultText(blankToNull(urlDefaultText));
        template.getUrlTemplateData().setUrlPattern(urlPattern.trim());
        template.getUrlTemplateData().setUrlImage(blankToNull(imageData));
        HistoryData historyData = template.getHistoryData();
        if (historyData == null) {
            historyData = new HistoryData();
        }
        Event[] oldEvents = historyData.getEvents() == null ? new Event[0] : historyData.getEvents();
        Event[] newEvents = new Event[oldEvents.length + 1];
        System.arraycopy(oldEvents, 0, newEvents, 0, oldEvents.length);
        newEvents[oldEvents.length] = new Event(EventType.ENTITY_EDITED, LocalDateTime.now(), accessor == null ? null : accessor.getId());
        historyData.setEvents(newEvents);
        template.setHistoryData(historyData);
        applyAccess(request, template);
        serviceProvider.getUrlTemplateService().replace(template);
        resourceGroupIndex.contentChanged();
        response.sendRedirect(request.getContextPath() + EditorTree.URL_TEMPLATES_HREF);
        return null;
    }

    @PostMapping("/url-template/delete")
    public String delete(@RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "returnGroup", required = false) String returnGroup,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        AccountEntity accessor = getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        UrlTemplateEntity template = loadTemplate(id);
        if (template != null) {
            try {
                serviceProvider.getUrlTemplateService().delete(template);
                resourceGroupIndex.contentChanged();
            } catch (Exception e) {
                LOGGER.error("Failed to delete URL template: {}", e.getMessage(), e);
            }
        }
        response.sendRedirect(request.getContextPath()
                + EditorTree.catalogReturnHref(returnGroup, EditorTree.URL_TEMPLATES_ID));
        return null;
    }

    private UrlTemplateEntity loadTemplate(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return (UrlTemplateEntity) serviceProvider.getUrlTemplateService()
                    .getById(UrlTemplateEntity.class, StoredIds.entityId(new ObjectId(id.trim())));
        } catch (Exception e) {
            return null;
        }
    }

    private void addForm(Model model, AccountEntity accessor, UrlTemplateEntity template) {
        model.addAttribute("template", template);
        addEditorChrome(model, accessor);
        if (template != null && template.getId() != null) {
            EditorInline.putTreeForUrlTemplate(model, resourceGroupIndex, template);
        } else {
            EditorInline.putTreeForUrlTemplateCreate(model, resourceGroupIndex);
        }
        addAccessPanel(model, template, accessor);
    }

    private UrlTemplateEntity formTemplate(String urlTemplateId, String urlDefaultText, String urlPattern,
            String urlImage) {
        UrlTemplateData data = new UrlTemplateData();
        data.setUrlDefaultText(urlDefaultText);
        data.setUrlPattern(urlPattern);
        data.setUrlImage(urlImage);
        UrlTemplateEntity template = new UrlTemplateEntity();
        template.setUrlTemplateId(urlTemplateId);
        template.setUrlTemplateData(data);
        return template;
    }

    private static byte[] uploadedBytes(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        return image.getBytes();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
