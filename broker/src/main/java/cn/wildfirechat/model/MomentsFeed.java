package cn.wildfirechat.model;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;

import java.util.List;

public class MomentsFeed {
    private static class FeedContent {
        //type
        public int t;
        //content
        public String c;
        //media urls
        public List<String> m;
        //to users
        public List<String> to;
        //excloud users
        public List<String> ex;
        //extra
        public String e;
    }

    private long feedId;
    private String sender;
    private int /*ProtoConstants.WFMContentType*/ type;
    private String text;
    private List<String> mediaUrls;
    private List<String> toUsers;
    private List<String> exUsers;

    public void setType(int type) {
        this.type = type;
    }

    public List<String> getToUsers() {
        return toUsers;
    }

    public void setToUsers(List<String> toUsers) {
        this.toUsers = toUsers;
    }

    public List<String> getExUsers() {
        return exUsers;
    }

    public void setExUsers(List<String> exUsers) {
        this.exUsers = exUsers;
    }

    private List<String> mentionedUser;
    private long serverTime;
    private String extra;

    public static MomentsFeed fromMessage(WFCMessage.Message msg) {
        MomentsFeed feed = new MomentsFeed();
        feed.sender = msg.getFromUser();
        feed.serverTime = msg.getServerTimestamp();
        feed.feedId = msg.getMessageId();

        FeedContent content = new Gson().fromJson(msg.getContent().getData().toString(), FeedContent.class);
        if (content != null) {
            feed.type = content.t;
            feed.text = content.c;
            feed.mediaUrls = content.m;
            feed.toUsers = content.to;
            feed.exUsers = content.ex;
            feed.extra = content.e;

        }
        return feed;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
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

    public List<String> getMentionedUser() {
        return mentionedUser;
    }

    public void setMentionedUser(List<String> mentionedUser) {
        this.mentionedUser = mentionedUser;
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
