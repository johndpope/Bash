/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.lexer;

import com.ansorgit.plugins.bash.lang.BashVersion;
import com.ansorgit.plugins.bash.util.IntStack;
import com.intellij.psi.tree.IElementType;

import java.io.IOException;

final class _BashLexer extends _BashLexerBase implements BashLexerDef {
    private final IntStack lastStates = new IntStack(25);
    //Help data to parse (nested) strings.
    private final StringLexingstate string = new StringLexingstate();
    private final HeredocLexingState heredocState = new HeredocLexingState();
    //parameter expansion parsing state
    private boolean paramExpansionHash = false;
    private boolean paramExpansionWord = false;
    private boolean paramExpansionOther = false;
    private int openParenths = 0;
    private boolean isBash4 = false;
    //True if the parser is in the case body. Necessary for proper lexing of the IN keyword
    private boolean inCaseBody = false;
    //conditional expressions
    private boolean emptyConditionalCommand = false;
    private boolean inHereString = false;

    _BashLexer(BashVersion version, java.io.Reader in) {
        super(in);

        this.isBash4 = BashVersion.Bash_v4.equals(version);
    }


    @Override
    public IElementType advance() throws IOException {
        try {
            return super.advance();
        } catch (Error e) {
            // provide the current file as context when a "couldn't match input" lexer error occurs
            throw new Error("Error lexing Bash file:\n" + getBuffer(), e);
        }
    }

    @Override
    public HeredocLexingState heredocState() {
        return heredocState;
    }

    @Override
    public boolean isInHereStringContent() {
        return inHereString;
    }

    @Override
    public void enterHereStringContent() {
        assert !inHereString : "inHereString must be false when entering a here string";

        inHereString = true;
    }

    @Override
    public void leaveHereStringContent() {
        inHereString = false;
    }

    @Override
    public boolean isEmptyConditionalCommand() {
        return emptyConditionalCommand;
    }

    @Override
    public void setEmptyConditionalCommand(boolean emptyConditionalCommand) {
        this.emptyConditionalCommand = emptyConditionalCommand;
    }

    @Override
    public StringLexingstate stringParsingState() {
        return string;
    }

    @Override
    public boolean isInCaseBody() {
        return inCaseBody;
    }

    @Override
    public void setInCaseBody(boolean inCaseBody) {
        this.inCaseBody = inCaseBody;
    }

    @Override
    public boolean isBash4() {
        return isBash4;
    }

    /**
     * Goes to the given state and stores the previous state on the stack of states.
     * This makes it possible to have several levels of lexing, e.g. for $(( 1+ $(echo 3) )).
     */
    public void goToState(int newState) {
        lastStates.push(yystate());
        yybegin(newState);
    }

    /**
     * Goes back to the previous state of the lexer. If there
     * is no previous state then YYINITIAL, the initial state, is chosen.
     */
    public void backToPreviousState() {
        // pop() will throw an exception if empty
        yybegin(lastStates.pop());
    }

    @Override
    public void popStates(int lastStateToPop) {
        if (yystate() == lastStateToPop) {
            backToPreviousState();
            return;
        }

        while (isInState(lastStateToPop)) {
            boolean finished = (yystate() == lastStateToPop);
            backToPreviousState();

            if (finished) {
                break;
            }
        }
    }

    @Override
    public boolean isInState(int state) {
        return (state == 0 && lastStates.empty()) || lastStates.contains(state);
    }

    @Override
    public int openParenthesisCount() {
        return openParenths;
    }

    @Override
    public void incOpenParenthesisCount() {
        openParenths++;
    }

    @Override
    public void decOpenParenthesisCount() {
        openParenths--;
    }

    @Override
    public boolean isParamExpansionWord() {
        return paramExpansionWord;
    }

    @Override
    public void setParamExpansionWord(boolean paramExpansionWord) {
        this.paramExpansionWord = paramExpansionWord;
    }

    @Override
    public boolean isParamExpansionOther() {
        return paramExpansionOther;
    }

    @Override
    public void setParamExpansionOther(boolean paramExpansionOther) {
        this.paramExpansionOther = paramExpansionOther;
    }

    @Override
    public boolean isParamExpansionHash() {
        return paramExpansionHash;
    }

    @Override
    public void setParamExpansionHash(boolean paramExpansionHash) {
        this.paramExpansionHash = paramExpansionHash;
    }
}
