package com.avaa.surfforecast.ai;

import android.util.Log;

/**
 * Created by Alan on 19 Jan 2017.
 */

public class Answer {
    public String forCommand = null;
    public String clarification = null;

    public String toShow = null;
    public String toSay = null;

    public boolean waitForReply = false;
    public String[] replyVariants = null; // TODO: decide how to point answer content
    public String[] replyInterpreters;

    public Answer() {
    }

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

    public Answer(String toShow, String toSay, boolean waitForReply, String[] replyVariants, String[] replyInterpreters) {
        this.toShow = toShow;
        this.toSay = toSay;
        this.waitForReply = waitForReply;
        this.replyVariants = replyVariants;
        this.replyInterpreters = replyInterpreters;
    }

    public boolean isEmpty() {
        return toShow == null && toSay == null;
    }

    public Answer add(String s) {
        if (s == null) return this;
        toShow = toShow == null ? s : toShow + "\n\n" + s;
        toSay = toSay == null ? s : toSay + "\n\n" + s;
        return this;
    }

    public Answer add(Answer a) {
        if (a == null) return this;
        Log.i("Answer", "add( " + a + " )");
        toShow = toShow == null ? a.toShow : toShow + "\n\n" + a.toShow;
        toSay = toSay == null ? a.toSay : toSay + "\n\n" + a.toSay;
        return this;
    }

    public Answer add(Answer a, boolean noWrap) {
        if (!noWrap) return add(a);
        if (a == null) return this;
        toShow = toShow == null ? a.toShow : toShow + " " + a.toShow;
        toSay = toSay == null ? a.toSay : toSay + " " + a.toSay;
        return this;
    }


    public void addClarification(String s) {
        if (clarification == null) clarification = CommandsExecutor.capitalize(s);
        else clarification += " " + s;
    }


    @Override
    public String toString() {
        return "Answer{" +
                "forCommand='" + forCommand + '\'' +
                ", clarification='" + clarification + '\'' +
                ", toShow='" + toShow + '\'' +
                ", toSay='" + toSay + '\'' +
                ", waitForReply=" + waitForReply +
//                ", replyVariants=" + Arrays.toString(replyVariants) +
//                ", replyInterpreters=" + Arrays.toString(replyInterpreters) +
                '}';
    }
}
