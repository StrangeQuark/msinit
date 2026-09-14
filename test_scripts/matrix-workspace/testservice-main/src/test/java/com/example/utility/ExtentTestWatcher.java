package com.example.utility;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

public class ExtentTestWatcher implements TestWatcher, BeforeEachCallback, AfterAllCallback {
    private static final ExtentReports extent = ExtentManager.getInstance();
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) {
        getCurrentTest(context).info("Starting test: " + context.getDisplayName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        getCurrentTest(context).pass("✅ Test passed");
        currentTest.remove();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        getCurrentTest(context).fail("❌ Test failed: " + cause.getMessage());
        currentTest.remove();
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        getCurrentTest(context).skip("⚠️ Test aborted: " + cause.getMessage());
        currentTest.remove();
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        getCurrentTest(context).skip("⏭️ Test skipped: " + reason.orElse("No reason provided"));
        currentTest.remove();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        extent.flush();
    }

    private ExtentTest getCurrentTest(ExtensionContext context) {
        ExtentTest test = currentTest.get();

        if(test == null) {
            test = extent.createTest(context.getDisplayName());
            currentTest.set(test);
        }

        return test;
    }

    // Optional utility to get the current test in progress
    public static ExtentTest getCurrentTest() {
        return currentTest.get();
    }

}
