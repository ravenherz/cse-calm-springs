package com.ravenherz.rhzwe.dal.dto.events;

public class PageEventComment {

    private String author;
    private String date;
    private String time;
    private String message;

    public PageEventComment(String author, String date, String time, String message) {
        this.author = author;
        this.date = date;
        this.time = time;
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}