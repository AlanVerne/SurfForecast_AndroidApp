package com.avaa.surfforecast.ai;

/**
 * Created by Alan on 19 Jan 2017.
 */

public class Answer {
    public String toShow = null;
    public String toSay = null;
    public String toAsk = null;
    public String toAskReplyVariants = null;

    public Answer() { }
    public Answer(String toShow) {
        this.toShow = toShow;
    }
    public Answer(String toShow, String toSay) {
        this.toShow = toShow;
        this.toSay = toSay;
    }

    public Answer add(String s) {
        if (s == null) return this;
        toShow = toShow == null ? s : toShow + "\n" + s;
        toSay  = toSay  == null ? s : toSay + "\n" + s;
        return this;
    }
    public Answer add(Answer a) {
        if (a == null) return this;
        toShow = toShow == null ? a.toShow : toShow + "\n" + a.toShow;
        toSay  = toSay  == null ? a.toSay : toSay + "\n" + a.toSay;
        return this;
    }

    @Override
    public String toString() {
        return toShow;
    }
}
