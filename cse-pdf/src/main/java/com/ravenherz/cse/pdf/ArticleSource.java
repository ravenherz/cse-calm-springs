package com.ravenherz.cse.pdf;

/**
 * Article fields the PDF renderer needs. The WAR fills this from a page event
 * after embeds are expanded.
 */
public record ArticleSource(
        String header,
        String subHeader,
        String description,
        String imageLinkFull,
        boolean album,
        boolean noTopDisplayImage,
        String pageLink) {
}
