package com.framework.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * IAnnotationTransformer that injects RetryAnalyzer into EVERY @Test method
 * at runtime — no need to annotate each test with retryAnalyzer=RetryAnalyzer.class.
 *
 * Register in testng.xml (must come BEFORE the test suite runs):
 *   <listeners>
 *     <listener class-name="com.framework.listeners.RetryTransformer"/>
 *     <listener class-name="com.framework.listeners.TestListener"/>
 *   </listeners>
 *
 * Disable retries entirely:
 *   -Dretry.count=0
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {

        // Only inject if no retryAnalyzer is already set on the annotation
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
