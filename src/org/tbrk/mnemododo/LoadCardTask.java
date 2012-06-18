/*
 * Copyright (C) 2010 Timothy Bourke
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.tbrk.mnemododo;

import android.app.ProgressDialog;
import mnemogogo.mobile.hexcsv.Card;
import android.util.Log;

class LoadCardTask
  extends ProgressTask<Boolean, Pair<Boolean, String>>
{
    Card card;
    boolean is_question;
    boolean start_thinking;

    boolean center;
    String html_pre;
    String html_post;

    boolean legacy;

    LoadCardTask(TaskListener<Pair<Boolean, String>> callback, Card card,
                 boolean center, String html_pre, String html_post, boolean legacy)
    {
        super(callback, R.string.loading_cards);
        this.card = card;

        this.center = center;
        this.html_pre = html_pre;
        this.html_post = html_post;
        this.legacy = legacy;
    }

    public void onPreExecute()
    {
        style = ProgressDialog.STYLE_HORIZONTAL;
    }

    public Pair<Boolean, String> doInBackground(Boolean... options)
    {
        is_question = !options[0];
        if (options.length > 1) {
            start_thinking = options[1];
        } else {
            start_thinking = is_question;
        }

        String html = makeCardHtml(card, !is_question);
        stopOperation();

        return new Pair(start_thinking, html);
    }

    protected void addReplayButton(StringBuffer html, String function)
    {
        html.append("<input type=\"button\" value=\"");
        html.append(getString(R.string.replay_sounds));
        html.append("\" style=\"margin: 1em;\" onclick=\"Mnemododo.");
        html.append(function);
        html.append(";\" />");
    }

    protected String makeCardHtml(Card c, boolean show_answer)
    {
        StringBuffer html = new StringBuffer(html_pre);

        html.append("<script language=\"javascript\">");
        html.append("function adjust_div_heights() {");
        html.append("    var qdiv = document.getElementById('q');");
        html.append("    var adiv = document.getElementById('a');");
        html.append("    var minh = window.innerHeight;");
        html.append("    if (adiv) {");
        html.append("      if (qdiv) { minh -= qdiv.offsetHeight; }");
        html.append("      adiv.style.minHeight = minh - 1 + 'px';");
        html.append("    } else {");
        html.append("      qdiv.style.minHeight = minh + 'px';");
        html.append("    }");
        html.append("}");
        html.append("window.onresize = adjust_div_heights;");
        html.append("</script>");

        html.append("<script language=\"javascript\">");
        html.append("function scroll() {");
        html.append("  var qelem = document.getElementById('q');");
        html.append("  var aelem = document.getElementById('a');");
        html.append("  if (qelem && aelem) { window.scrollTo(0, qelem.offsetHeight); } }");
        html.append("</script>");

        html.append("<body onload=\"adjust_div_heights()\">");

        String question = c.getQuestion();
        String answer = c.getAnswer();
        
        boolean question_replay = c.hasQuestionSounds();
        boolean answer_replay = c.hasAnswerSounds();
        
        if (question == null || answer == null) {
            html.append(getString(R.string.no_card_loaded_text));

        } else if (show_answer) {
            if (!card.getOverlay()) {
                if (legacy) { html.append("<div id=\"q\"><div>"); }

                html.append(question);
                if (question_replay) {
                    this.addReplayButton(html, "replayQuestionSounds()");
                }
                html.append("</div></div>"); // unclosed divs in question
                html.append("<hr/>");
            }
            if (legacy) { html.append("<div id=\"a\"><div>"); }
            html.append(answer);
            if (answer_replay) {
                this.addReplayButton(html, "replayAnswerSounds()");
            }
            html.append("</div></div>"); // unclosed divs in answer

        } else {
            if (legacy) { html.append("<div id=\"q\"><div>"); }
            html.append(question);
            if (question_replay) {
                this.addReplayButton(html, "replayQuestionSounds()");
            }
            html.append("</div></div>"); // unclosed divs in question
        }

        html.append(html_post);
        return html.toString();
    }

}

