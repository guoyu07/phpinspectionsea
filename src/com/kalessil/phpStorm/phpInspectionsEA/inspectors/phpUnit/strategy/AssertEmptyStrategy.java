package com.kalessil.phpStorm.phpInspectionsEA.inspectors.phpUnit.strategy;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.impl.PhpEmptyImpl;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import org.jetbrains.annotations.NotNull;

public class AssertEmptyStrategy {
    final static String message = "assertEmpty should be used instead";

    static public void apply(@NotNull String function, @NotNull MethodReference reference, @NotNull ProblemsHolder holder) {
        final PsiElement[] params = reference.getParameters();
        if (1 == params.length && (function.equals("assertTrue") || function.equals("assertNotFalse"))) {
            final PsiElement param = ExpressionSemanticUtil.getExpressionTroughParenthesis(params[0]);
            if (param instanceof PhpEmptyImpl) {
                final PsiElement[] variables = ((PhpEmptyImpl) param).getVariables();
                if (1 != variables.length) {
                    return;
                }

                final TheLocalFix fixer = new TheLocalFix(variables[0]);
                holder.registerProblem(reference, message, ProblemHighlightType.WEAK_WARNING, fixer);
            }
        }
    }

    private static class TheLocalFix implements LocalQuickFix {
        private PsiElement argument;

        TheLocalFix(@NotNull PsiElement fileName) {
            super();
            this.argument = fileName;
        }

        @NotNull
        @Override
        public String getName() {
            return "Use ::assertEmpty";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            final PsiElement expression = descriptor.getPsiElement();
            if (expression instanceof FunctionReference) {
                final FunctionReference replacement = PhpPsiElementFactory.createFunctionReference(project, "pattern(null)");
                replacement.getParameters()[0].replace(this.argument);

                final FunctionReference call = (FunctionReference) expression;
                //noinspection ConstantConditions I'm really sure NPE will not happen
                call.getParameterList().replace(replacement.getParameterList());
                call.handleElementRename("assertEmpty");
            }
        }
    }
}
