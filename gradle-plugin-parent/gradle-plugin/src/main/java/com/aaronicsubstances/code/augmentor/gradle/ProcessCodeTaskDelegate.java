package com.aaronicsubstances.code.augmentor.gradle;

import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;

class ProcessCodeTaskDelegate extends ProcessCodeGenericTask {
    private final Closure<?> userFunctionCallClosure;
    private final JsonSlurper jsonSlurper;

    ProcessCodeTaskDelegate(Closure<?> userFunctionCallClosure) {
        this.userFunctionCallClosure = userFunctionCallClosure;
        this.jsonSlurper = new JsonSlurper();
    }

    protected Object parseJsonArg(String str) {
        return jsonSlurper.parseText(str);
    }

    protected Object callUserFunction(String functionName, AugmentingCode augCode,
            Map<String, Object> context) {
        return userFunctionCallClosure.call(functionName, augCode, context);
    }
}