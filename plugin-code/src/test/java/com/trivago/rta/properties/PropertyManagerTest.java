package com.trivago.rta.properties;

import com.trivago.rta.exceptions.CucablePluginException;
import com.trivago.rta.exceptions.properties.WrongOrMissingPropertiesException;
import com.trivago.rta.logging.CucableLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.trivago.rta.logging.CucableLogger.CucableLogLevel.COMPACT;
import static com.trivago.rta.logging.CucableLogger.CucableLogLevel.DEFAULT;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PropertyManagerTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private PropertyManager propertyManager;
    private CucableLogger logger;

    @Before
    public void setup() {
        logger = mock(CucableLogger.class);
        propertyManager = new PropertyManager(logger);
    }

    @Test
    public void customPlaceholdersTest() {
        Map<String, String> customPlaceholders = new HashMap<>();
        customPlaceholders.put("one", "two");
        customPlaceholders.put("three", "four");
        propertyManager.setCustomPlaceholders(customPlaceholders);
        assertThat(propertyManager.getCustomPlaceholders().size(), is(2));
        assertThat(propertyManager.getCustomPlaceholders().get("one"), is("two"));
        assertThat(propertyManager.getCustomPlaceholders().get("three"), is("four"));
    }

    @Test
    public void sourceRunnerTemplateFileTest() {
        propertyManager.setSourceRunnerTemplateFile("myTemplate");
        assertThat(propertyManager.getSourceRunnerTemplateFile(), is("myTemplate"));
    }

    @Test
    public void parallelizationModeTest() throws CucablePluginException {
        propertyManager.setParallelizationMode("features");
        assertThat(propertyManager.getParallelizationMode(), is(PropertyManager.ParallelizationMode.FEATURES));
        propertyManager.setParallelizationMode("scenarios");
        assertThat(propertyManager.getParallelizationMode(), is(PropertyManager.ParallelizationMode.SCENARIOS));
    }

    @Test
    public void wrongParallelizationModeTest() throws CucablePluginException {
        expectedException.expect(CucablePluginException.class);
        expectedException.expectMessage("Unknown parallelizationMode 'unknown'. Please use 'scenarios' or 'features'.");
        propertyManager.setParallelizationMode("unknown");
    }

    @Test
    public void featureWithoutScenarioLineNumberTest() {
        propertyManager.setSourceFeatures("my.feature");
        assertThat(propertyManager.getSourceFeatures(), is("my.feature"));
        assertThat(propertyManager.getScenarioLineNumbers(), is(notNullValue()));
        assertThat(propertyManager.getScenarioLineNumbers().size(), is(0));
    }

    @Test
    public void featureWithScenarioLineNumberTest() {
        propertyManager.setSourceFeatures("my.feature:123");
        assertThat(propertyManager.getSourceFeatures(), is("my.feature"));
        assertThat(propertyManager.getScenarioLineNumbers().size(), is(1));
        assertThat(propertyManager.getScenarioLineNumbers().get(0), is(123));
    }

    @Test
    public void featureWithInvalidScenarioLineNumberTest() {
        propertyManager.setSourceFeatures("my.feature:abc");
        assertThat(propertyManager.getSourceFeatures(), is("my.feature:abc"));
        assertThat(propertyManager.getScenarioLineNumbers(), is(notNullValue()));
        assertThat(propertyManager.getScenarioLineNumbers().size(), is(0));
    }

    @Test
    public void wrongIncludeTagFormatTest() throws Exception {
        expectedException.expect(CucablePluginException.class);
        expectedException.expectMessage("Tag 'noAtInFront' of type 'include' does not start with '@'.");

        List<String> tags = new ArrayList<>();
        tags.add("noAtInFront");
        propertyManager.setIncludeScenarioTags(tags);
    }

    @Test
    public void wrongExcludeTagFormatTest() throws Exception {
        expectedException.expect(CucablePluginException.class);
        expectedException.expectMessage("Tag 'noAtInFront' of type 'exclude' does not start with '@'.");

        List<String> tags = new ArrayList<>();
        tags.add("noAtInFront");
        propertyManager.setExcludeScenarioTags(tags);
    }

    @Test
    public void logMandatoryPropertiesTest() {
        propertyManager.logProperties();
        verify(logger, times(1)).info("- sourceRunnerTemplateFile  : null", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- generatedRunnerDirectory  : null", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- sourceFeature(s)          : null", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- generatedFeatureDirectory : null", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- numberOfTestRuns          : 0", DEFAULT, COMPACT);
    }

    @Test
    public void logExtendedPropertiesTest() throws CucablePluginException {
        List<String> excludeScenarioTags = new ArrayList<>();
        excludeScenarioTags.add("@exclude1");
        excludeScenarioTags.add("@exclude2");
        propertyManager.setExcludeScenarioTags(excludeScenarioTags);

        List<String> includeScenarioTags = new ArrayList<>();
        includeScenarioTags.add("@include1");
        includeScenarioTags.add("@include2");
        propertyManager.setIncludeScenarioTags(includeScenarioTags);

        Map<String, String> customPlaceholders = new HashMap<>();
        customPlaceholders.put("key1", "value1");
        customPlaceholders.put("key2", "value2");
        propertyManager.setCustomPlaceholders(customPlaceholders);

        propertyManager.setSourceFeatures("test.feature:3");
        propertyManager.setDesiredNumberOfRunners(2);

        propertyManager.logProperties();
        verify(logger, times(1)).info("- sourceFeature(s)          : test.feature", DEFAULT, COMPACT);
        verify(logger, times(1)).info("                              with line number(s) 3", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- include scenario tag(s)   : @include1, @include2", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- exclude scenario tag(s)   : @exclude1, @exclude2", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- sourceRunnerTemplateFile  : null", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- generatedRunnerDirectory  : null", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- custom placeholder(s)     :", DEFAULT, COMPACT);
        verify(logger, times(1)).info("  key1 => value1", DEFAULT, COMPACT);
        verify(logger, times(1)).info("  key2 => value2", DEFAULT, COMPACT);
        verify(logger, times(1)).info("- desiredNumberOfRunners    : 2", DEFAULT, COMPACT);
    }

    @Test
    public void logMissingPropertiesTest() throws CucablePluginException {
        expectedException.expect(WrongOrMissingPropertiesException.class);
        expectedException.expectMessage("Properties not specified correctly in the configuration section of your pom file: [<sourceRunnerTemplateFile>, <generatedRunnerDirectory>, <sourceFeatures>, <generatedFeatureDirectory>]");
        propertyManager.checkForMissingMandatoryProperties();

    }
}
