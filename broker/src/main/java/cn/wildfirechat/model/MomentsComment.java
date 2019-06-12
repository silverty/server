package cn.wildfirechat.model;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;

import java.util.List;

public class MomentsComment {
    private static class CommentContent {
        //feedId
        public long f;
        //type
        public int t;
        //content
        public String c;
        //media urls
        public List<String> m;
        //to replyTo
        public String r;
        //extra
        public String e;
    }

    public static MomentsComment fromMessage(WFCMessage.Message msg) {
        MomentsComment feed = new MomentsComment();
        feed.sender = msg.getFromUser();
        feed.serverTime = msg.getServerTimestamp();
        feed.commentId = msg.getMessageId();

        CommentContent content = new Gson().fromJson(new String(msg.getContent().getData().toByteArray()), CommentContent.class);
        if (content != null) {
            feed.feedId = content.f;
            feed.type = content.t;
            feed.text = content.c;
            feed.mediaUrls = content.m;
            feed.replyTo = content.r;
            feed.extra = content.e;

        }
        return feed;
    }

    private long feedId;
    private long commentId;
    private String sender;
    private int /*ProtoConstants.WFMContentType*/ type;
    private String text;
    private List<String> mediaUrls;
    private String replyTo;
    private long serverTime;
    private String extra;

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
