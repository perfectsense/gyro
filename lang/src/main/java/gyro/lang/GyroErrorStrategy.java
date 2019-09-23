/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.lang;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;

public class GyroErrorStrategy extends DefaultErrorStrategy {

    public static final GyroErrorStrategy INSTANCE = new GyroErrorStrategy();

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException error) {
        recognizer.notifyErrorListeners(
            error.getOffendingToken(),
            "Expected " + error.getExpectedTokens().toString(recognizer.getVocabulary()),
            error);
    }

    @Override
    protected void reportMissingToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        recognizer.notifyErrorListeners(
            recognizer.getCurrentToken(),
            "Missing " + getExpectedTokens(recognizer).toString(recognizer.getVocabulary()),
            null);
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException error) {
        recognizer.notifyErrorListeners(error.getOffendingToken(), "Invalid input", error);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);
        recognizer.notifyErrorListeners(recognizer.getCurrentToken(), "Extra input", null);
    }

}
