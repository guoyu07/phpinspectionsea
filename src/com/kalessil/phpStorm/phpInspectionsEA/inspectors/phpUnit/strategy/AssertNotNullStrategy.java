package com.kalessil.phpStorm.phpInspectionsEA.inspectors.phpUnit.strategy;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import org.jetbrains.annotations.NotNull;

public class AssertNotNullStrategy {
    final static String message = "assertNotNull should be used instead";

    static public void apply(@NotNull String function, @NotNull MethodReference reference, @NotNull ProblemsHolder holder) {
        final PsiElement[] params = reference.getParameters();
        if (2 == params.length && function.equals("assertNotSame")) {
            /* analyze parameters which makes the call equal to assertCount */
            boolean isFirstNull = false;
            if (params[0] instanceof ConstantReference) {
                final String constantName = ((ConstantReference) params[0]).getName();
                isFirstNull = !StringUtil.isEmpty(constantName) && constantName.equals("null");
            }
            boolean isSecondNull = false;
            if (params[1] instanceof ConstantReference) {
                final String referenceName = ((ConstantReference) params[1]).getName();
                isSecondNull = !StringUtil.isEmpty(referenceName) && referenceName.equals("null");
            }

            /* fire assertCount warning when needed */
            if ((isFirstNull && !isSecondNull) || (!isFirstNull && isSecondNull)) {
                final TheLocalFix fixer = new TheLocalFix(isFirstNull ? params[1] : params[0]);
                holder.registerProblem(reference, message, ProblemHighlightType.WEAK_WARNING, fixer);
            }
        }
    }

    private static class TheLocalFix implements LocalQuickFix {
        private PsiElement value;

        TheLocalFix(@NotNull PsiElement value) {
            super();
            this.value = value;
        }

        @NotNull
        @Override
        public String getName() {
            return "Use ::assertNotNull";
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
                replacement.getParameters()[0].replace(this.value);

                final FunctionReference call = (FunctionReference) expression;
                //noinspection ConstantConditions I'm really sure NPE will not happen
                call.getParameterList().replace(replacement.getParameterList());
                call.handleElementRename("assertNotNull");
            }
        }
    }
}