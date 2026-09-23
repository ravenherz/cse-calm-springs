package com.ravenherz.cse.dal.dto.basic;

import java.util.Objects;

public class UrlTemplateData {

    private String urlImage;
    private String urlDefaultText;
    private String urlPattern;

    public UrlTemplateData() {
    }

    public String getUrlImage() {
        return urlImage;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }

    public String getUrlDefaultText() {
        return urlDefaultText;
    }

    public void setUrlDefaultText(String urlDefaultText) {
        this.urlDefaultText = urlDefaultText;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UrlTemplateData that = (UrlTemplateData) o;
        return Objects.equals(urlImage, that.urlImage)
                && Objects.equals(urlDefaultText, that.urlDefaultText)
                && Objects.equals(urlPattern, that.urlPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urlImage, urlDefaultText, urlPattern);
    }
}
