package com.finance.tracker.dto.response;

public class AiChatResponse {
    private boolean success;
    private String reply;
    private String message;

    public AiChatResponse() {}

    public AiChatResponse(boolean success, String reply, String message) {
        this.success = success;
        this.reply = reply;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static AiChatResponseBuilder builder() {
        return new AiChatResponseBuilder();
    }

    public static class AiChatResponseBuilder {
        private boolean success;
        private String reply;
        private String message;

        public AiChatResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public AiChatResponseBuilder reply(String reply) {
            this.reply = reply;
            return this;
        }

        public AiChatResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AiChatResponse build() {
            return new AiChatResponse(success, reply, message);
        }
    }
}
