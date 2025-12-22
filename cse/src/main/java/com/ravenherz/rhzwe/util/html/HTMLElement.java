package com.ravenherz.rhzwe.util.html;

import com.ravenherz.rhzwe.constants.Strings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class HTMLElement {

    public static class HTMLElementBuilder {

        private HTMLElement element;

        private HTMLElementBuilder() {
            element = new HTMLElement();
        }

        private HTMLElementBuilder(String elem, String content) {
            element = new HTMLElement(elem, content);
        }

        private HTMLElementBuilder(String childElementContent, String dynamicElementTitle,
                String visibilityFactorContent) {
            element = HTMLElement.getContainer(childElementContent, dynamicElementTitle,
                    visibilityFactorContent);
        }

        private HTMLElementBuilder(String childElementContent, String dynamicElementTitle,
                String visibilityFactorContent, String elemType) {
            element = new HTMLElement(childElementContent, dynamicElementTitle,
                    visibilityFactorContent, elemType);
        }

        public static HTMLElementBuilder get() {
            return new HTMLElementBuilder();
        }

        public static HTMLElementBuilder get(String elem, String content) {
            return new HTMLElementBuilder(elem, content);
        }

        public static HTMLElementBuilder get(String childElementContent, String dynamicElementTitle,
                String visibilityFactorContent) {
            return new HTMLElementBuilder(childElementContent, dynamicElementTitle,
                    visibilityFactorContent);
        }

        public static HTMLElementBuilder get(String childElementContent, String dynamicElementTitle,
                String visibilityFactorContent, String elemType) {
            return new HTMLElementBuilder(childElementContent, dynamicElementTitle,
                    visibilityFactorContent, elemType);
        }

        public HTMLElementBuilder addAttribute(String attrName, String attrValue) {
            element.addAttribute(attrName, attrValue);
            return this;
        }

        public HTMLElementBuilder surroundByContainer(String dynamicElementTitle,
                String visibilityFactorContent) {
            element = new HTMLElement(element.getCode(), dynamicElementTitle,
                    visibilityFactorContent, Strings.ELEM_NAME_DIV);
            return this;
        }

        public HTMLElementBuilder surroundByContainer(String dynamicElementTitle,
                String visibilityFactorContent, String elemType) {
            element = new HTMLElement(element.getCode(), dynamicElementTitle,
                    visibilityFactorContent, elemType);
            return this;
        }

        public HTMLElement build() {
            return element;
        }

    }

    // According to https://stackoverflow.com/questions/3558119/are-non-void-self-closing-tags-valid-in-html5
    private static final HashSet<String> voidElements;

    static {
        voidElements = new HashSet<>();
        voidElements.add("area");
        voidElements.add("base");
        voidElements.add("br");
        voidElements.add("col");
        voidElements.add("embed");
        voidElements.add("img");
        voidElements.add("keygen");
        voidElements.add("input");
        voidElements.add("link");
        voidElements.add("meta");
        voidElements.add("param");
        voidElements.add("source");
        voidElements.add("track");
        voidElements.add("wbr");
    }

    private HashMap<String, String> attributes;
    private String element;
    private String content;
    private Boolean none = false;

    public HTMLElement(String element, String content) {
        this.element = element;
        this.content = content;
        this.attributes = new HashMap<>();
    }

    private HTMLElement(String childElementContent, String dynamicElementTitle,
            String visibilityFactorContent, String elemType) {
        this.attributes = new HashMap<>();
        this.attributes.put(Strings.ATTR_NAME_ID, dynamicElementTitle + Strings.POSTFIX_CONTAINER);
        if (elemType.equals(Strings.ELEM_NAME_DIV)) {
            this.attributes.put(Strings.ATTR_NAME_CLASS, Strings.CSS_CLASS_ALL_WIDE_DIV);
        }
        if ((visibilityFactorContent != null) && (visibilityFactorContent.trim().length() > 0)) {
            this.attributes.put(Strings.ATTR_NAME_STYLE, "display:inline-block");
        } else {
            this.attributes.put(Strings.ATTR_NAME_STYLE, "display:none");
        }
        this.element = elemType;
        this.content = childElementContent;
    }

    public HTMLElement() {
        none = true;
    }

    public Boolean addAttribute(String attrName, String attrValue) {
        if ((attrName != null) && (attrName.trim().length() > 0)) {
            if ((attrValue != null) && (attrValue.trim().length() > 0)) {
                this.attributes.put(attrName, attrValue);
                return true;
            }
        }
        return false;
    }

    public String getCode() {
        if (!none) {
            String code = "";
            if ((element != null) && (element.trim().length() > 0)) {
                code = "<" + element;
                if (attributes != null) {
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        code = createAttribute(code, entry.getKey(), entry.getValue());
                    }
                }
                if (isVoid(element)) {
                    code += ">";
                } else {
                    if (content == null) {
                        content = "";
                    }
                    code += ">" + content + "</" + element + ">";
                }
            }
            return code;
        }
        return "";
    }

    private String createAttribute(String source, String attrName, String attrValue) {
        if (attrName != null && attrName.length() > 0) {
            if (attrValue != null && attrValue.length() > 0) {
                source += " " + attrName + "=\"" + attrValue + "\"";
            }
        }
        return source;
    }

    private static Boolean isVoid(String elementType) {
        return voidElements.contains(elementType);
    }

    public HTMLElement surroundByContainer(String dynamicElementTitle,
            String visibilityFactorContent) {
        return new HTMLElement(this.getCode(), dynamicElementTitle, visibilityFactorContent,
                Strings.ELEM_NAME_DIV);
    }

    public HTMLElement surroundByContainer(String dynamicElementTitle,
            String visibilityFactorContent, String elemType) {
        return new HTMLElement(this.getCode(), dynamicElementTitle, visibilityFactorContent,
                elemType);
    }

    public static HTMLElement getContainer(String content, String dynamicElementTitle,
            String visibilityFactorContent) {
        return new HTMLElement(content, dynamicElementTitle, visibilityFactorContent,
                Strings.ELEM_NAME_DIV);
    }

}
