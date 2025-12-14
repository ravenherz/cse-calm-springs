package com.ravenherz.rhzwe.controller.objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class FormDescription implements Serializable {

    public enum FormType {
        LIST,
        TABLE
    }

    public static class FormElementGroup {

        public static class FormElementDescription {

            public enum ElementRegex {
                REGEX_LOGIN,
                REGEX_PASSWORD,
                REGEX_EMAIL,
                REGEX_SHAWN_NAME;
            }

            public enum FormElementType {
                SELECTOR,
                INPUT
            }

            private String id;
            private String label;
            private FormElementType elementType;
            private Map<String, String> options;
            private String defaultValue;
            private ElementRegex regexp;
            private boolean required;
            private String tooltip;
            private String description;
            private String matchOtherField;
            private List<String> classes;
            private String placeholder;
            private String contentType;

            public FormElementDescription() {
            }

            public FormElementType getElementType() {
                return elementType;
            }

            public void setElementType(
                    FormElementType elementType) {
                this.elementType = elementType;
            }

            public Map<String, String> getOptions() {
                return options;
            }

            public void setOptions(Map<String, String> options) {
                this.options = options;
            }

            public String getDefaultValue() {
                return defaultValue;
            }

            public void setDefaultValue(String defaultValue) {
                this.defaultValue = defaultValue;
            }

            public ElementRegex getRegexp() {
                return regexp;
            }

            public void setRegexp(
                    ElementRegex regexp) {
                this.regexp = regexp;
            }

            public boolean isRequired() {
                return required;
            }

            public void setRequired(boolean required) {
                this.required = required;
            }

            public String getTooltip() {
                return tooltip;
            }

            public void setTooltip(String tooltip) {
                this.tooltip = tooltip;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getMatchOtherField() {
                return matchOtherField;
            }

            public void setMatchOtherField(String matchOtherField) {
                this.matchOtherField = matchOtherField;
            }

            public List<String> getClasses() {
                return classes;
            }

            public void setClasses(List<String> classes) {
                this.classes = classes;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getLabel() {
                return label;
            }

            public void setLabel(String label) {
                this.label = label;
            }

            public String getPlaceholder() {
                return placeholder;
            }

            public void setPlaceholder(String placeholder) {
                this.placeholder = placeholder;
            }

            public String getContentType() {
                return contentType;
            }

            public void setContentType(String contentType) {
                this.contentType = contentType;
            }
        }

        private String groupId;
        private String groupTitle;
        private List<FormElementDescription> elements;

        public FormElementGroup() {
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getGroupTitle() {
            return groupTitle;
        }

        public void setGroupTitle(String groupTitle) {
            this.groupTitle = groupTitle;
        }

        public List<FormElementDescription> getElements() {
            return elements;
        }

        public void setElements(
                List<FormElementDescription> elements) {
            this.elements = elements;
        }
    }

    public static class FormButton {
        private String text;
        private String tooltip;
        private String onClick;
        private String rest;

        public FormButton() {
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        public String getOnClick() {
            return onClick;
        }

        public void setOnClick(String onClick) {
            this.onClick = onClick;
        }

        public String getRest() {
            return rest;
        }

        public void setRest(String rest) {
            this.rest = rest;
        }
    }

    private String id;
    private FormType formType;
    private FormButton button;
    private List<FormElementGroup> groups;

    public FormDescription() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FormType getFormType() {
        return formType;
    }

    public void setFormType(FormType formType) {
        this.formType = formType;
    }

    public FormButton getButton() {
        return button;
    }

    public void setButton(FormButton button) {
        this.button = button;
    }

    public List<FormElementGroup> getGroups() {
        return groups;
    }

    public void setGroups(
            List<FormElementGroup> groups) {
        this.groups = groups;
    }
}
