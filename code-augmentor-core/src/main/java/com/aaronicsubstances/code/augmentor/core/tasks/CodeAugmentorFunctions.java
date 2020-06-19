package com.aaronicsubstances.code.augmentor.core.tasks;

import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;

public class CodeAugmentorFunctions {

    /**
     * Sets/Replaces a variable in file scope.
     * @param augCode
     * @param context
     * 
     * @return object indicating skipping code generation for augCode
     */
    public static GeneratedCode setScopeVar(AugmentingCode augCode, ProcessCodeContext context) {
        modifyScope(context.getFileScope(), augCode);
        return context.newSkipGenCode();
    } 

    /**
     * Sets/Replaces a variable in global scope.
     * @param augCode
     * @param context
     * 
     * @return object indicating skipping code generation for augCode
     */
    public static GeneratedCode setGlobalScopeVar(AugmentingCode augCode, ProcessCodeContext context) {
        modifyScope(context.getGlobalScope(), augCode);
        return context.newSkipGenCode();
    }

    @SuppressWarnings("unchecked")
    private static void modifyScope(Map<String, Object> scope, AugmentingCode augCode) {
        for (Object arg : augCode.getArgs()) {
            Map<String, Object> m = (Map<String, Object>) arg;
            for (Map.Entry<String, Object> e: m.entrySet()) {
                String name = e.getKey();
                Object value = e.getValue();
                scope.put(name, value);
            }
        }
    }
}