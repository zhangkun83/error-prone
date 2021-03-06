/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * @author alexloh@google.com (Alex Loh)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "ClassCanBeStatic",
  summary = "Inner class is non-static but does not reference enclosing class",
  explanation =
      "An inner class should be static unless it references members"
          + " of its enclosing class. An inner class that is made non-static unnecessarily"
          + " uses more memory and does not make the intent of the class clear.",
  category = JDK,
  
  severity = WARNING
)
public class ClassCanBeStatic extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(final ClassTree tree, final VisitorState state) {
    final ClassSymbol currentClass = ASTHelpers.getSymbol(tree);
    if (currentClass == null || !currentClass.hasOuterInstance()) {
      return Description.NO_MATCH;
    }
    if (currentClass.getNestingKind() != NestingKind.MEMBER) {
      // local or anonymous classes can't be static
      return Description.NO_MATCH;
    }
    if (currentClass.owner.enclClass().hasOuterInstance()) {
      // class is nested inside an inner class, so it can't be static
      return Description.NO_MATCH;
    }
    if (tree.getExtendsClause() != null) {
      Type extendsType = ASTHelpers.getType(tree.getExtendsClause());
      if (CanBeStaticAnalyzer.memberOfEnclosing(currentClass, state, extendsType.tsym)) {
        return Description.NO_MATCH;
      }
    }
    if (CanBeStaticAnalyzer.referencesOuter((JCTree) tree, currentClass, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, SuggestedFixes.addModifiers(tree, state, Modifier.STATIC));
  }
}
