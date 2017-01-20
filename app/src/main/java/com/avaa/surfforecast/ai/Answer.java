package com.avaa.surfforecast.ai;

/**
 * Created by Alan on 19 Jan 2017.
 */

public class Answer {
    public String forCommand = null;

    public String toShow = null;
    public String toSay = null;

    public boolean waitForReply = false;
    public String[] replyVariants = null; // TODO: decide how to point answer content
    public String[] replyInterpreters;

    public Answer() { }
    public Answer(String toShow) {
        this.toShow = toShow;
    }
    public Answer(String toShow, String toSay) {
        this.toShow = toShow;
        this.toSay = toSay;
    }
    public Answer(String toShow, String toSay, String[] replyVariants) {
        this.toShow = toShow;
        this.toSay = toSay;
        this.replyVariants = replyVariants;
    }

    public Answer(String toShow, String toSay, String[] replyVariants, String[] replyInterpreters) {
        this.toShow = toShow;
        this.toSay = toSay;
        this.replyVariants = replyVariants;
        this.replyInterpreters = replyInterpreters;
    }

    public boolean isEmpty() {
        return toShow == null && toSay == null;
    }

    public Answer add(String s) {
        if (s == null) return this;
        toShow = toShow == null ? s : toShow + "\n\n" + s;
        toSay  = toSay  == null ? s : toSay + "\n\n" + s;
        return this;
    }
    public Answer add(Answer a) {
        if (a == null) return this;
        toShow = toShow == null ? a.toShow : toShow + "\n\n" + a.toShow;
        toSay  = toSay  == null ? a.toSay : toSay + "\n\n" + a.toSay;
        return this;
    }

    @Override
    public String toString() {
        return toShow;
    }
}
