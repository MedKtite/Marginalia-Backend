package com.marginalia.dev.dto.notifications;

import java.io.Serializable;
import java.util.Map;

public class EmailMessageDto implements Serializable {

    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> variables;
    private String plainTextContent;

    public EmailMessageDto() {
    }

    public EmailMessageDto(String to, String subject, String templateName, Map<String, Object> variables, String plainTextContent) {
        this.to = to;
        this.subject = subject;
        this.templateName = templateName;
        this.variables = variables;
        this.plainTextContent = plainTextContent;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getPlainTextContent() {
        return plainTextContent;
    }

    public void setPlainTextContent(String plainTextContent) {
        this.plainTextContent = plainTextContent;
    }
}
